package com.space.core.network.service.executor

import com.space.core.network.service.builder.SPNetworkBuilder
import kotlinx.coroutines.CoroutineScope

/**
 * [SPNetworkExecutor] is responsible to execute network operations
 */
interface SPNetworkExecutor {

    /**
     * [launchNetworkOperation] launches network operations and executes services from viewModels
     *
     * @param T Response Type
     * @param E Error Type
     * @param coroutineScope is a scope for network operation, if scope will be cancelled operation will be destroyed as well
     * @param renewTokenRequest denotes if request happens after renew token or not, as a default it is always false
     * @param networkBuilder is a network Builder, which stores network building configuration, for additional information see [SPNetworkBuilder]
     */
    fun <T, E> launchNetworkOperation(
        coroutineScope: CoroutineScope,
        renewTokenRequest: Boolean = false,
        networkBuilder: SPNetworkBuilder<T, E>.() -> Unit
    )
}
