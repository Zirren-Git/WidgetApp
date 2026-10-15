package com.zirren.tessera.widget

import com.zirren.tessera.R
import com.zirren.tessera.core.weather.WeatherIcon

/** Tessera's hand-drawn weather icons (vector drawables that follow day/night colours). */
object WeatherIcons {
    fun drawable(icon: WeatherIcon?): Int = when (icon) {
        WeatherIcon.CLEAR_DAY -> R.drawable.ic_weather_clear_day
        WeatherIcon.CLEAR_NIGHT -> R.drawable.ic_weather_clear_night
        WeatherIcon.PARTLY_CLOUDY_DAY -> R.drawable.ic_weather_partly_cloudy_day
        WeatherIcon.PARTLY_CLOUDY_NIGHT -> R.drawable.ic_weather_partly_cloudy_night
        WeatherIcon.FOG -> R.drawable.ic_weather_fog
        WeatherIcon.DRIZZLE -> R.drawable.ic_weather_drizzle
        WeatherIcon.RAIN -> R.drawable.ic_weather_rain
        WeatherIcon.HEAVY_RAIN -> R.drawable.ic_weather_heavy_rain
        WeatherIcon.SHOWERS_DAY -> R.drawable.ic_weather_showers_day
        WeatherIcon.SHOWERS_NIGHT -> R.drawable.ic_weather_showers_night
        WeatherIcon.SLEET -> R.drawable.ic_weather_sleet
        WeatherIcon.SNOW -> R.drawable.ic_weather_snow
        WeatherIcon.THUNDER -> R.drawable.ic_weather_thunder
        WeatherIcon.CLOUDY, null -> R.drawable.ic_weather_cloudy
    }
}
