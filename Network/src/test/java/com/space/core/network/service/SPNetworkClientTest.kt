package com.space.core.network.service

import com.space.core.common.server_config.core.SPServerConfigApi
import com.space.core.network.helper.retrofit.SPRetrofitHelperImpl
import com.space.core.network.service.entity.SPNetworkErrorEntity
import com.space.core.network.service.errors.throwable_handler.SPNetworkExceptionHandler
import com.space.core.network.service.response.SPResponseBaseErrorType
import com.space.core.network.service.serializers.SPGsonDefaultTypeAdapters
import com.space.core.test.SPBaseKoinTest
import com.space.core.test.utils.isNotTheSame
import com.space.core.test.utils.isTheSame
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.verify
import io.mockk.verifySequence
import kotlinx.coroutines.ExperimentalCoroutinesApi
import okhttp3.OkHttpClient
import org.junit.Assert
import org.junit.Test
import org.koin.core.module.Module
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory

@OptIn(ExperimentalCoroutinesApi::class)
class SPNetworkClientTest : SPBaseKoinTest() {

    @MockK
    private lateinit var okHttpClient: OkHttpClient

    @MockK
    private lateinit var exceptionHandler: SPNetworkExceptionHandler

    @MockK
    private lateinit var serverConfig: SPServerConfigApi
    private var defaultTypeAdapters: SPGsonDefaultTypeAdapters = mockk()

    private lateinit var client: Client
    private val modernizedBaseUrl = "https://space.ge"

    override fun setUp() {
        super.setUp()
        every { serverConfig.generalConfig.modernizedBaseUrl } returns modernizedBaseUrl
        every { defaultTypeAdapters.get() } returns listOf()
        client = Client(okHttpClient, exceptionHandler, serverConfig, defaultTypeAdapters)
    }

    @Test
    fun `retrofit is initialized correctly`() {
        val gsonConverterFactory: GsonConverterFactory = mockk()
        val scalarsConverterFactory: ScalarsConverterFactory = mockk()

        mockkStatic(ScalarsConverterFactory::class)
        every { ScalarsConverterFactory.create() } returns scalarsConverterFactory

        mockkStatic(GsonConverterFactory::class)
        every { GsonConverterFactory.create(client.gson) } returns gsonConverterFactory

        client.retrofit.baseUrl().toString() isTheSame client.modernizedBaseUrl()
        client.retrofit.converterFactories().contains(scalarsConverterFactory) isTheSame true
        client.retrofit.converterFactories().contains(gsonConverterFactory) isTheSame true

        verifySequence {
            serverConfig.generalConfig.modernizedBaseUrl
            ScalarsConverterFactory.create()
            GsonConverterFactory.create(client.gson)
           serverConfig.generalConfig.modernizedBaseUrl
        }
    }

    @Test
    fun `injectNetwork works correctly`() {
        val moduleMock: Module = mockk()
        val callbackMock: SPNetworkClient<TestErrorEnum>.(Module) -> Unit = mockk()

        justRun { callbackMock.invoke(client, moduleMock) }
        client.injectNetwork(moduleMock, callbackMock)

        verify { callbackMock.invoke(client, moduleMock) }
    }

    @Test
    fun `getModernizedBaseUrl() throws RuntimeException when path contains versioning`() {
        Assert.assertThrows(
            "Path should not contain version information." +
                " Move versioning in ServiceApi directly.",
            RuntimeException::class.java
        ) {
            client = Client(
                okHttpClient,
                exceptionHandler,
                serverConfig,
                defaultTypeAdapters,
                "test/path/v1/"
            )
        }
    }

    @Test
    fun `getRetrofitHelper works correctly when exceptionHelper is null`() {
        client.getRetrofitHelper(null) isTheSame client.retrofitHelper
    }

    @Test
    fun `getRetrofitHelper works correctly when exceptionHelper is not null`() {
        val exceptionHelper: SPNetworkExceptionHelper = mockk()
        client.getRetrofitHelper(exceptionHelper) isNotTheSame client.retrofitHelper
    }

    class Client(
        okHttpClient: OkHttpClient,
        exceptionHandler: SPNetworkExceptionHandler,
        serverConfig: SPServerConfigApi,
        defaultTypeAdapters: SPGsonDefaultTypeAdapters,
        private val path: String = "/test/path/"
    ) : SPNetworkClient<TestErrorEnum>(okHttpClient, exceptionHandler, serverConfig, defaultTypeAdapters) {

        fun modernizedBaseUrl() = getModernizedBaseUrl(path)

        override val baseUrl: String = getModernizedBaseUrl(path)

        override fun getTypeEnumClass() = TestErrorEnum::class.java

        override fun provideExceptionHelper() = TestExceptionHelper()
    }

    class TestExceptionHelper : SPNetworkExceptionHelper {
        override fun handleThrowable(throwable: Throwable): SPNetworkErrorEntity? {
            return null
        }
    }

    enum class TestErrorEnum(override val value: String) : SPResponseBaseErrorType {
        Test("test")
    }
}
