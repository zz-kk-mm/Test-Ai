package com.space.core.network.service.interceptor.authentication

import okhttp3.Interceptor

/**
 * [SPAuthInterceptor] is responsible to initialize headers before each service
 */
interface SPAuthInterceptor : Interceptor
