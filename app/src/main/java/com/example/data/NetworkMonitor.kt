package com.example.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object NetworkMonitor {

    private val _networkState = MutableStateFlow(
        NetworkState(
            isOnline = true,
            isValidated = true,
            transport = NetworkTransport.WIFI,
            label = "ONLINE (Wi-Fi Validated)"
        )
    )
    val networkState: StateFlow<NetworkState> = _networkState.asStateFlow()

    // Simulation override for developer & user testing
    private var simulationOverride: Boolean? = null

    private var connectivityManager: ConnectivityManager? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var isRegistered = false

    fun startMonitoring(context: Context) {
        if (isRegistered) return
        val appContext = context.applicationContext
        connectivityManager = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val cm = connectivityManager ?: return

        // Evaluate initial network state
        evaluateCurrentNetwork(cm)

        // Build request for validated internet capabilities
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                evaluateCurrentNetwork(cm)
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                evaluateCurrentNetwork(cm)
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                super.onCapabilitiesChanged(network, networkCapabilities)
                evaluateCurrentNetwork(cm)
            }
        }

        try {
            cm.registerNetworkCallback(request, networkCallback!!)
            isRegistered = true
        } catch (_: Exception) {
            // Fallback: Default to online if registration not permitted
        }
    }

    private fun evaluateCurrentNetwork(cm: ConnectivityManager) {
        if (simulationOverride != null) {
            applySimulation(simulationOverride!!)
            return
        }

        val activeNetwork = cm.activeNetwork
        if (activeNetwork == null) {
            updateState(
                isOnline = false,
                isValidated = false,
                transport = NetworkTransport.NONE,
                label = "OFFLINE (No Network Connection)"
            )
            return
        }

        val capabilities = cm.getNetworkCapabilities(activeNetwork)
        if (capabilities == null) {
            updateState(
                isOnline = false,
                isValidated = false,
                transport = NetworkTransport.NONE,
                label = "OFFLINE (No Network Capabilities)"
            )
            return
        }

        val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        val isValidated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        val isOnlineNow = hasInternet && isValidated

        val transport = when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkTransport.WIFI
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkTransport.CELLULAR
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkTransport.ETHERNET
            else -> NetworkTransport.OTHER
        }

        val label = if (isOnlineNow) {
            "ONLINE (${transport.displayName} Validated)"
        } else if (hasInternet && !isValidated) {
            "OFFLINE (${transport.displayName} Captive / Not Validated)"
        } else {
            "OFFLINE (SIM & Local Storage Only)"
        }

        updateState(
            isOnline = isOnlineNow,
            isValidated = isValidated,
            transport = transport,
            label = label
        )
    }

    private fun updateState(
        isOnline: Boolean,
        isValidated: Boolean,
        transport: NetworkTransport,
        label: String
    ) {
        val previous = _networkState.value.isOnline
        _networkState.value = NetworkState(
            isOnline = isOnline,
            isValidated = isValidated,
            transport = transport,
            label = label
        )

        // Sync with NillBookRepository
        if (previous != isOnline) {
            NillBookRepository.setOnlineMode(isOnline)
        }
    }

    // Manual simulation override for testing in emulator or by user
    fun setSimulationOverride(online: Boolean?) {
        simulationOverride = online
        if (online != null) {
            applySimulation(online)
        } else {
            connectivityManager?.let { evaluateCurrentNetwork(it) }
        }
    }

    fun toggleSimulationMode() {
        val current = _networkState.value.isOnline
        setSimulationOverride(!current)
    }

    private fun applySimulation(online: Boolean) {
        val transport = if (online) NetworkTransport.WIFI else NetworkTransport.NONE
        val label = if (online) "ONLINE (Simulated Wi-Fi)" else "OFFLINE (Simulated No-Internet)"
        updateState(
            isOnline = online,
            isValidated = online,
            transport = transport,
            label = label
        )
    }
}
