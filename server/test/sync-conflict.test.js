'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');
const { spawn } = require('node:child_process');
const crypto = require('node:crypto');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');

const serverDir = path.resolve(__dirname, '..');

test('a stale client cannot overwrite a newer record after a failed pull', async (t) => {
    const tempDir = fs.mkdtempSync(path.join(os.tmpdir(), 'mbhm-sync-test-'));
    const token = `test-${crypto.randomUUID()}`;
    const tokenFile = path.join(tempDir, '.sync-token');
    const port = 32000 + Math.floor(Math.random() * 2000);
    const baseUrl = `http://127.0.0.1:${port}`;
    fs.writeFileSync(tokenFile, token, { encoding: 'utf8', mode: 0o600 });

    let output = '';
    const child = spawn(process.execPath, ['server.js'], {
        cwd: serverDir,
        env: {
            ...process.env,
            PORT: String(port),
            DB_FILE: path.join(tempDir, 'test.db'),
            TOKEN_FILE: tokenFile
        },
        stdio: ['ignore', 'pipe', 'pipe']
    });
    child.stdout.on('data', (chunk) => { output += chunk.toString(); });
    child.stderr.on('data', (chunk) => { output += chunk.toString(); });

    t.after(async () => {
        if (child.exitCode === null && !child.killed) {
            child.kill();
            await new Promise((resolve) => child.once('exit', resolve));
        }
        fs.rmSync(tempDir, { recursive: true, force: true });
    });

    const headers = {
        Authorization: `Bearer ${token}`,
        'Content-Type': 'application/json'
    };

    for (let attempt = 0; attempt < 50; attempt += 1) {
        try {
            const response = await fetch(`${baseUrl}/api/status`, { headers });
            if (response.ok) break;
        } catch (_) {
            // The server is still starting.
        }
        if (attempt === 49) {
            throw new Error(`Server did not start:\n${output}`);
        }
        await new Promise((resolve) => setTimeout(resolve, 100));
    }

    async function push(clientId, payload) {
        const response = await fetch(`${baseUrl}/api/push`, {
            method: 'POST',
            headers,
            body: JSON.stringify({ clientId, payload })
        });
        assert.equal(response.status, 200);
        return response.json();
    }

    async function pull(since) {
        const response = await fetch(`${baseUrl}/api/pull?since=${since}`, { headers });
        assert.equal(response.status, 200);
        return response.json();
    }

    const first = await push('client-A', { boarders: [{ id: 'b1', name: 'A' }] });
    assert.equal(first.changes, 1);
    assert.equal(first.conflicts, 0);

    // Client B has never pulled. Its stale row must conflict, not advance its
    // acknowledged version, and must not change the server record.
    const stale = await push('client-B', { boarders: [{ id: 'b1', name: 'B' }] });
    assert.equal(stale.changes, 0);
    assert.equal(stale.conflicts, 1);

    const canonical = await pull(0);
    assert.equal(canonical.upserts.boarders[0].name, 'A');

    // After pulling, the same row is byte-for-byte canonical and should be
    // acknowledged without another conflict or write.
    const resolved = await push('client-B', { boarders: [{ id: 'b1', name: 'A' }] });
    assert.equal(resolved.changes, 0);
    assert.equal(resolved.conflicts, 0);
});
