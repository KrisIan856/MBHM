'use strict';

const http = require('node:http');
const os = require('node:os');
const path = require('node:path');
const { DatabaseSync } = require('node:sqlite');

const PORT = Number(process.env.PORT || 3000);
const DB_FILE = process.env.DB_FILE || path.join(__dirname, 'mbhm_server.db');
const MAX_BODY = 64 * 1024 * 1024;

const COLLECTIONS = {
    users: { pk: 'username' },
    boarders: { pk: 'id' },
    paymentRecords: { pk: 'id' },
    attendanceSessions: { pk: 'id' },
    attendanceRecords: { pk: 'id' },
    announcements: { pk: 'id' },
    visitorEntries: { pk: 'id' },
    incidents: { pk: 'id' },
    maintenanceReports: { pk: 'id' },
    curfewRecords: { pk: 'id' },
    worshipSchedules: { pk: 'id' },
    billingSettings: { pk: 'id' },
    leaveNotices: { pk: 'id' },
    absenceAlerts: { pk: 'id' },
    rooms: { pk: 'name' },
    jobs: { pk: 'name' },
    guardianProfile: { pk: 'id' }
};

const db = new DatabaseSync(DB_FILE);
db.exec(`
    PRAGMA journal_mode = WAL;
    CREATE TABLE IF NOT EXISTS records (
        collection TEXT NOT NULL,
        record_id  TEXT NOT NULL,
        data       TEXT NOT NULL,
        version    INTEGER NOT NULL,
        deleted    INTEGER NOT NULL DEFAULT 0,
        PRIMARY KEY (collection, record_id)
    );
    CREATE TABLE IF NOT EXISTS client_rows (
        client_id     TEXT NOT NULL,
        collection    TEXT NOT NULL,
        record_id     TEXT NOT NULL,
        known_version INTEGER NOT NULL DEFAULT 0,
        PRIMARY KEY (client_id, collection, record_id)
    );
    CREATE TABLE IF NOT EXISTS meta (
        key   TEXT PRIMARY KEY,
        value TEXT NOT NULL
    );
    CREATE INDEX IF NOT EXISTS idx_records_version ON records (version);
`);

const clientRowColumns = db.prepare('PRAGMA table_info(client_rows)').all();
if (!clientRowColumns.some((column) => column.name === 'known_version')) {
    db.exec('ALTER TABLE client_rows ADD COLUMN known_version INTEGER NOT NULL DEFAULT 0');
}

const stmt = {
    metaGet: db.prepare('SELECT value FROM meta WHERE key = ?'),
    metaSet: db.prepare('INSERT INTO meta (key, value) VALUES (?, ?) ON CONFLICT(key) DO UPDATE SET value = excluded.value'),
    recordGet: db.prepare('SELECT * FROM records WHERE collection = ? AND record_id = ?'),
    recordInsert: db.prepare('INSERT INTO records (collection, record_id, data, version, deleted) VALUES (?, ?, ?, ?, ?)'),
    recordUpdate: db.prepare('UPDATE records SET data = ?, version = ?, deleted = ? WHERE collection = ? AND record_id = ?'),
    recordsSince: db.prepare('SELECT * FROM records WHERE version > ? ORDER BY version'),
    recordsLive: db.prepare("SELECT * FROM records WHERE deleted = 0 ORDER BY collection, record_id"),
    clientRowsAll: db.prepare('SELECT record_id, known_version FROM client_rows WHERE client_id = ? AND collection = ?'),
    clientRowsDelete: db.prepare('DELETE FROM client_rows WHERE client_id = ? AND collection = ?'),
    clientRowsInsert: db.prepare('INSERT INTO client_rows (client_id, collection, record_id, known_version) VALUES (?, ?, ?, ?)'),
    countLive: db.prepare('SELECT COUNT(*) AS n FROM records WHERE collection = ? AND deleted = 0'),
    countAll: db.prepare('SELECT COUNT(*) AS n FROM records')
};

let version = Number(stmt.metaGet.get('version')?.value || 0);

function bump() {
    return ++version;
}

function saveVersion() {
    stmt.metaSet.run('version', String(version));
}

function emptyCollections() {
    const upserts = {};
    const deletes = {};
    for (const name of Object.keys(COLLECTIONS)) {
        upserts[name] = [];
        deletes[name] = [];
    }
    return { upserts, deletes };
}

function handlePush(body) {
    const clientId = String((body && body.clientId) || 'unknown');
    const payload = (body && body.payload) || {};
    let changes = 0;
    let conflicts = 0;

    for (const [collection, cfg] of Object.entries(COLLECTIONS)) {
        if (!Object.prototype.hasOwnProperty.call(payload, collection)) continue;
        const rows = Array.isArray(payload[collection]) ? payload[collection] : [];
        const known = new Map();
        for (const prev of stmt.clientRowsAll.all(clientId, collection)) {
            known.set(prev.record_id, prev.known_version);
        }

        const incomingIds = new Set();
        const nextKnown = new Map();

        for (const row of rows) {
            if (!row || row[cfg.pk] == null) continue;
            const id = String(row[cfg.pk]);
            incomingIds.add(id);
            const json = JSON.stringify(row);
            const seen = known.get(id) || 0;
            const existing = stmt.recordGet.get(collection, id);

            if (!existing) {
                const inserted = bump();
                stmt.recordInsert.run(collection, id, json, inserted, 0);
                nextKnown.set(id, inserted);
                changes++;
                continue;
            }

            if (existing.version > seen) {
                nextKnown.set(id, existing.version);
                conflicts++;
                continue;
            }

            if (existing.data === json && existing.deleted === 0) {
                nextKnown.set(id, existing.version);
                continue;
            }

            const updated = bump();
            stmt.recordUpdate.run(json, updated, 0, collection, id);
            nextKnown.set(id, updated);
            changes++;
        }

        for (const [id, seen] of known) {
            if (incomingIds.has(id)) continue;
            const existing = stmt.recordGet.get(collection, id);
            if (!existing) continue;
            let current = existing.version;
            if (existing.deleted === 0) {
                current = bump();
                stmt.recordUpdate.run(existing.data, current, 1, collection, id);
                changes++;
            }
            nextKnown.set(id, Math.max(seen, current));
        }

        stmt.clientRowsDelete.run(clientId, collection);
        for (const [id, seen] of nextKnown) {
            stmt.clientRowsInsert.run(clientId, collection, id, seen);
        }
    }

    saveVersion();
    return { ok: true, version, changes, conflicts };
}

function handlePull(sinceRaw) {
    const requested = Number(sinceRaw || 0);
    const since = requested > 0 && requested <= version ? requested : 0;
    const { upserts, deletes } = emptyCollections();

    for (const row of stmt.recordsSince.all(since)) {
        if (row.deleted) {
            if (deletes[row.collection]) deletes[row.collection].push(row.record_id);
        } else {
            try {
                upserts[row.collection].push(JSON.parse(row.data));
            } catch (err) {
                console.error(`[pull] bad row ${row.collection}/${row.record_id}: ${err.message}`);
            }
        }
    }

    return {
        since,
        version,
        upserts: Object.assign({ timestamp: Date.now(), deviceName: 'server' }, upserts),
        deletes
    };
}

function handleExport() {
    const { upserts } = emptyCollections();
    for (const row of stmt.recordsLive.all()) {
        try {
            upserts[row.collection].push(JSON.parse(row.data));
        } catch (err) {
            console.error(`[export] bad row ${row.collection}/${row.record_id}: ${err.message}`);
        }
    }
    return { version, deviceName: 'server', timestamp: Date.now(), ...upserts };
}

function handleStatus() {
    const tables = {};
    for (const name of Object.keys(COLLECTIONS)) {
        tables[name] = stmt.countLive.get(name).n;
    }
    return {
        ok: true,
        service: 'mbhm-sync-server',
        version,
        records: stmt.countAll.get().n,
        tables
    };
}

function escapeHtml(value) {
    return String(value)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;');
}

function cellText(value) {
    if (value == null) return '';
    if (typeof value === 'object') return escapeHtml(JSON.stringify(value));
    return escapeHtml(String(value));
}

function renderDashboard() {
    const status = handleStatus();
    const data = handleExport();
    const sections = [];

    for (const [collection, rows] of Object.entries(data)) {
        if (!Array.isArray(rows) || rows.length === 0) continue;
        const columns = [...new Set(rows.flatMap((row) => Object.keys(row)))];
        const head = columns.map((c) => `<th>${escapeHtml(c)}</th>`).join('');
        const body = rows
            .map((row) => `<tr>${columns.map((c) => `<td>${cellText(row[c])}</td>`).join('')}</tr>`)
            .join('');
        sections.push(`
            <section>
                <h2>${escapeHtml(collection)} <span>${rows.length}</span></h2>
                <div class="wrap"><table><thead><tr>${head}</tr></thead><tbody>${body}</tbody></table></div>
            </section>`);
    }

    if (sections.length === 0) {
        sections.push('<section><p class="empty">No records yet. Sync a device to see data here.</p></section>');
    }

    const addresses = lanAddresses()
        .map((ip) => `<code>http://${ip}:${PORT}</code>`)
        .join(' ');

    return `<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<meta http-equiv="refresh" content="10">
<title>MBHM Shared Database</title>
<style>
  :root { color-scheme: light; }
  body { margin: 0; font: 14px/1.5 system-ui, -apple-system, Segoe UI, sans-serif; background: #f5f2ee; color: #2c2825; }
  header { padding: 20px 24px; background: #8b7fc7; color: #fff; }
  header h1 { margin: 0 0 4px; font-size: 18px; }
  header p { margin: 0; font-size: 13px; opacity: .9; }
  main { padding: 20px 24px 48px; max-width: 1400px; margin: 0 auto; }
  section { background: #fff; border: 1px solid #e2ddd9; border-radius: 14px; padding: 16px; margin-bottom: 16px; }
  h2 { margin: 0 0 12px; font-size: 15px; display: flex; justify-content: space-between; }
  h2 span { color: #8a847e; font-weight: 500; }
  .wrap { overflow-x: auto; }
  table { border-collapse: collapse; width: 100%; font-size: 12.5px; }
  th, td { text-align: left; padding: 7px 10px; border-bottom: 1px solid #eee9e4; white-space: nowrap; max-width: 340px; overflow: hidden; text-overflow: ellipsis; }
  th { background: #f7f5f2; position: sticky; top: 0; }
  .empty { color: #8a847e; margin: 4px 0; }
</style>
</head>
<body>
<header>
  <h1>MBHM Shared Database</h1>
  <p>version ${status.version} &middot; ${status.records} records &middot; refreshes every 10s</p>
  <p>Devices connect to: ${addresses || `<code>http://&lt;this-pc&gt;:${PORT}</code>`}</p>
</header>
<main>${sections.join('')}</main>
</body>
</html>`;
}

function lanAddresses() {
    const out = [];
    for (const info of Object.values(os.networkInterfaces())) {
        for (const addr of info || []) {
            if (addr.family === 'IPv4' && !addr.internal) out.push(addr.address);
        }
    }
    return out;
}

function sendJson(res, statusCode, payload) {
    const body = JSON.stringify(payload);
    res.writeHead(statusCode, {
        'Content-Type': 'application/json; charset=utf-8',
        'Content-Length': Buffer.byteLength(body),
        'Access-Control-Allow-Origin': '*',
        'Access-Control-Allow-Headers': 'Content-Type',
        'Access-Control-Allow-Methods': 'GET, POST, OPTIONS',
        'Connection': 'close'
    });
    res.end(body);
}

function sendHtml(res, statusCode, html) {
    res.writeHead(statusCode, {
        'Content-Type': 'text/html; charset=utf-8',
        'Content-Length': Buffer.byteLength(html),
        'Connection': 'close'
    });
    res.end(html);
}

function readBody(req) {
    return new Promise((resolve, reject) => {
        let size = 0;
        const chunks = [];
        req.on('data', (chunk) => {
            size += chunk.length;
            if (size > MAX_BODY) {
                reject(new Error('payload too large'));
                req.destroy();
                return;
            }
            chunks.push(chunk);
        });
        req.on('end', () => resolve(Buffer.concat(chunks).toString('utf8')));
        req.on('error', reject);
    });
}

function countPayload(payload) {
    let n = 0;
    for (const name of Object.keys(COLLECTIONS)) {
        if (Array.isArray(payload && payload[name])) n += payload[name].length;
    }
    return n;
}

const server = http.createServer(async (req, res) => {
    const url = new URL(req.url, `http://${req.headers.host || 'localhost'}`);

    if (req.method === 'OPTIONS') {
        res.writeHead(204, {
            'Access-Control-Allow-Origin': '*',
            'Access-Control-Allow-Headers': 'Content-Type',
            'Access-Control-Allow-Methods': 'GET, POST, OPTIONS'
        });
        res.end();
        return;
    }

    try {
        if (req.method === 'GET' && (url.pathname === '/' || url.pathname === '/index.html')) {
            sendHtml(res, 200, renderDashboard());
            return;
        }

        if (req.method === 'GET' && url.pathname === '/api/status') {
            sendJson(res, 200, handleStatus());
            return;
        }

        if (req.method === 'GET' && url.pathname === '/api/export') {
            sendJson(res, 200, handleExport());
            return;
        }

        if (req.method === 'GET' && url.pathname === '/api/pull') {
            sendJson(res, 200, handlePull(url.searchParams.get('since')));
            return;
        }

        if (req.method === 'POST' && url.pathname === '/api/push') {
            const raw = (await readBody(req)).replace(/^\uFEFF/, '');
            const body = raw ? JSON.parse(raw) : {};
            const result = handlePush(body);
            console.log(`[${new Date().toISOString()}] PUSH ${body.clientId || 'unknown'} · ${countPayload(body.payload)} rows · ${result.changes} changed · ${result.conflicts} kept · version ${result.version}`);
            sendJson(res, 200, result);
            return;
        }

        sendJson(res, 404, { ok: false, error: 'not found' });
    } catch (err) {
        console.error(`[${new Date().toISOString()}] ${req.method} ${url.pathname} failed: ${err.stack || err.message}`);
        sendJson(res, 500, { ok: false, error: err.message });
    }
});

server.listen(PORT, '0.0.0.0', () => {
    console.log('MBHM sync server running');
    console.log(`  database : ${DB_FILE}`);
    console.log(`  dashboard: http://localhost:${PORT}/`);
    for (const ip of lanAddresses()) {
        console.log(`  devices  : http://${ip}:${PORT}`);
    }
    console.log('Keep this window open while devices sync.');
});
