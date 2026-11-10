package com.example.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.PrimaryKey

/**
 * Room FTS4 Full-Text Search entity for high-performance bilingual tag retrieval.
 * Supports indexing and fast fuzzy queries on both english_tag and chinese_name fields.
 */
@Entity(tableName = "tags_fts")
@Fts4
data class TagFtsEntity(
    @PrimaryKey
    @ColumnInfo(name = "rowid")
    val rowid: Int = 0,

    @ColumnInfo(name = "english_tag")
    val englishTag: String,

    @ColumnInfo(name = "chinese_name")
    val chineseName: String,

    @ColumnInfo(name = "category")
    val category: String = ""
)
