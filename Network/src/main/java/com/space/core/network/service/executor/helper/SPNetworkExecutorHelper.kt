package com.space.core.network.service.executor.helper

import com.space.core.network.service.builder.SPNetworkBuilder
import com.space.core.network.service.entity.SPNetworkErrorEntity
import com.space.core.network.service.errors.state_handler.SPNetworkErrorStateHandlerHandler
import kotlinx.coroutines.CoroutineScope

/**
 * [SPNetworkExecutorHelper] is a helper class for [SPNetworkExecutorHelper], it is responsible to handle caught errors during network execution
 */
interface SPNetworkExecutorHelper {

    /**
     * [shouldFinalizeRequest] function checks current error entity type and returns appropriate boolean value
     * For instance: when we have ErrorEntity.StrongAuthenticationError case,
     * we return false to avoid execution of finishLambda and loadingLambda functions
     *
     * @param errorEntity we check error type by its value
     */
    fun shouldFinalizeRequest(errorEntity: SPNetworkErrorEntity?): Boolean

    /**
     * [handleNetworkError] checks [renewTokenRequest] and according its value, it starts concrete error handling flow
     *
     * @param T - Response type
     * @param E - Error type
     * @param renewTokenRequest - Boolean value which represents if it is token renew service or not
     * @param coroutineScope - is a coroutine scope
     * @param error - [SPNetworkErrorEntity] type, used for error type clarification
     * @param errorHandler - [SPNetworkErrorStateHandlerHandler] help us to handle error states
     */
    suspend fun <T, E> handleNetworkError(
        renewTokenRequest: Boolean,
        coroutineScope: CoroutineScope,
        error: SPNetworkErrorEntity,
        errorHandler: SPNetworkErrorStateHandlerHandler?,
        operationBuilderFunc: SPNetworkBuilder<T, E>.() -> Unit,
        builder: SPNetworkBuilder<T, E>
    )
}
