package com.space.core.network.service

import com.space.core.network.service.entity.SPNetworkErrorEntity

interface SPNetworkExceptionHelper {

    /**
     * Handle [throwable] in local API fail cases.
     *
     * If null is returned, thus local exception handler can't handle [throwable],
     * in that case [throwable] will be delegated to global exception handler
     *
     * @param throwable exception which occurred during API execution
     * @return [SPNetworkErrorEntity] or null
     */
    fun handleThrowable(throwable: Throwable): SPNetworkErrorEntity?
}
