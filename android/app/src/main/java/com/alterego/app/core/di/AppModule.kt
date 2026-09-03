package com.alterego.app.core.di

import com.alterego.app.core.analytics.Analytics
import com.alterego.app.core.analytics.LocalAnalytics
import com.alterego.app.core.content.MomentSelector
import com.alterego.app.core.scheduler.MomentPlanner
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {
    @Binds @Singleton
    abstract fun bindAnalytics(impl: LocalAnalytics): Analytics

    companion object {
        @Provides @Singleton fun provideMomentPlanner(): MomentPlanner = MomentPlanner()
        @Provides @Singleton fun provideMomentSelector(): MomentSelector = MomentSelector()
    }
}
