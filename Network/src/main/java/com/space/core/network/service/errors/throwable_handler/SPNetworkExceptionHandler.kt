package com.space.core.network.service.errors.throwable_handler

import com.space.core.network.service.entity.SPNetworkErrorEntity

/**
 * [SPNetworkExceptionHandler] is a helper, handler class to qualify returned error by its type
 */
interface SPNetworkExceptionHandler {

    /**
     * [getErrorByThrowable] checks [throwable] type and returns appropriate [SPNetworkErrorEntity] type
     *
     * @param throwable caught, returned error from service
     * @return appropriate [SPNetworkErrorEntity] type
     */
    suspend fun getErrorByThrowable(throwable: Throwable): SPNetworkErrorEntity
}
