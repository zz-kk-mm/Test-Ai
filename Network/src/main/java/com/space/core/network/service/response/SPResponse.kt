package com.space.core.network.service.response

/**
 * [SPResponse] is used for Api service responses
 *
 * @param status contains status code and message
 * @param data is a response data
 */
data class SPResponse<DATA>(
    val status: SPStatus,
    val data: DATA?
)

/**
 * [SPStatus] is used in [SPResponse], it contains
 * @param type type of status code
 * @param message backend status message
 */
data class SPStatus(
    val message: String,
    val type: String
)

/**
 * [SPGeneralResponse] is used by services which haven't response
 */
class SPGeneralResponse
