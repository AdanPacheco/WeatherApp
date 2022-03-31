package com.udemy.weatherapp.core

import android.content.Context

class Prefs(context: Context) {

    val storage = context.getSharedPreferences(Constants.PREFERENCE_NAME , 0)
    fun saveWeather(weather: String) = storage.edit().putString(Constants.WEATHER_RESPONSE_DATA, weather).apply()
    fun getWeather(): String = storage.getString(Constants.WEATHER_RESPONSE_DATA, "")!!
}