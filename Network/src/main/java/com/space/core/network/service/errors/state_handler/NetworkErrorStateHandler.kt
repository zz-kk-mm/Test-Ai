package com.space.core.network.service.errors.state_handler

import com.space.core.network.service.entity.SPNetworkErrorEntity
import com.space.core.network.service.response.SPResponseBaseErrorType
import kotlinx.coroutines.CoroutineScope

/**
 *
 * [SPNetworkErrorStateHandlerHandler] is a core network error state handler interface
 *
 * See Core network error state:
 * * [handleErrorEntityState]
 * * [handleRefreshTokenState]
 * * [handleNoConnectionErrorState]
 * * [handleServerTimeoutErrorState]
 * * [handleAppMustUpdateState]
 *
 * Based on project requirements you can create new error handler interface which will be extended by core [SPNetworkErrorStateHandlerHandler]
 */
interface SPNetworkErrorStateHandlerHandler {

    /**
     * [handleErrorEntityState] handles error by [SPNetworkErrorEntity] type
     */
    suspend fun handleErrorEntityState(error: SPNetworkErrorEntity)

    /**
     * App needs to refresh token
     */
    suspend fun handleRefreshTokenState(
        coroutineScope: CoroutineScope,
        onRenewed: suspend () -> Unit
    )

    /**
     * network connection problem
     */
    fun handleNoConnectionErrorState() = Unit

    /**
     * network request timed out
     */
    fun handleServerTimeoutErrorState() = Unit

    /**
     * network request must update
     */
    fun handleAppMustUpdateState(message: String) = Unit

    /**
     * network request passport renewal
     */
    fun handleGlobalAction(type: SPResponseBaseErrorType, message: String)
}
