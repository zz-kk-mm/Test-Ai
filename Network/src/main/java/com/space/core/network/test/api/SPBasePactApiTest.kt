package com.space.core.network.test.api

import androidx.annotation.VisibleForTesting
import au.com.dius.pact.consumer.MockServer
import au.com.dius.pact.consumer.PactTestExecutionContext
import au.com.dius.pact.consumer.dsl.PactDslRequestWithPath
import au.com.dius.pact.consumer.dsl.PactDslRequestWithoutPath
import au.com.dius.pact.consumer.dsl.PactDslResponse
import au.com.dius.pact.consumer.dsl.PactDslWithProvider
import au.com.dius.pact.consumer.dsl.PactDslWithState
import au.com.dius.pact.consumer.junit.ConsumerPactTest
import au.com.dius.pact.core.model.RequestResponsePact
import com.google.gson.Gson
import com.space.core.test.utils.runSPTest
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.random.Random
import kotlin.random.nextInt
import kotlin.random.nextLong
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.isAccessible
import net.bytebuddy.utility.RandomString
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.UUID

/**
 * Base class for Pact API testing.
 * @param T The type of API being tested.
 * @property providerName The name of the provider which you can find in PactFlow. For example Space.Service.* (where * - the name of requests category)
 * @property consumerName The name of the consumer. Better to use this format - Space.Android.* (where * - the name of requests category)
 * @property testApiClass The class representing the API being tested.
 * @property requestPrefix The prefix for API requests. By default [SPPactApiPrefix.API]
 */
@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
abstract class SPBasePactApiTest<T : Any>(
    private val providerName: String,
    private val consumerName: String,
    private val testApiClass: Class<T>,
    private val requestPrefix: SPPactApiPrefix = SPPactApiPrefix.API
) : ConsumerPactTest() {

    /**
     * Instance of [Gson] to be use in [getApiModels] function
     */
    protected val gsonInstance by lazy { Gson() }

    /**
    Default headers. You can override this field and use another headers if you need.
     */
    open val defaultHeaders = mapOf(
        AUTHORIZATION_HEADER to AUTHORIZATION_HEADER,
        TENANT_ID_HEADER to TENANT_ID_HEADER
    )

    /**
     * Provide ApiModel/s that will be tested in this test
     */
    abstract fun getApiModels(): List<SPTestApiModel>

    /**
     * Abstract function to test the API
     * @param api The API instance to test
     */
    abstract suspend fun testApi(api: T)

    /**
     * Create a Pact for the provider
     * @param builder The Pact DSL builder
     * @return The created [RequestResponsePact]
     */
    override fun createPact(builder: PactDslWithProvider): RequestResponsePact {
        return buildPact(builder)
    }

    /**
     * Get the provider name
     * @return The provider name
     */
    override fun providerName() = providerName

    /**
     * Get the consumer name
     * @return The consumer name
     */
    override fun consumerName() = consumerName

    /**
     * Run the test with the mock server
     * @param mockServer The mock server
     * @param context The test execution context
     */
    override fun runTest(mockServer: MockServer?, context: PactTestExecutionContext?) {
        val retrofit = Retrofit.Builder()
            .baseUrl(mockServer!!.getUrl() + requestPrefix.value + "/")
            .callFactory(PactCallFactory(requestPrefix, getApiModels(), defaultHeaders))
            .addConverterFactory(GsonConverterFactory.create(Gson()))
            .build()

        runSPTest {
            testApi(retrofit.create(testApiClass))
        }
    }

    /**
     * Builds a Pact based on the provided Pact DSL builder and API models
     * @param builder The Pact DSL builder
     * @return The created RequestResponsePact
     */
    private fun buildPact(builder: PactDslWithProvider): RequestResponsePact {
        var response: PactDslResponse? = null

        // Iterate over API models and build the Pact
        getApiModels().forEach {
            response = if (response == null) {
                builderFlow(builder, it)
            } else {
                responseFlow(response!!, it)
            }
        }
        return response!!.toPact()
    }

    /**
     * Builds the Pact request/response flow for a given API model
     * @param builder The Pact DSL builder
     * @param apiModel The API model to build the Pact for
     * @return The Pact DSL response
     */
    private fun builderFlow(
        builder: PactDslWithProvider,
        apiModel: SPTestApiModel
    ) = createPactResponse(builder.given(apiModel.state), apiModel)

    /**
     * Builds the Pact response flow for a given response and API model
     * @param response The Pact DSL response
     * @param apiModel The API model to build the Pact for
     * @return The Pact DSL response
     */
    private fun responseFlow(
        response: PactDslResponse,
        apiModel: SPTestApiModel
    ) = createPactResponse(response.given(apiModel.state), apiModel)

    /**
     * Creates a Pact response based on the given state and API model
     * @param state The Pact DSL state
     * @param apiModel The API model to build the Pact for
     * @return The Pact DSL response
     */
    private fun createPactResponse(
        state: PactDslWithState,
        apiModel: SPTestApiModel
    ): PactDslResponse {
        return state
            .uponReceiving(apiModel.description)
            // Request part
            .setupPath(apiModel.request.path)
            .method(apiModel.request.type.toString())
            .setupRequestParams(apiModel.request)
            .willRespondWith()
            // Response part
            .status(apiModel.response.status)
            .apply { apiModel.response.json?.let { body(it) } }
    }

    /**
     * Sets up the path for a Pact request without path
     * @param pathType The type of path (default or with parameters)
     * @return The Pact DSL request with path
     */
    private fun PactDslRequestWithoutPath.setupPath(pathType: SPPathType): PactDslRequestWithPath {
        return when (pathType) {
            is SPPathType.DefaultPath -> {
                path(requestPrefix.value + pathType.path)
            }

            is SPPathType.PathWithParams -> {
                pathFromProviderState(
                    requestPrefix.value + pathType.expression,
                    requestPrefix.value + pathType.example
                )
            }
        }
    }

    /**
     * Sets up request parameters for a Pact request with path
     * @param request The API request model
     * @return The Pact DSL request with path
     */
    private fun PactDslRequestWithPath.setupRequestParams(
        request: SPTestApiModelRequest
    ): PactDslRequestWithPath {
        with(request) {
            headers(defaultHeaders + (headers ?: mapOf()))
            body?.let { body(it) }
            queries?.forEach { query -> matchQuery(query.key, query.value) }
        }
        return this
    }

    /**
     * Custom Call Factory for making HTTP calls with Pact
     * @property requestPrefix The prefix uses to build request
     * @property apiModels The list of API models
     * @property defaultHeaders The default headers
     */
    private class PactCallFactory(
        private val requestPrefix: SPPactApiPrefix,
        private val apiModels: List<SPTestApiModel>,
        private val defaultHeaders: Map<String, String>
    ) : okhttp3.Call.Factory {

        private val client = OkHttpClient()

        /**
         * Creates a new HTTP call based on the given request
         * @param request The HTTP request
         * @return The HTTP call
         */
        override fun newCall(request: Request): okhttp3.Call {
            val requestBuilder = request.newBuilder()

            // Iterate over API models and add headers if request matches
            apiModels.forEach { apiModel ->
                if (isTheSameRequest(requestPrefix, request, apiModel.request)) {
                    (defaultHeaders + (apiModel.request.headers ?: mapOf())).forEach { header ->
                        requestBuilder.addHeader(header.key, header.value)
                    }
                }
            }

            return client.newCall(requestBuilder.build())
        }

        /**
         * Checks if the provided request matches the API model's request
         * @param requestPrefix The prefix uses to build request
         * @param callRequest The HTTP request being made
         * @param apiRequest The API request model
         * @return `true` if the requests match, `false` otherwise
         */
        private fun isTheSameRequest(
            requestPrefix: SPPactApiPrefix,
            callRequest: Request,
            apiRequest: SPTestApiModelRequest
        ): Boolean {
            return callRequest.url.toUrl().path == (
                requestPrefix.value + apiRequest.path.toString()
                ) && callRequest.method == apiRequest.type.toString()
        }
    }

    /**
     * Creates a new instance of the given class using reflection.
     * This function uses the primary constructor of the class to create the instance.
     * It first makes all member properties accessible, then generates mock values for each property, and finally calls the primary constructor with the generated mock values as arguments.
     * Params: clazz - The class to create an instance of.
     * Returns: A new instance of the class.
     */
    protected fun <T : Any> newInstance(clazz: KClass<T>): T {
        // Try to get the primary constructor or any available constructor
        val constructor = clazz.primaryConstructor ?: clazz.constructors.firstOrNull()
        ?: throw IllegalArgumentException("Class must have at least one constructor")

        constructor.isAccessible = true // Make the constructor accessible, even if it's private

        // Prepare arguments for the constructor
        val arguments = constructor.parameters.associateWith { parameter ->
            val propertyName = parameter.name
                ?: throw IllegalArgumentException("Parameter name is not available for ${clazz.simpleName}")

            // Try to find a matching property in the class
            val property = clazz.memberProperties.find { it.name == propertyName }

            val value = if (property != null) {
                property.isAccessible = true // Make private properties accessible
                generateMockValueForType(property.name, property.returnType)
            } else {
                // If no matching property, generate a mock value based on the parameter type
                generateMockValueForType(propertyName, parameter.type)
            }

            // Handle non-nullable parameters
            if (!parameter.type.isMarkedNullable && value == null) {
                throw IllegalArgumentException("Cannot generate a non-nullable mock value for parameter '${parameter.name}'")
            }

            value
        }

        return constructor.callBy(arguments)
    }

    /**
     * Generates a mock value for the given type.
     * This function supports various types, including primitive types, enum classes, lists, arrays, and other classes.
     * For primitive types, it generates random values within a reasonable range.
     * For enum classes, it generates a random enum value.
     * For lists and arrays, it generates a list or array of mock values for the element type.
     * For other classes, it recursively calls the newInstance() function to create a new instance.
     * Params: name - The name of property.
     * Params: type - The type to generate a mock value for.
     * Returns: A mock value for the given type.
     */
    private fun generateMockValueForType(name: String, type: KType): Any? {
        if (type.isMarkedNullable) return null

        return when (val classifier = type.classifier) {
            String::class -> {
                if (name.contains("id", true)) {
                    UUID.randomUUID().toString()
                } else {
                    RandomString.make(8)
                }
            }
            Int::class -> Random.nextInt(0..1000)
            Long::class -> Random.nextLong(0L..1000L)
            Float::class -> Random.nextFloat()
            Double::class -> Random.nextDouble(1000.0)
            Boolean::class -> Random.nextBoolean()
            BigInteger::class -> BigInteger(Random.nextBytes(1000))
            BigDecimal::class -> BigDecimal(Random.nextDouble(1000.0))
            Short::class -> Random.nextInt(0..1000).toShort()
            Byte::class -> Random.nextBytes(1000)
            List::class -> listOf(generateMockValueForType(name, type.arguments[0].type!!))
            Array::class -> arrayOf(generateMockValueForType(name, type.arguments[0].type!!))
            is KClass<*> -> {
                if (classifier.isSubclassOf(Enum::class)) {
                    // Generate a random enum value
                    classifier.java.enumConstants.random()
                } else {
                    newInstance(classifier)
                }
            }

            else -> {
                throw IllegalArgumentException("we don't know about your type")
            }
        }
    }

    // Constants and configurations for the Pact API testing
    companion object {
        private const val AUTHORIZATION_HEADER = "Authorization"
        private const val TENANT_ID_HEADER = "TenantID"
    }
}
