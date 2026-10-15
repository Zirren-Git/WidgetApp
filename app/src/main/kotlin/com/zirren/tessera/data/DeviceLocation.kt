package com.zirren.tessera.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Looper
import com.zirren.tessera.core.model.GeoPoint
import java.io.IOException
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * "Use my location", without Google Play services: plain [LocationManager], approximate
 * permission only. Called while the app is open, so no background location permission is needed.
 */
class DeviceLocation(private val context: Context) {
    private val manager: LocationManager? = context.getSystemService(LocationManager::class.java)
    private val callbacks = Executors.newSingleThreadExecutor()

    fun hasPermission(): Boolean =
        context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    /** Blocking; returns null if no fix arrives in time. Call from a background thread. */
    @SuppressLint("MissingPermission")
    fun locate(timeoutMillis: Long = 20_000): GeoPoint? {
        val lm = manager ?: return null
        if (!hasPermission()) return null
        val providers = providers(lm)
        if (providers.isEmpty()) return null

        lastKnown(lm, providers)?.takeIf { System.currentTimeMillis() - it.time < MAX_AGE_MILLIS }?.let { return it.toPoint() }

        val latch = CountDownLatch(1)
        var fix: Location? = null
        val provider = providers.first()
        if (Build.VERSION.SDK_INT >= 30) {
            val signal = CancellationSignal()
            lm.getCurrentLocation(provider, signal, callbacks) { location ->
                fix = location
                latch.countDown()
            }
            if (!latch.await(timeoutMillis, TimeUnit.MILLISECONDS)) signal.cancel()
        } else {
            // Before Android 10 these LocationListener methods have no default bodies, so all four are implemented.
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    fix = location
                    latch.countDown()
                }

                @Deprecated("Required on Android 9 and older")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
                override fun onProviderEnabled(provider: String) = Unit
                override fun onProviderDisabled(provider: String) = Unit
            }
            @Suppress("DEPRECATION")
            lm.requestSingleUpdate(provider, listener, Looper.getMainLooper())
            if (!latch.await(timeoutMillis, TimeUnit.MILLISECONDS)) lm.removeUpdates(listener)
        }
        return (fix ?: lastKnown(lm, providers))?.toPoint()
    }

    /** Town name for a point, when the phone has a geocoder (most do; some de-Googled phones don't). */
    @Suppress("DEPRECATION")
    fun placeName(point: GeoPoint, locale: Locale): String? {
        if (!Geocoder.isPresent()) return null
        return try {
            Geocoder(context, locale).getFromLocation(point.latitude, point.longitude, 1)
                ?.firstOrNull()
                ?.let { it.locality ?: it.subAdminArea ?: it.adminArea }
        } catch (e: IOException) {
            null
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    @SuppressLint("MissingPermission")
    private fun lastKnown(lm: LocationManager, providers: List<String>): Location? =
        providers.mapNotNull { provider ->
            try {
                lm.getLastKnownLocation(provider)
            } catch (e: SecurityException) {
                null
            } catch (e: IllegalArgumentException) {
                null
            }
        }.maxByOrNull { it.time }

    private fun providers(lm: LocationManager): List<String> {
        val enabled = try {
            lm.getProviders(true)
        } catch (e: SecurityException) {
            emptyList<String>()
        }
        return PREFERRED.filter { it in enabled }
    }

    private fun Location.toPoint(): GeoPoint? = try {
        // ~1 km is plenty for weather, and it's all Tessera ever stores.
        GeoPoint(latitude, longitude, if (hasAltitude()) altitude.toInt() else null).rounded(2)
    } catch (e: IllegalArgumentException) {
        null
    }

    private companion object {
        const val MAX_AGE_MILLIS = 30 * 60 * 1000L
        // "fused" exists on Android 12+ without Google Play services.
        val PREFERRED = listOf("fused", LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER, LocationManager.PASSIVE_PROVIDER)
    }
}
