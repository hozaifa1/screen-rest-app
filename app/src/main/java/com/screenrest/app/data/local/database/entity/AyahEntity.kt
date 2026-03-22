package com.screenrest.app.data.local.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.screenrest.app.domain.model.Ayah

@Entity(tableName = "ayahs")
data class AyahEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val surahNumber: Int,
    val ayahNumber: Int,
    val arabicText: String,
    val englishTranslation: String,
    val surahName: String,
    val createdAt: Long
)

fun AyahEntity.toDomain(): Ayah {
    return Ayah(
        id = id,
        surahNumber = surahNumber,
        ayahNumber = ayahNumber,
        arabicText = arabicText,
        englishTranslation = englishTranslation,
        surahName = surahName
    )
}

fun Ayah.toEntity(createdAt: Long = System.currentTimeMillis()): AyahEntity {
    return AyahEntity(
        id = id,
        surahNumber = surahNumber,
        ayahNumber = ayahNumber,
        arabicText = arabicText,
        englishTranslation = englishTranslation,
        surahName = surahName,
        createdAt = createdAt
    )
}
