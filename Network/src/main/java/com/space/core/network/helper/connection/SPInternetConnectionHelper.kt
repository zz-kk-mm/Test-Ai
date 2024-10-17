package com.space.core.network.helper.connection

/**
 * [SPInternetConnectionHelper] is responsible to check internet connectivity state
 */
interface SPInternetConnectionHelper {

    /**
     * [isConnectionAvailable] checks connection state by pinging to google host
     *
     * @return true if internet connection is available
     */
    suspend fun isConnectionAvailable(): Boolean
}
