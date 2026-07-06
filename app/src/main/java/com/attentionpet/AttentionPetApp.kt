package com.attentionpet

import android.app.Application
import androidx.room.Room
import com.attentionpet.data.AttentionPetDatabase
import com.attentionpet.data.AttentionPetRepository

class AttentionPetApp : Application() {
    lateinit var repository: AttentionPetRepository
        private set

    override fun onCreate() {
        super.onCreate()
        val database = Room.databaseBuilder(
            this,
            AttentionPetDatabase::class.java,
            "attention-pet.db"
        ).build()
        repository = AttentionPetRepository(
            database.configDao(),
            database.sessionDao(),
            database.eventDao()
        )
    }
}
