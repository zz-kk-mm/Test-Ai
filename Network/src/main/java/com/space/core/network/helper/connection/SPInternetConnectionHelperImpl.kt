package com.space.core.network.helper.connection

import java.lang.Exception
import java.net.InetSocketAddress
import java.net.Socket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SPInternetConnectionHelperImpl : SPInternetConnectionHelper {

    override suspend fun isConnectionAvailable(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val socket = Socket()
                socket.connect(InetSocketAddress("8.8.8.8", 53), 3000)
                socket.close()
                true
            } catch (e: Exception) {
                false
            }
        }
    }
}
