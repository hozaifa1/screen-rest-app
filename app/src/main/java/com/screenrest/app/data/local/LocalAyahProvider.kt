package com.screenrest.app.data.local

import android.content.Context
import com.screenrest.app.R
import com.screenrest.app.domain.model.Ayah
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Serializable
data class AyahDto(
    val surahNumber: Int,
    val ayahNumber: Int,
    val arabicText: String,
    val englishTranslation: String,
    val surahName: String
)

@Singleton
class LocalAyahProvider @Inject constructor(
    private val context: Context
) {
    private val json = Json { ignoreUnknownKeys = true }
    private var cachedAyahs: List<Ayah>? = null

    private fun loadAyahs(): List<Ayah> {
        if (cachedAyahs != null) {
            return cachedAyahs!!
        }

        val jsonString = context.resources.openRawResource(R.raw.local_ayahs)
            .bufferedReader()
            .use { it.readText() }

        val ayahDtos = json.decodeFromString<List<AyahDto>>(jsonString)
        cachedAyahs = ayahDtos.map { dto ->
            Ayah(
                surahNumber = dto.surahNumber,
                ayahNumber = dto.ayahNumber,
                arabicText = dto.arabicText,
                englishTranslation = dto.englishTranslation,
                surahName = dto.surahName
            )
        }
        return cachedAyahs!!
    }

    fun getRandomLocalAyah(): Ayah {
        val ayahs = loadAyahs()
        return ayahs[Random.nextInt(ayahs.size)]
    }

    fun getAllLocalAyahs(): List<Ayah> {
        return loadAyahs()
    }
}
