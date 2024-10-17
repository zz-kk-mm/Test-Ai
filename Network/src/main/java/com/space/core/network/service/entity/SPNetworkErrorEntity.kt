package com.space.core.network.service.entity

import com.space.core.network.service.errors.SPErrorWrapper
import com.space.core.network.service.response.SPResponseStatusCode.UnknownError

abstract class SPNetworkErrorEntity(val data: SPErrorWrapper) : Throwable(data.errorMessage) {
    val errorMessage get() = data.errorMessage
    val errorCode get() = data.errorCode
}

class SPDocumentExpiredError(data: SPErrorWrapper = SPErrorWrapper.empty()) : SPNetworkErrorEntity(
    data
)
class SPUpdateRequiredError(data: SPErrorWrapper) : SPNetworkErrorEntity(data)
class SPUnauthorizedError(data: SPErrorWrapper) : SPNetworkErrorEntity(data)
class SPUnauthorizedUserDetailsError(data: SPErrorWrapper) : SPNetworkErrorEntity(data)
class SPInternalServerError(data: SPErrorWrapper = SPErrorWrapper.empty()) :
    SPNetworkErrorEntity(data)

class SPServerTimeoutError(data: SPErrorWrapper = SPErrorWrapper.empty()) :
    SPNetworkErrorEntity(data)

class SPNoConnectionError(data: SPErrorWrapper = SPErrorWrapper.empty()) :
    SPNetworkErrorEntity(data)

class SPStrongAuthenticationError(val authId: String? = null, data: SPErrorWrapper) :
    SPNetworkErrorEntity(data)

class SPUntrustedDeviceLivenessError(
    val authId: String? = null,
    val userId: String? = null,
    val userName: String? = null,
    data: SPErrorWrapper
) : SPNetworkErrorEntity(data)

class SPJobCancellationError(data: SPErrorWrapper = SPErrorWrapper.empty()) :
    SPNetworkErrorEntity(data)

/**
 * [SPUnknownError] Represents an unknown error entity, indicating that the error code is not registered on the front end [SPResponseStatusCode].
 *
 * @param initialErrorCode The initial error code from the response before it is cast to an unknown error.
 * @param initialExceptionName The initial exception name before it was mapped to [SPUnknownError]
 */
class SPUnknownError(
    data: SPErrorWrapper = SPErrorWrapper.empty(),
    val initialErrorCode: String = UnknownError.value,
    val initialExceptionName: String = SPUnknownError::class.java.simpleName
) : SPNetworkErrorEntity(data)

class SPSCACodeRequiredError(val requirements: String?, val traceId: String?, data: SPErrorWrapper) :
    SPNetworkErrorEntity(data)
