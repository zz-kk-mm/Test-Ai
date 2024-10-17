package com.space.core.network.service.executor.helper

import androidx.annotation.VisibleForTesting
import com.space.core.network.service.builder.SPNetworkBuilder
import com.space.core.network.service.entity.SPNetworkErrorEntity
import com.space.core.network.service.errors.state_handler.SPNetworkErrorStateHandlerHandler
import kotlinx.coroutines.CoroutineScope

@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
class SPNetworkExecutorHelperFakeImpl : SPNetworkExecutorHelper {

    override fun shouldFinalizeRequest(errorEntity: SPNetworkErrorEntity?): Boolean {
        return true
    }

    override suspend fun <T, E> handleNetworkError(
        renewTokenRequest: Boolean,
        coroutineScope: CoroutineScope,
        error: SPNetworkErrorEntity,
        errorHandler: SPNetworkErrorStateHandlerHandler?,
        operationBuilderFunc: SPNetworkBuilder<T, E>.() -> Unit,
        builder: SPNetworkBuilder<T, E>
    ) {
        builder.errorLambda?.invoke(error, error.data.getErrorData<E>(builder.errorTypeToken))
    }
}
