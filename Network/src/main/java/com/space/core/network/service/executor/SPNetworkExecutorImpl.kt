package com.space.core.network.service.executor

import com.space.core.common.koin.inject
import com.space.core.network.service.builder.SPNetworkBuilder
import com.space.core.network.service.entity.SPNetworkErrorEntity
import com.space.core.network.service.errors.state_handler.SPNetworkErrorStateHandlerHandler
import com.space.core.network.service.executor.helper.SPNetworkExecutorHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * See documentation in [SPNetworkExecutor]
 */
class SPNetworkExecutorImpl(
    private val networkExecutorHelper: SPNetworkExecutorHelper
) : SPNetworkExecutor {

    override fun <T, E> launchNetworkOperation(
        coroutineScope: CoroutineScope,
        renewTokenRequest: Boolean,
        networkBuilder: SPNetworkBuilder<T, E>.() -> Unit
    ) {
        val errorHandler by inject<SPNetworkErrorStateHandlerHandler>()

        coroutineScope.launch {
            var errorEntity: SPNetworkErrorEntity? = null
            val builder = SPNetworkBuilder<T, E>().apply { networkBuilder() }

            try {
                builder.loadingLambda?.invoke(true, builder.loadingManager)

                builder.loadKoinModule?.invoke(true)

                builder.startLambda?.invoke()

                val result = builder.executionLambda?.invoke(coroutineScope)!!

                builder.loadKoinModule?.invoke(false)

                builder.success?.invoke(result)

                builder.suspendSuccess?.invoke(result)
            } catch (error: SPNetworkErrorEntity) {
                errorEntity = error

                builder.loadKoinModule?.invoke(false)

                networkExecutorHelper.handleNetworkError(
                    renewTokenRequest,
                    coroutineScope,
                    error,
                    errorHandler,
                    networkBuilder,
                    builder
                )
            } finally {
                if (networkExecutorHelper.shouldFinalizeRequest(errorEntity)) {
                    builder.finishLambda?.invoke()
                    builder.loadingLambda?.invoke(
                        false,
                        builder.loadingManager
                    )
                }
            }
        }
    }
}
