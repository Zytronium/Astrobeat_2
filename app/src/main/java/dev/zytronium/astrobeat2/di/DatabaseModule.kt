package dev.zytronium.astrobeat2.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.zytronium.astrobeat2.data.local.AppDatabase
import dev.zytronium.astrobeat2.data.local.dao.TrackDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "astrobeat2.db",
        ).build()

    @Provides
    fun provideTrackDao(db: AppDatabase): TrackDao = db.trackDao()
}
