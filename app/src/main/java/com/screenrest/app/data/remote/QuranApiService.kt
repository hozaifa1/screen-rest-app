package com.screenrest.app.data.remote

import com.screenrest.app.data.remote.dto.AyahResponseDto
import com.screenrest.app.data.remote.dto.RandomAyahResponseDto
import retrofit2.http.GET
import retrofit2.http.Path

interface QuranApiService {
    
    @GET("ayah/{reference}/en.asad")
    suspend fun getAyahEnglish(@Path("reference") reference: String): AyahResponseDto
    
    @GET("ayah/{reference}/ar.alafasy")
    suspend fun getAyahArabic(@Path("reference") reference: String): AyahResponseDto
    
    @GET("ayah/{reference}")
    suspend fun getAyah(@Path("reference") reference: String): AyahResponseDto
    
    companion object {
        const val BASE_URL = "https://api.alquran.cloud/v1/"
    }
}
