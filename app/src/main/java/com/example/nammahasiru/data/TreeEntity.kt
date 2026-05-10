package com.example.nammahasiru.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trees")
data class TreeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val speciesName: String,
    val latitude: Double,
    val longitude: Double,
    val datePlanted: Long,
    val status: String = "Planted", // Planted, Survived, Died
    val photoUri: String? = null
)
