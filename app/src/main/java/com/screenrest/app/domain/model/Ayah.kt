package com.screenrest.app.domain.model

data class Ayah(
    val id: Long = 0,
    val surahNumber: Int,
    val ayahNumber: Int,
    val englishTranslation: String,
    val surahName: String
)
