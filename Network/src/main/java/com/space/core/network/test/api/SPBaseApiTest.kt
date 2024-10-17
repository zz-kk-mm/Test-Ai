package com.space.core.network.test.api

import androidx.annotation.VisibleForTesting
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.space.core.test.SPBaseTest
import com.space.core.test.utils.MockWebServerSetupTools.createApi
import com.space.core.test.utils.MockWebServerSetupTools.createEmptyOkHttpClient
import com.space.core.test.utils.MockWebServerSetupTools.enqueueResourceResponse
import com.space.core.test.utils.rules.MainCoroutineRule
import com.space.core.test.utils.rules.MockWebServerRule
import java.io.InputStream
import java.net.HttpURLConnection
import java.nio.charset.StandardCharsets
import junit.framework.TestCase.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import okio.buffer
import okio.source
import org.junit.Rule
import retrofit2.Response

/**
 * Base class for APIService testing
 * includes all necessary rules and functions
 * to test API Services
 */
@ExperimentalCoroutinesApi
@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
open class SPBaseApiTest<T : Any>(private val testApiClass: Class<T>) : SPBaseTest() {

    @get:Rule
    val mockServerRule = MockWebServerRule()

    @get:Rule
    val mainCoroutineRule = MainCoroutineRule(TestCoroutineDispatcher())

    private lateinit var mockedApiService: T

    override fun setUp() {
        super.setUp()
        mockedApiService = provideMockedApiService(testApiClass)
    }

    /**
     * Mocks passed API Service
     * @param apiClazz - API Service java class
     * @return mocked API Service
     */
    private fun <T> provideMockedApiService(
        apiClazz: Class<T>
    ): T {
        return createApi(
            mockServerRule.server.url("space/").toString(),
            createEmptyOkHttpClient(),
            apiClazz
        )
    }

    /**
     * Makes fake request through the MockWebServer
     * and returns a mocked Response
     * @param responseJsonPath - string path to mocked json response
     * @param responseCode - response code which will be returned by MockWebServer
     * @param operation - mocked api request function
     * @return mock response of MockWebServer
     */
    suspend fun <Q : Any> request(
        responseJsonPath: String,
        responseCode: Int = HttpURLConnection.HTTP_OK,
        operation: suspend (T) -> Response<Q>
    ): Response<Q> {
        mockServerRule.server.enqueueResourceResponse(
            responseJsonPath = responseJsonPath,
            responseCode = responseCode
        )
        return operation.invoke(mockedApiService)
    }

    /**
     * Makes assertion on success response
     * @param response - mock web server response
     */
    fun <T> testSuccessResponse(response: Response<T>) {
        assertNotNull(response)
        assertTrue(response.isSuccessful)
        assertNotNull(response.body())
    }

    /**
     * Makes assertion on error response
     * @param response - mock web server response
     */
    fun <T> testErrorResponse(response: Response<T>) {
        assertNotNull(response)
        assertFalse(response.isSuccessful)
        assertNotNull(response.errorBody())
        assertNull(response.body())
    }

    /**
     * Reads json file from assets folder
     * and assigns its value to json:String
     * @param jsonPath - string path to json file
     */
    fun readJsonFromAsset(jsonPath: String) =
        try {
            val inputStream: InputStream = javaClass.classLoader!!.getResourceAsStream(jsonPath)!!
            inputStream.source().buffer().readString(StandardCharsets.UTF_8)
        } catch (ex: Exception) {
            ex.printStackTrace()
            null
        }

    /**
     * Deserializes given Json into an object of the given type.
     * @param jsonPath - string path to json file
     */
    inline fun <reified DTO> parseJSON(jsonPath: String): DTO {
        return Gson().fromJson(readJsonFromAsset(jsonPath), object : TypeToken<DTO>() {}.type)
    }
}
