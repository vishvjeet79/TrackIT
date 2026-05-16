package com.example.trackit.data

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket

private const val SERVICE_TYPE = "_trackit_sync._tcp"
private const val TAG = "LocalSyncManager"

class LocalSyncManager(context: Context) {

    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private var serverSocket: ServerSocket? = null
    private var registrationListener: NsdManager.RegistrationListener? = null

    fun startServer(onClientConnected: suspend (OutputStream) -> Unit) {
        stopServer()
        Thread {
            try {
                val ss = ServerSocket(0)
                serverSocket = ss
                val port = ss.localPort
                registerService(port)

                Log.d(TAG, "Server started on port $port")
                while (!ss.isClosed) {
                    val client = try {
                        ss.accept()
                    } catch (e: java.net.SocketException) {
                        if (ss.isClosed) {
                            Log.d(TAG, "Server socket closed intentionally")
                        } else {
                            Log.e(TAG, "Socket error during accept", e)
                        }
                        break
                    }

                    Log.d(TAG, "Client connected: ${client.inetAddress}")
                    Thread {
                        try {
                            client.use {
                                val output = it.getOutputStream()
                                val input = it.getInputStream()
                                
                                // Simple handshake
                                val buffer = ByteArray(1024)
                                val read = input.read(buffer)
                                if (read > 0 && String(buffer, 0, read) == "GET_DATA") {
                                    kotlinx.coroutines.runBlocking {
                                        onClientConnected(output)
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error handling client", e)
                        }
                    }.start()
                }
            } catch (e: Exception) {
                // Only log if it's not a closed socket exception which is expected during stop
                if (!(e is java.net.SocketException && e.message?.contains("closed", ignoreCase = true) == true)) {
                    Log.e(TAG, "Server error", e)
                }
            }
        }.start()
    }

    fun stopServer() {
        try {
            serverSocket?.close()
            serverSocket = null
            registrationListener?.let { nsdManager.unregisterService(it) }
            registrationListener = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping server", e)
        }
    }

    private fun registerService(port: Int) {
        val serviceInfo = NsdServiceInfo().apply {
            serviceName = "TrackIT_${android.os.Build.MODEL}"
            serviceType = SERVICE_TYPE
            setPort(port)
        }

        registrationListener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(NsdServiceInfo: NsdServiceInfo) {
                Log.d(TAG, "Service registered: ${NsdServiceInfo.serviceName}")
            }

            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e(TAG, "Registration failed: $errorCode")
            }

            override fun onServiceUnregistered(arg0: NsdServiceInfo) {}
            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
        }

        nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
    }

    fun discoverServices(): Flow<List<NsdServiceInfo>> = callbackFlow {
        val foundServices = mutableListOf<NsdServiceInfo>()
        
        val discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {
                Log.d(TAG, "Discovery started")
            }

            override fun onDiscoveryStopped(regType: String) {
                Log.d(TAG, "Discovery stopped")
            }

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                if (serviceInfo.serviceType == SERVICE_TYPE || serviceInfo.serviceType == "$SERVICE_TYPE.") {
                    Log.d(TAG, "Service found: ${serviceInfo.serviceName}")
                    nsdManager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                            Log.e(TAG, "Resolve failed: $errorCode")
                        }

                        override fun onServiceResolved(resolvedService: NsdServiceInfo) {
                            Log.d(TAG, "Service resolved: ${resolvedService.host}:${resolvedService.port}")
                            foundServices.add(resolvedService)
                            trySend(foundServices.toList())
                        }
                    })
                }
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                foundServices.removeAll { it.serviceName == serviceInfo.serviceName }
                trySend(foundServices.toList())
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "Start discovery failed: $errorCode")
                close()
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "Stop discovery failed: $errorCode")
                close()
            }
        }

        nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        
        awaitClose {
            nsdManager.stopServiceDiscovery(discoveryListener)
        }
    }

    suspend fun receiveData(service: NsdServiceInfo, onDataReceived: (InputStream) -> Unit) = withContext(Dispatchers.IO) {
        Socket(service.host, service.port).use { socket ->
            socket.soTimeout = 30000
            val output = socket.getOutputStream()
            
            output.write("GET_DATA".toByteArray())
            output.flush()
            
            onDataReceived(socket.getInputStream())
        }
    }
}
