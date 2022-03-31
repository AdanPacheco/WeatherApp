package com.udemy.weatherapp.data.model

import java.io.Serializable


data class WeatherResponse(
    val coord: Coord,
    val weather: List<Weather>,
    val base: String,
    val main: Main,
    val visibility: Int,
    val wind: Wind,
    val clouds: Clouds,
    val sys: Sys,
    val timezone: Int,
    val dt: Int,
    val name: String,
    val code: Int,
    val id: Int
) : Serializable