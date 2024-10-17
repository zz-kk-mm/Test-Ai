package com.space.core.network.helper.retrofit

import com.space.core.network.service.SPNetworkExceptionHelper
import com.space.core.network.service.errors.throwable_handler.SPNetworkExceptionHandler
import com.space.core.network.service.response.SPResponse
import com.space.core.network.service.result_handler.SPServiceResultHandler
import retrofit2.Response

/**
 * @see [SPRetrofitHelper] for additional documentation
 */
class SPRetrofitHelperImpl(
    private val resultHandler: SPServiceResultHandler,
    private val networkExceptionHandler: SPNetworkExceptionHandler,
    private val networkExceptionHelper: SPNetworkExceptionHelper? = null
) : SPRetrofitHelper {

    override suspend fun <DTO : Any, SERVICE_DATA : SPResponse<DTO>> apiExecute(
        apiCall: suspend () -> Response<SERVICE_DATA>
    ): DTO {
        return try {
            resultHandler.getResult(apiCall())
        } catch (throwable: Throwable) {
            throw networkExceptionHelper?.handleThrowable(throwable)
                ?: networkExceptionHandler.getErrorByThrowable(throwable)
        }
    }

    override suspend fun <DTO : Any> apiExecuteV2(apiCall: suspend () -> Response<DTO>): DTO {
        return try {
            resultHandler.getResultV2(apiCall())
        } catch (throwable: Throwable) {
            throw networkExceptionHelper?.handleThrowable(throwable)
                ?: networkExceptionHandler.getErrorByThrowable(throwable)
        }
    }
}
