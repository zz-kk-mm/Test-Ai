package com.space.core.network.service.errors.throwable_handler

import com.space.core.network.helper.connection.SPInternetConnectionHelper
import com.space.core.network.service.entity.SPInternalServerError
import com.space.core.network.service.entity.SPJobCancellationError
import com.space.core.network.service.entity.SPNetworkErrorEntity
import com.space.core.network.service.entity.SPNoConnectionError
import com.space.core.network.service.entity.SPServerTimeoutError
import com.space.core.network.service.entity.SPUnknownError
import com.space.core.network.service.errors.SPErrorWrapper
import com.space.core.network.service.response.SPResponseStatusCode
import kotlinx.coroutines.CancellationException
import java.io.IOException
import java.net.SocketTimeoutException

/**
 * @see SPNetworkExceptionHandler for documentation
 */
class SPNetworkExceptionHandlerImpl(
    private val internetConnectionChecker: SPInternetConnectionHelper
) : SPNetworkExceptionHandler {

    override suspend fun getErrorByThrowable(throwable: Throwable): SPNetworkErrorEntity {
        return when (throwable) {
            is SocketTimeoutException -> SPServerTimeoutError(
                SPErrorWrapper.empty(throwable.message, SPResponseStatusCode.GeneralHttpException)
            )

            is IOException -> getIoExceptionType(throwable)
            is SPNetworkErrorEntity -> throwable
            is CancellationException -> SPJobCancellationError(
                SPErrorWrapper.empty(throwable.message, SPResponseStatusCode.GeneralHttpException)
            )

            else -> SPUnknownError(
                SPErrorWrapper.empty(
                    throwable.message,
                    SPResponseStatusCode.GeneralHttpException
                ),
                initialExceptionName = throwable.javaClass.simpleName
            )
        }
    }

    /**
     * [getIoExceptionType] checks reason of the exception and returns specific [SPNetworkErrorEntity]
     *
     * @param throwable caught exception from service
     * @return specific [SPNetworkErrorEntity]
     */
    private suspend fun getIoExceptionType(throwable: Throwable): SPNetworkErrorEntity {
        return if (!internetConnectionChecker.isConnectionAvailable()) {
            SPNoConnectionError(
                SPErrorWrapper.empty(
                    throwable.message,
                    SPResponseStatusCode.GeneralHttpException
                )
            )
        } else {
            SPInternalServerError(
                SPErrorWrapper.empty(
                    throwable.message,
                    SPResponseStatusCode.GeneralHttpException
                )
            )
        }
    }
}
