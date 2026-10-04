package com.example.mbhm.data.converter

import androidx.room.TypeConverter
import com.example.mbhm.model.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromPayStatus(value: String?): PayStatus? = value?.let { PayStatus.valueOf(it) }

    @TypeConverter
    fun toPayStatus(value: PayStatus?): String? = value?.name

    @TypeConverter
    fun fromReceiptStatus(value: String?): ReceiptStatus? = value?.let { ReceiptStatus.valueOf(it) }

    @TypeConverter
    fun toReceiptStatus(value: ReceiptStatus?): String? = value?.name

    @TypeConverter
    fun fromPayMethod(value: String?): PayMethod? = value?.let { PayMethod.valueOf(it) }

    @TypeConverter
    fun toPayMethod(value: PayMethod?): String? = value?.name

    @TypeConverter
    fun fromRecordedBy(value: String?): RecordedBy? = value?.let { RecordedBy.valueOf(it) }

    @TypeConverter
    fun toRecordedBy(value: RecordedBy?): String? = value?.name

    @TypeConverter
    fun fromAttendanceStatus(value: String?): AttendanceStatus? = value?.let { AttendanceStatus.valueOf(it) }

    @TypeConverter
    fun toAttendanceStatus(value: AttendanceStatus?): String? = value?.name

    @TypeConverter
    fun fromAttendanceMethod(value: String?): AttendanceMethod? = value?.let { AttendanceMethod.valueOf(it) }

    @TypeConverter
    fun toAttendanceMethod(value: AttendanceMethod?): String? = value?.name

    @TypeConverter
    fun fromSessionType(value: String?): SessionType? = value?.let { SessionType.valueOf(it) }

    @TypeConverter
    fun toSessionType(value: SessionType?): String? = value?.name

    @TypeConverter
    fun fromPriority(value: String?): Priority? = value?.let { Priority.valueOf(it) }

    @TypeConverter
    fun toPriority(value: Priority?): String? = value?.name

    @TypeConverter
    fun fromPenaltyType(value: String?): PenaltyType? = value?.let { PenaltyType.valueOf(it) }

    @TypeConverter
    fun toPenaltyType(value: PenaltyType?): String? = value?.name

    @TypeConverter
    fun fromIncidentType(value: String?): IncidentType? = value?.let { IncidentType.valueOf(it) }

    @TypeConverter
    fun toIncidentType(value: IncidentType?): String? = value?.name

    @TypeConverter
    fun fromMaintCategory(value: String?): MaintCategory? = value?.let { MaintCategory.valueOf(it) }

    @TypeConverter
    fun toMaintCategory(value: MaintCategory?): String? = value?.name

    @TypeConverter
    fun fromMaintStatus(value: String?): MaintStatus? = value?.let { MaintStatus.valueOf(it) }

    @TypeConverter
    fun toMaintStatus(value: MaintStatus?): String? = value?.name

    @TypeConverter
    fun fromCurfewStatus(value: String?): CurfewStatus? = value?.let { CurfewStatus.valueOf(it) }

    @TypeConverter
    fun toCurfewStatus(value: CurfewStatus?): String? = value?.name

    @TypeConverter
    fun fromLeaveNoticeStatus(value: String?): LeaveNoticeStatus? = value?.let { LeaveNoticeStatus.valueOf(it) }

    @TypeConverter
    fun toLeaveNoticeStatus(value: LeaveNoticeStatus?): String? = value?.name

    @TypeConverter
    fun fromStringList(value: String?): List<String>? = stringToList(value)

    @TypeConverter
    fun toStringList(value: List<String>?): String? = listToString(value)

    companion object {
        private val gson = Gson()

        @JvmStatic
        fun stringToList(value: String?): List<String>? = value?.let {
            gson.fromJson(it, object : TypeToken<List<String>>() {}.type)
        }

        @JvmStatic
        fun listToString(value: List<String>?): String? = value?.let { gson.toJson(it) }
    }
}