package com.makemission.ora.downloads

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction

/** One unfinished download. Only downloads that can still resume are stored. */
@Entity(tableName = "downloads")
data class DownloadRow(
    @PrimaryKey val id: Long,
    val fileName: String,
    val url: String,
    val mimeType: String?,
    val totalBytes: Long,
    val etag: String?,
    val ranged: Boolean,
    val startedAt: Long,
)

/** One part of a download: its byte range and how much of it is on disk. [idx] keeps the parts in order. */
@Entity(
    tableName = "download_parts",
    primaryKeys = ["downloadId", "idx"],
    foreignKeys = [ForeignKey(entity = DownloadRow::class, parentColumns = ["id"], childColumns = ["downloadId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("downloadId")],
)
data class PartRow(
    val downloadId: Long,
    val idx: Int,
    val start: Long,
    val end: Long,
    val done: Long,
    val finished: Boolean,
)

@Dao
abstract class DownloadDao {
    @Query("SELECT * FROM downloads ORDER BY id")
    abstract suspend fun rows(): List<DownloadRow>

    @Query("SELECT * FROM download_parts ORDER BY downloadId, idx")
    abstract suspend fun parts(): List<PartRow>

    @Query("DELETE FROM download_parts")
    abstract suspend fun clearParts()

    @Query("DELETE FROM downloads")
    abstract suspend fun clearRows()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertRows(rows: List<DownloadRow>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertParts(parts: List<PartRow>)

    /** Replaces the whole saved queue in one transaction, so a crash never leaves half of it. */
    @Transaction
    open suspend fun replaceAll(rows: List<DownloadRow>, parts: List<PartRow>) {
        clearParts()
        clearRows()
        insertRows(rows)
        insertParts(parts)
    }
}

@Database(entities = [DownloadRow::class, PartRow::class], version = 1, exportSchema = false)
abstract class DownloadDatabase : RoomDatabase() {
    abstract fun downloadDao(): DownloadDao
}
