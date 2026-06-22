package dev.zytronium.astrobeat2.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import dev.zytronium.astrobeat2.data.local.converter.Converters
import dev.zytronium.astrobeat2.data.local.dao.TrackDao
import dev.zytronium.astrobeat2.data.local.entity.TrackEntity

@Database(
    entities = [TrackEntity::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun trackDao(): TrackDao
}
