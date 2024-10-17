package com.space.core.network.helper.logger

interface SPSentryHttpLogger {
    fun captureHttpRequestData(requestData: String?)
    fun captureHttpResponseData(responseData: String?)
}

