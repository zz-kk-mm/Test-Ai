package com.space.core.network.service.interceptor.logging

import com.space.core.network.service.interceptor.authentication.SPAuthInterceptor
import okhttp3.Interceptor

/**
 * [SPAuthInterceptor] is responsible to log service request/response during each service
 */
interface SPLoggingInterceptor : Interceptor
