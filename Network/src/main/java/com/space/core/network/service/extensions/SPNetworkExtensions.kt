package com.space.core.network.service.extensions

import com.space.core.common.koin.inject
import com.space.core.network.service.builder.SPNetworkBuilder
import com.space.core.network.service.executor.SPNetworkExecutor
import kotlinx.coroutines.CoroutineScope

/**
 * [networkModuleOperation] is a wrapper extension function of [CoroutineScope], It injects [SPNetworkExecutor] class automatically and use launchNetworkOperation function
 *
 * [networkModuleOperation] gives possibility to use [SPNetworkExecutor] outside of viewModels, without injecting [SPNetworkExecutor] class
 *
 * @param networkBuilder - [SPNetworkBuilder] which stores service builder configuration
 */
@JvmName("networkModuleOperation")
fun <T : Any> CoroutineScope.networkModuleOperation(
    networkBuilder: SPNetworkBuilder<T, Unit>.() -> Unit
) {
    networkModuleOperation<T, Unit>(networkBuilder)
}

@JvmName("networkModuleOperationWithError")
fun <T : Any, E : Any?> CoroutineScope.networkModuleOperation(
    networkBuilder: SPNetworkBuilder<T, E>.() -> Unit
) {
    val networkExecutor by inject<SPNetworkExecutor>()
    networkExecutor.launchNetworkOperation(this, false, networkBuilder)
}
