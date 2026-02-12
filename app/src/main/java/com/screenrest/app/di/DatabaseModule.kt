package com.screenrest.app.di

import android.content.Context
import androidx.room.Room
import com.screenrest.app.data.local.database.AppDatabase
import com.screenrest.app.data.local.database.dao.CustomMessageDao
import com.screenrest.app.data.local.database.dao.IslamicReminderDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "screenrest_database"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    @Singleton
    fun provideCustomMessageDao(database: AppDatabase): CustomMessageDao {
        return database.customMessageDao()
    }

    @Provides
    @Singleton
    fun provideIslamicReminderDao(database: AppDatabase): IslamicReminderDao {
        return database.islamicReminderDao()
    }
}
