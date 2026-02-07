package com.example.netblockerpro.di

import android.content.Context
import androidx.room.Room
import com.example.netblockerpro.data.local.AppDatabase
import com.example.netblockerpro.data.local.dao.AppRuleDao
import com.example.netblockerpro.data.local.dao.ConnectionLogDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing Room database and DAOs.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration()
            .build()
    }
    
    @Provides
    @Singleton
    fun provideAppRuleDao(database: AppDatabase): AppRuleDao {
        return database.appRuleDao()
    }
    
    @Provides
    @Singleton
    fun provideConnectionLogDao(database: AppDatabase): ConnectionLogDao {
        return database.connectionLogDao()
    }
}
