package com.screenrest.app.di

import com.screenrest.app.data.repository.AyahRepository
import com.screenrest.app.data.repository.AyahRepositoryImpl
import com.screenrest.app.data.repository.CustomMessageRepository
import com.screenrest.app.data.repository.CustomMessageRepositoryImpl
import com.screenrest.app.data.repository.IslamicReminderRepository
import com.screenrest.app.data.repository.IslamicReminderRepositoryImpl
import com.screenrest.app.data.repository.SettingsRepository
import com.screenrest.app.data.repository.SettingsRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        settingsRepositoryImpl: SettingsRepositoryImpl
    ): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindCustomMessageRepository(
        customMessageRepositoryImpl: CustomMessageRepositoryImpl
    ): CustomMessageRepository

    @Binds
    @Singleton
    abstract fun bindAyahRepository(
        ayahRepositoryImpl: AyahRepositoryImpl
    ): AyahRepository

    @Binds
    @Singleton
    abstract fun bindIslamicReminderRepository(
        islamicReminderRepositoryImpl: IslamicReminderRepositoryImpl
    ): IslamicReminderRepository
}
