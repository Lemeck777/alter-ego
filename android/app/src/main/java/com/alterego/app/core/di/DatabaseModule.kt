package com.alterego.app.core.di

import android.content.Context
import androidx.room.Room
import com.alterego.app.core.database.AppDatabase
import com.alterego.app.core.database.ContentDao
import com.alterego.app.core.database.CustomContentDao
import com.alterego.app.core.database.DeliveryDao
import com.alterego.app.core.database.JourneyDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME)
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()

    @Provides fun provideContentDao(db: AppDatabase): ContentDao = db.contentDao()
    @Provides fun provideJourneyDao(db: AppDatabase): JourneyDao = db.journeyDao()
    @Provides fun provideDeliveryDao(db: AppDatabase): DeliveryDao = db.deliveryDao()
    @Provides fun provideCustomContentDao(db: AppDatabase): CustomContentDao = db.customContentDao()
}
