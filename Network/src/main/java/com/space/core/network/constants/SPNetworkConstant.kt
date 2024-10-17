package com.space.core.network.constants

object SPNetworkConstant {

    const val TIME_OUT_VALUE: Long = 60L
    const val DEFAULT_TIME_OUT_VALUE: Long = 2 * TIME_OUT_VALUE

    const val CONNECT_TIMEOUT = "CONNECT_TIMEOUT"
    const val READ_TIMEOUT = "READ_TIMEOUT"
    const val WRITE_TIMEOUT = "WRITE_TIMEOUT"

    const val UNKNOWN_ERROR = "Unknown Error"
    internal const val AUTHENTICATION_HEADER_ID = "AuthenticatedActionId"
    internal const val UNTRUSTED_USER_NAME = "username"
    internal const val UNTRUSTED_USER_ID = "userid"
    internal const val UNTRUSTED_AUTH_ID = "authenticationcode"

    internal const val SCA_REQUIREMENTS_ID = "X-Sca-Requirements"
    internal const val TRACE_ID = "x-trace-id"
}
