package com.space.core.network.service.errors

import com.space.core.network.service.entity.SPNetworkErrorEntity
import com.space.core.network.service.response.SPResponseStatusCode

class SPFakeNetworkError(data: SPErrorWrapper = SPErrorWrapper.empty(message = ERROR_MESSAGE)) :
    SPNetworkErrorEntity(data) {
    constructor(message: String) : this(SPErrorWrapper.empty(message))
    constructor(errorCode: SPResponseStatusCode) : this(
        SPErrorWrapper.empty(ERROR_MESSAGE, errorCode)
    )
    constructor(message: String, errorCode: SPResponseStatusCode) : this(
        SPErrorWrapper.empty(message, errorCode)
    )

    companion object {
        const val ERROR_MESSAGE = "error_message"
    }
}
