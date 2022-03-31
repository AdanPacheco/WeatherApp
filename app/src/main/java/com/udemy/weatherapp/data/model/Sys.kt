package com.udemy.weatherapp.data.model

import java.io.Serializable

data class Sys(
    val id: Int,
    val type: Int,
    val message: Double,
    val country: String,
    val sunrise: Long,
    val sunset: Long,
) : Serializable
