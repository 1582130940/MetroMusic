package code.name.monkey.retromusic.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_23_24 = object : Migration(23, 24) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(sql = "DROP TABLE LyricsEntity")
        database.execSQL(sql = "DROP TABLE BlackListStoreEntity")
    }
}
