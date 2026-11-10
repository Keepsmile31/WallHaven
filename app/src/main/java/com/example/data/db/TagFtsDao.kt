package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface TagFtsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tags: List<TagFtsEntity>)

    @Query("SELECT COUNT(*) FROM tags_fts")
    suspend fun getTagCount(): Int

    /**
     * Efficient fuzzy matching on chinese_name and english_tag with relevancy ranking.
     */
    @Query("""
        SELECT rowid, english_tag, chinese_name, category FROM tags_fts 
        WHERE chinese_name LIKE '%' || :query || '%' 
           OR english_tag LIKE '%' || :query || '%'
        ORDER BY 
           CASE 
             WHEN LOWER(chinese_name) = LOWER(:query) THEN 1
             WHEN LOWER(english_tag) = LOWER(:query) THEN 2
             WHEN chinese_name LIKE :query || '%' THEN 3
             WHEN english_tag LIKE :query || '%' THEN 4
             ELSE 5 
           END
        LIMIT :limit
    """)
    suspend fun searchTags(query: String, limit: Int = 12): List<TagFtsEntity>

    @Query("SELECT rowid, english_tag, chinese_name, category FROM tags_fts WHERE LOWER(english_tag) = LOWER(:englishTag) LIMIT 1")
    suspend fun findByEnglishTag(englishTag: String): TagFtsEntity?

    @Query("SELECT rowid, english_tag, chinese_name, category FROM tags_fts WHERE chinese_name = :chineseName LIMIT 1")
    suspend fun findByChineseName(chineseName: String): TagFtsEntity?

    @Query("SELECT rowid, english_tag, chinese_name, category FROM tags_fts")
    suspend fun getAllTags(): List<TagFtsEntity>

    @Query("DELETE FROM tags_fts WHERE chinese_name LIKE '%6ffafab0%'")
    suspend fun purgeCorruptedTags()
}
