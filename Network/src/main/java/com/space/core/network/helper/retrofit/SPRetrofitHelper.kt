package com.space.core.network.helper.retrofit

import com.space.core.network.service.response.SPResponse
import retrofit2.Response

/**
 * [SPRetrofitHelper] executes api calls, Returns DTO objects or Throws Exceptions
 */
interface SPRetrofitHelper {

    /**
     * @param DTO is a returning response type (DTO)
     * @param SERVICE_DATA is a response from service which contains data [DTO] and status code
     * @param apiCall is a suspend lambda function, which returns some data from remote source
     */
    suspend fun <DTO : Any, SERVICE_DATA : SPResponse<DTO>> apiExecute(
        apiCall: suspend () -> Response<SERVICE_DATA>
    ): DTO

    /**
     * @param DTO is a returning response type (DTO)
     * @param apiCall is a suspend lambda function, which returns some data from remote source
     */
    suspend fun <DTO : Any> apiExecuteV2(
        apiCall: suspend () -> Response<DTO>
    ): DTO
}
