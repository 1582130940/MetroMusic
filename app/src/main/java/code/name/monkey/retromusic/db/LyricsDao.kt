package code.name.monkey.retromusic.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface LyricsDao {
    @Query("SELECT * FROM LyricsEntity WHERE songId =:songId LIMIT 1")
    fun lyricsWithSongId(songId: Int): LyricsEntity?

    @Insert
    fun insertLyrics(lyricsEntity: LyricsEntity)

    @Delete
    fun deleteLyrics(lyricsEntity: LyricsEntity)

    @Update
    fun updateLyrics(lyricsEntity: LyricsEntity)
}
