package com.space.core.network.test.api

import androidx.annotation.VisibleForTesting

/**
 * Represents a model for testing API interactions
 * @property state The state of the API model. It can be name of the request
 * @property description The description of the API interaction
 * @property request The request model.
 * @property response The response model (default is an empty response).
 */
@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
data class SPTestApiModel(
    val state: String,
    val description: String,
    val request: SPTestApiModelRequest,
    val response: SPTestApiModelResponse = SPTestApiModelResponse()
)

/**
 * Represents a request model for testing API interactions
 * @property path The path for the request
 * @property type The request type (GET, POST, etc.)
 * @property headers The headers for the request.
 * @property queries The query parameters for the request
 * @property body The request body
 */
@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
data class SPTestApiModelRequest(
    val path: SPPathType,
    val type: SPRequestType,
    val headers: Map<String, String>? = null,
    val queries: Map<String, String>? = null,
    val body: String? = null
)

/**
 * Represents a response model for testing API interactions
 * @property json The JSON response body
 * @property status The HTTP status code (default is 200)
 */
@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
data class SPTestApiModelResponse(
    val json: String? = null,
    val status: Int = 200
)

/**
 * Enum representing HTTP request types
 */
@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
enum class SPRequestType {
    GET,
    PUT,
    POST,
    PATCH,
    DELETE
}

/**
 * Sealed class representing different types of API paths
 */
@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
sealed class SPPathType {

    /**
     * Represents a default path without parameters
     * @property path The path string
     */
    class DefaultPath(val path: String) : SPPathType() {
        override fun toString() = path
    }

    /**
     * Represents a path with parameters
     * @property expression The expression for parameterized path
     * @property example An example of the parameterized path
     */
    class PathWithParams(val expression: String, val example: String) : SPPathType() {
        override fun toString() = example
    }
}
