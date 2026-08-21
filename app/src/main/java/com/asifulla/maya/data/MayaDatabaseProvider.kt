package com.asifulla.maya.data

import android.content.Context
import androidx.room.Room

object MayaDatabaseProvider {
    @Volatile private var instance: MayaDatabase? = null

    fun get(context: Context): MayaDatabase = instance ?: synchronized(this) {
        instance ?: Room.databaseBuilder(
            context.applicationContext,
            MayaDatabase::class.java,
            "maya.db"
        ).fallbackToDestructiveMigration().build().also { instance = it }
    }
}
