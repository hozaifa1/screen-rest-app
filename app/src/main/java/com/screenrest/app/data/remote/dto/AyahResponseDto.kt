package com.screenrest.app.data.remote.dto

import com.screenrest.app.domain.model.Ayah
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AyahResponseDto(
    @SerialName("code")
    val code: Int,
    @SerialName("status")
    val status: String,
    @SerialName("data")
    val data: AyahDataDto
)

@Serializable
data class AyahDataDto(
    @SerialName("number")
    val number: Int,
    @SerialName("text")
    val text: String,
    @SerialName("edition")
    val edition: EditionDto,
    @SerialName("surah")
    val surah: SurahDto,
    @SerialName("numberInSurah")
    val numberInSurah: Int,
    @SerialName("juz")
    val juz: Int? = null,
    @SerialName("manzil")
    val manzil: Int? = null,
    @SerialName("page")
    val page: Int? = null,
    @SerialName("ruku")
    val ruku: Int? = null,
    @SerialName("hizbQuarter")
    val hizbQuarter: Int? = null,
    @SerialName("sajda")
    val sajda: SajdaDto? = null
)

@Serializable
data class EditionDto(
    @SerialName("identifier")
    val identifier: String,
    @SerialName("language")
    val language: String,
    @SerialName("name")
    val name: String,
    @SerialName("englishName")
    val englishName: String,
    @SerialName("format")
    val format: String,
    @SerialName("type")
    val type: String
)

@Serializable
data class SurahDto(
    @SerialName("number")
    val number: Int,
    @SerialName("name")
    val name: String,
    @SerialName("englishName")
    val englishName: String,
    @SerialName("englishNameTranslation")
    val englishNameTranslation: String,
    @SerialName("revelationType")
    val revelationType: String,
    @SerialName("numberOfAyahs")
    val numberOfAyahs: Int
)

@Serializable
data class SajdaDto(
    @SerialName("id")
    val id: Int,
    @SerialName("recommended")
    val recommended: Boolean,
    @SerialName("obligatory")
    val obligatory: Boolean
)

@Serializable
data class RandomAyahResponseDto(
    @SerialName("code")
    val code: Int,
    @SerialName("status")
    val status: String,
    @SerialName("data")
    val data: List<AyahDataDto>
)

fun AyahDataDto.toAyah(arabicText: String? = null): Ayah {
    return Ayah(
        surahNumber = surah.number,
        ayahNumber = numberInSurah,
        arabicText = arabicText ?: "",
        englishTranslation = text,
        surahName = surah.englishName
    )
}
