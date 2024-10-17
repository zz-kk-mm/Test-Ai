package com.space.core.network.service.errors

import com.google.gson.Gson
import com.space.core.network.constants.SPNetworkConstant
import com.space.core.network.service.response.SPResponseBaseErrorType
import com.space.core.network.service.response.SPResponseStatusCode
import java.lang.reflect.Type

data class SPErrorWrapper(
    val traceId: String?,
    val externalEndpoint: String?,
    val validationErrors: Map<String, List<String>>?,
    val errorCode: SPResponseBaseErrorType,
    val type: String,
    val title: String?,
    val status: Int?,
    val detail: String?,
    val instance: String?,
    val extensions: Any?
) {

    val errorMessage: String get() = detail ?: SPNetworkConstant.UNKNOWN_ERROR

    /**
     * Get Error data with deserializing [extensions] and then serializing it under [T] type
     *
     * @param T generic of error data
     * @param type saved type of error data
     * @return Error data of [T] type
     */
    fun <T> getErrorData(type: Type): T = gson.fromJson(gson.toJson(extensions), type)

    companion object {

        private val gson = Gson()

        fun empty(
            message: String? = null,
            errorCode: SPResponseBaseErrorType = SPResponseStatusCode.UnknownError,
            instance: String? = null
        ) = SPErrorWrapper(
            traceId = null,
            externalEndpoint = null,
            validationErrors = null,
            errorCode = errorCode,
            type = errorCode.value,
            title = null,
            status = null,
            detail = message,
            instance = instance,
            extensions = null
        )
    }
}
