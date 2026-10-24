package com.example.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class LocationData(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val accuracy: Float = 0f,
    val hasCoordinates: Boolean = false
)

/**
 * Manages mandatory GPS checks, runtime permission states,
 * and location retrieval for the GetAtMap application.
 */
class LocationStateManager(private val context: Context) {

    private val locationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

    private val fusedLocationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    private val _isGpsEnabled = MutableStateFlow(isLocationServiceEnabled())
    val isGpsEnabled: StateFlow<Boolean> = _isGpsEnabled.asStateFlow()

    private val _hasPermission = MutableStateFlow(checkLocationPermission())
    val hasPermission: StateFlow<Boolean> = _hasPermission.asStateFlow()

    private val _currentLocation = MutableStateFlow(LocationData())
    val currentLocation: StateFlow<LocationData> = _currentLocation.asStateFlow()

    private var receiverRegistered = false

    private val providerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == LocationManager.PROVIDERS_CHANGED_ACTION) {
                refreshStatus()
            }
        }
    }

    fun startListening() {
        if (!receiverRegistered) {
            try {
                val filter = IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.registerReceiver(providerReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
                } else {
                    context.registerReceiver(providerReceiver, filter)
                }
                receiverRegistered = true
            } catch (_: Exception) {
                // Ignore receiver registration issues
            }
        }
        refreshStatus()
    }

    fun stopListening() {
        if (receiverRegistered) {
            try {
                context.unregisterReceiver(providerReceiver)
            } catch (_: Exception) {
                // Ignore
            }
            receiverRegistered = false
        }
    }

    /**
     * Checks if GPS or any high-accuracy location provider is enabled on the device.
     */
    fun isLocationServiceEnabled(): Boolean {
        val manager = locationManager ?: return false
        val isGps = try {
            manager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        } catch (_: Exception) {
            false
        }
        val isNetwork = try {
            manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        } catch (_: Exception) {
            false
        }
        return isGps || isNetwork
    }

    /**
     * Checks if runtime location permission is granted.
     */
    fun checkLocationPermission(): Boolean {
        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fineGranted || coarseGranted
    }

    /**
     * Refreshes the internal StateFlows for both GPS switch and permission status.
     */
    fun refreshStatus() {
        val gps = isLocationServiceEnabled()
        val perm = checkLocationPermission()
        _isGpsEnabled.value = gps
        _hasPermission.value = perm

        if (gps && perm) {
            fetchLastLocation()
        }
    }

    /**
     * Requests fresh single location update from FusedLocationClient
     */
    @SuppressLint("MissingPermission")
    fun fetchLastLocation() {
        if (!checkLocationPermission()) return
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { loc: Location? ->
                if (loc != null) {
                    _currentLocation.value = LocationData(
                        latitude = loc.latitude,
                        longitude = loc.longitude,
                        accuracy = loc.accuracy,
                        hasCoordinates = true
                    )
                } else {
                    val tokenSource = CancellationTokenSource()
                    fusedLocationClient.getCurrentLocation(
                        Priority.PRIORITY_HIGH_ACCURACY,
                        tokenSource.token
                    ).addOnSuccessListener { highLoc: Location? ->
                        if (highLoc != null) {
                            _currentLocation.value = LocationData(
                                latitude = highLoc.latitude,
                                longitude = highLoc.longitude,
                                accuracy = highLoc.accuracy,
                                hasCoordinates = true
                            )
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // Ignore
        }
    }

    /**
     * Creates an Intent to launch the system Location Settings screen.
     */
    fun createLocationSettingsIntent(): Intent {
        return Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }
}
