package com.space.core.network.service.result_handler

import com.google.gson.Gson
import com.space.core.common.koin.inject
import com.space.core.network.constants.SPNetworkConstant.AUTHENTICATION_HEADER_ID
import com.space.core.network.constants.SPNetworkConstant.SCA_REQUIREMENTS_ID
import com.space.core.network.constants.SPNetworkConstant.TRACE_ID
import com.space.core.network.constants.SPNetworkConstant.UNKNOWN_ERROR
import com.space.core.network.constants.SPNetworkConstant.UNTRUSTED_AUTH_ID
import com.space.core.network.constants.SPNetworkConstant.UNTRUSTED_USER_ID
import com.space.core.network.constants.SPNetworkConstant.UNTRUSTED_USER_NAME
import com.space.core.network.service.entity.SPDocumentExpiredError
import com.space.core.network.service.entity.SPInternalServerError
import com.space.core.network.service.entity.SPNetworkErrorEntity
import com.space.core.network.service.entity.SPSCACodeRequiredError
import com.space.core.network.service.entity.SPStrongAuthenticationError
import com.space.core.network.service.entity.SPUnauthorizedError
import com.space.core.network.service.entity.SPUnauthorizedUserDetailsError
import com.space.core.network.service.entity.SPUnknownError
import com.space.core.network.service.entity.SPUntrustedDeviceLivenessError
import com.space.core.network.service.entity.SPUpdateRequiredError
import com.space.core.network.service.errors.SPErrorWrapper
import com.space.core.network.service.errors.state_handler.SPNetworkErrorStateHandlerHandler
import com.space.core.network.service.response.SPNewResponseStatusCode
import com.space.core.network.service.response.SPResponse
import com.space.core.network.service.response.SPResponseBaseErrorType
import com.space.core.network.service.response.SPResponseStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import retrofit2.Response

/**
 * See documentation in [SPServiceResultHandler]
 */
class SPServiceResultHandlerImpl(private val gson: Gson) : SPServiceResultHandler {

    override suspend fun <DTO : Any, SERVICE_DATA : SPResponse<DTO>> getResult(
        response: Response<SERVICE_DATA>
    ): DTO = with(response) {
        return when {
            response.isSuccessful -> handleSuccessfulFlow()
            else -> handleFailedFlow(
                data = body()?.data,
                headers = headers(),
                initialErrorCode = getInitialErrorCode(),
                errorWrapper = SPErrorWrapper.empty(
                    errorCode = getStatusCode(),
                    instance = getEncodedPath()
                )
            )
        }
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun <DTO : Any> getResultV2(response: Response<DTO>): DTO {
        return when {
            response.isSuccessful -> response.body() ?: Unit as DTO
            else -> {
                val error = deserializeNewError(response)
                    ?: SPErrorWrapper.empty(instance = response.getEncodedPath())
                handleFailedFlowNew(
                    data = response.body(),
                    errorWrapper = error,
                    headers = response.headers(),
                    initialErrorCode = error.type
                )
            }
        }
    }

    /**
     * [handleSuccessfulFlow] is executed if response isSuccessful
     * It checks [SPResponseStatusCode] and returns data or throws exception
     */
    private suspend fun <DTO : Any, SERVICE_DATA : SPResponse<DTO>> Response<SERVICE_DATA>.handleSuccessfulFlow(): DTO {
        val errorMessage = body()?.status?.message
        return when (SPResponseStatusCode.fromValue(body()?.status?.type!!)) {
            SPResponseStatusCode.success -> body()?.data
                ?: throw SPUnknownError(
                    data = SPErrorWrapper.empty(errorMessage, getStatusCode(), getEncodedPath()),
                    initialErrorCode = getInitialErrorCode()
                )

            else -> handleFailedFlow(
                data = body()?.data,
                headers = headers(),
                initialErrorCode = getInitialErrorCode(),
                errorWrapper = SPErrorWrapper.empty(errorMessage, getStatusCode(), getEncodedPath())
            )
        }
    }

    /**
     * [handleFailedFlow] will be executed when response is not successful
     * It throws exception
     */
    private suspend fun <DTO : Any> handleFailedFlow(
        data: DTO?,
        errorWrapper: SPErrorWrapper,
        headers: okhttp3.Headers,
        initialErrorCode: String
    ): DTO {
        val statusCode = errorWrapper.errorCode
        val errorMsg = errorWrapper.detail

        return when (statusCode) {
            SPResponseStatusCode.httpUnauthorized -> throw unauthorizedError(errorWrapper)
            SPResponseStatusCode.INTERNAL_SERVER_ERROR -> throw SPInternalServerError(
                data = errorWrapper
            )
            SPResponseStatusCode.updateRequired -> throw SPUpdateRequiredError(data = errorWrapper)
            SPResponseStatusCode.ExistingDocumentExpiredTransfer,
            SPResponseStatusCode.ExistingDocumentExpiredProduct,
            SPResponseStatusCode.ExistingDocumentInactiveTransfer,
            SPResponseStatusCode.ExistingDocumentInactiveProduct,
            SPResponseStatusCode.ExistingDocumentExpires,
            SPResponseStatusCode.ExistingDocumentExpired -> handleDocumentError(
                data,
                statusCode,
                errorMsg!!
            )

            SPResponseStatusCode.AuthenticationCodeRequired -> throw SPStrongAuthenticationError(
                headers[AUTHENTICATION_HEADER_ID] ?: UNKNOWN_ERROR,
                data = errorWrapper
            )

            SPResponseStatusCode.UntrustedDeviceLivenessCheckRequired -> throw SPUntrustedDeviceLivenessError(
                headers[UNTRUSTED_AUTH_ID],
                headers[UNTRUSTED_USER_ID] ?: UNKNOWN_ERROR,
                headers[UNTRUSTED_USER_NAME] ?: UNKNOWN_ERROR,
                data = errorWrapper
            )

            SPResponseStatusCode.ScaCodeRequired -> throw SPSCACodeRequiredError(
                requirements = headers[SCA_REQUIREMENTS_ID],
                traceId = headers[TRACE_ID],
                data = errorWrapper
            )

            else -> throw SPUnknownError(data = errorWrapper, initialErrorCode)
        }
    }

    /**
     * [handleFailedFlow] will be executed when response is not successful
     * It throws exception
     */
    private suspend fun <DTO : Any> handleFailedFlowNew(
        data: DTO?,
        errorWrapper: SPErrorWrapper,
        headers: okhttp3.Headers,
        initialErrorCode: String
    ): DTO {
        val statusCode = errorWrapper.errorCode
        val errorMsg = errorWrapper.detail

        return when (statusCode) {
            SPNewResponseStatusCode.httpUnauthorized -> throw unauthorizedError(errorWrapper)
            SPNewResponseStatusCode.INTERNAL_SERVER_ERROR -> throw SPInternalServerError(
                data = errorWrapper
            )

            SPNewResponseStatusCode.updateRequired -> throw SPUpdateRequiredError(
                data = errorWrapper
            )

            SPNewResponseStatusCode.ExistingDocumentExpiredTransfer,
            SPNewResponseStatusCode.ExistingDocumentExpiredProduct,
            SPNewResponseStatusCode.ExistingDocumentInactiveTransfer,
            SPNewResponseStatusCode.ExistingDocumentInactiveProduct,
            SPNewResponseStatusCode.ExistingDocumentExpires,
            SPNewResponseStatusCode.ExistingDocumentExpired -> handleDocumentError(
                data,
                statusCode,
                errorMsg!!
            )

            SPNewResponseStatusCode.AuthenticationCodeRequired -> throw SPStrongAuthenticationError(
                headers[AUTHENTICATION_HEADER_ID] ?: UNKNOWN_ERROR,
                data = errorWrapper
            )

            SPNewResponseStatusCode.UntrustedDeviceLivenessCheckRequired -> throw SPUntrustedDeviceLivenessError(
                headers[UNTRUSTED_AUTH_ID],
                headers[UNTRUSTED_USER_ID] ?: UNKNOWN_ERROR,
                headers[UNTRUSTED_USER_NAME] ?: UNKNOWN_ERROR,
                data = errorWrapper
            )

            SPNewResponseStatusCode.ScaCodeRequired -> throw SPSCACodeRequiredError(
                requirements = headers[SCA_REQUIREMENTS_ID],
                traceId = headers[TRACE_ID],
                data = errorWrapper
            )

            else -> throw SPUnknownError(data = errorWrapper, initialErrorCode)
        }
    }

    /**
     * @return received data and shows dialog
     */
    private suspend inline fun <F : Any> handleDocumentError(
        data: F?,
        statusCode: SPResponseBaseErrorType,
        errorMsg: String
    ): F {
        // Changing thread to show dialog,
        // Current thread is Dispatcher IO.
        withContext(Dispatchers.Main) {
            inject<SPNetworkErrorStateHandlerHandler>().value.handleGlobalAction(
                statusCode,
                errorMsg
            )
            delay(1000L)
        }
        return data ?: throw SPDocumentExpiredError()
    }

    /**
     * @returns unauthorized error with specific classification
     */
    private fun unauthorizedError(errorWrapper: SPErrorWrapper): SPNetworkErrorEntity {
        return if (isUnauthorizedUserError(errorWrapper.errorCode)) {
            SPUnauthorizedUserDetailsError(data = errorWrapper)
        } else {
            SPUnauthorizedError(data = errorWrapper)
        }
    }

    /**
     * [deserializeNewError] deserializes error body of retrofit response
     */
    private fun deserializeNewError(response: Response<*>): SPErrorWrapper? {
        if (response.errorBody() == null) return null
        return gson.fromJson(response.errorBody()!!.charStream(), SPErrorWrapper::class.java)
    }

    /**
     * [isUnauthorizedUserError] checks if unauthorized error is UserBlocked or UserPasswordChanged
     */
    private fun isUnauthorizedUserError(code: SPResponseBaseErrorType): Boolean {
        return code == SPResponseStatusCode.UserBlocked || code == SPResponseStatusCode.UserPasswordChanged
    }

    /**
     * Extension function to retrieve the status code from a Retrofit Response.
     * If the response body has a status type, it uses that; otherwise, it falls back to the HTTP status code.
     *
     * @return The corresponding SPResponseStatusCode.
     */
    private fun <DTO : Any, SERVICE_DATA : SPResponse<DTO>> Response<SERVICE_DATA>.getStatusCode() =
        SPResponseStatusCode.fromValue(body()?.status?.type ?: code().toString())

    /**
     * Retrieves the initial error code from the response.
     *
     * Returns the `status.type` from the response body if available,
     * otherwise returns the HTTP status code as a string.
     *
     * @return The error code as a string.
     */
    private fun <DTO : Any, SERVICE_DATA : SPResponse<DTO>> Response<SERVICE_DATA>.getInitialErrorCode() =
        body()?.status?.type ?: code().toString()

    /**
     * Extension function to retrieve the encoded path of the request URL from a Retrofit Response.
     *
     * @return The encoded path of the request URL as a String.
     */
    private fun <SERVICE_DATA> Response<SERVICE_DATA>.getEncodedPath(): String {
        return (raw() as okhttp3.Response).request.url.encodedPath
    }
}
