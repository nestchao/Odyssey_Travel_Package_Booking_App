package com.example.mad_assignment.di

import com.google.ai.client.generativeai.GenerativeModel
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideGenerativeModel(): GenerativeModel {
        return GenerativeModel(
            modelName = "gemini-2.5-flash",
            apiKey = "GOOGLE_API_KEY"
        )
    }

    @Provides
    @Singleton
    fun provideGson(): Gson {
        return Gson()
    }
}
