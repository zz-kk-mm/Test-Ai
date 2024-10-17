package com.space.core.network.service.result_handler

import com.space.core.network.service.entity.SPNetworkErrorEntity
import com.space.core.network.service.response.SPResponse
import retrofit2.Response

/**
 * [SPServiceResultHandler] class handles process to transform service data to DTO
 * If service response won't be successful it will throw [SPNetworkErrorEntity] exception
 */
interface SPServiceResultHandler {

    /**
     * @param DTO is a return type (Data transfer object)
     * @param SERVICE_DATA is a return type (Data transfer object)
     */
    suspend fun <DTO : Any, SERVICE_DATA : SPResponse<DTO>> getResult(
        response: Response<SERVICE_DATA>
    ): DTO

    /**
     * @param response The Retrofit response containing the result of the API call.
     * @return The response body if the request was successful, or [Unit] if the response
     * has no content (e.g. 204 status).
     * @throws [SPNetworkErrorEntity] If the API call returns error.
     */
    suspend fun <DTO : Any> getResultV2(
        response: Response<DTO>
    ): DTO
}
