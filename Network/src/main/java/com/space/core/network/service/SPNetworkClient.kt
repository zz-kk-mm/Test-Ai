package com.space.core.network.service

import androidx.annotation.VisibleForTesting
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.space.core.common.server_config.core.SPServerConfigApi
import com.space.core.network.ext.registerDefaultTypeAdapters
import com.space.core.network.helper.retrofit.SPRetrofitHelper
import com.space.core.network.helper.retrofit.SPRetrofitHelperImpl
import com.space.core.network.service.errors.throwable_handler.SPNetworkExceptionHandler
import com.space.core.network.service.response.SPResponseBaseErrorType
import com.space.core.network.service.result_handler.SPServiceResultHandler
import com.space.core.network.service.result_handler.SPServiceResultHandlerImpl
import com.space.core.network.service.serializers.SPErrorWrapperDeserializer.Companion.registerErrorWrapperAdapter
import com.space.core.network.service.serializers.SPGsonDefaultTypeAdapters
import com.space.core.network.service.serializers.SPResponseErrorTypeDeserializer
import okhttp3.OkHttpClient
import org.koin.core.module.Module
import org.koin.core.qualifier.StringQualifier
import org.koin.core.scope.Scope
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory

abstract class SPNetworkClient<T>(
    okHttpClient: OkHttpClient,
    private val exceptionHandler: SPNetworkExceptionHandler,
    private val serverConfigApi: SPServerConfigApi,
    private val defaultTypeAdapters: SPGsonDefaultTypeAdapters
)
    where T : SPResponseBaseErrorType, T : Enum<T> {

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal val gson: Gson by lazy { getGson() }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal val resultHandler: SPServiceResultHandler by lazy { SPServiceResultHandlerImpl(gson) }

    @PublishedApi
    internal val retrofit: Retrofit by lazy { getRetrofitClient(gson, baseUrl, okHttpClient) }

    @PublishedApi
    internal val retrofitHelper: SPRetrofitHelper by lazy {
        SPRetrofitHelperImpl(
            resultHandler = resultHandler,
            networkExceptionHandler = exceptionHandler,
            networkExceptionHelper = provideExceptionHelper()
        )
    }

    /**
     * Base url for API services
     */
    abstract val baseUrl: String

    /**
     * Build [Retrofit] client with [gson] and [baseUrl]
     *
     * @param gson
     */
    private fun getRetrofitClient(gson: Gson, baseUrl: String, okHttpClient: OkHttpClient) =
        Retrofit.Builder()
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(GsonConverterFactory.create(gson))
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .build()

    /**
     * Build [Gson] with [SPResponseErrorTypeDeserializer] of [T] to convert error type into local class
     */
    private fun getGson(): Gson {
        val errorTypeDeserializer = SPResponseErrorTypeDeserializer(getTypeEnumClass())
        return GsonBuilder()
            .registerDefaultTypeAdapters(defaultTypeAdapters)
            .registerErrorWrapperAdapter(errorTypeDeserializer)
            .setLenient()
            .create()
    }

    /**
     * Get [Class] of [T] for error type
     *
     * @return enum [Class] of [T]
     */
    abstract fun getTypeEnumClass(): Class<T>

    /**
     * Provides [SPNetworkExceptionHelper] to delegate exception handling locally
     * If nothing is passed, it will delegate exception handling to global handler
     *
     * @return [SPNetworkExceptionHelper] or null
     */
    abstract fun provideExceptionHelper(): SPNetworkExceptionHelper?

    /**
     * Inject network specific instances into [module]
     *
     * We create [Gson], [Retrofit], [SPRetrofitHelper] and [SPServiceResultHandler] for local use
     * [SPNetworkExceptionHelper] is created as well if it's provided via [provideExceptionHelper]
     *
     * @param module module in which network specific instances are instantiated
     * @param onProvideModule
     */
    fun injectNetwork(
        module: Module,
        onProvideModule: SPNetworkClient<T>.(Module) -> Unit
    ) {
        onProvideModule.invoke(this@SPNetworkClient, module)
    }

    /**
     * Create Service API of [T] type under [featureQualifier] with [Retrofit] which is created by [injectNetwork]
     *
     * @param T type of Service API
     * @param featureQualifier scope of module
     */
    inline fun <reified T> Module.createServiceAPI(featureQualifier: StringQualifier? = null) =
        single(featureQualifier) { retrofit.create(T::class.java) }

    /**
     * Get Service API of [T] type with [Retrofit] which is created by [injectNetwork]
     *
     * @param T type of Service API
     */
    inline fun <reified T> getServiceAPI(): T = retrofit.create(T::class.java)

    /**
     * Create Data Source of [T] type under [featureQualifier] with [SPRetrofitHelper] which is created by [injectNetwork]
     *
     * @param T type of Data Source
     * @param featureQualifier scope of module
     * @param onCreate callback delegation to create data source with [SPRetrofitHelper] passed
     */
    inline fun <reified T> Module.createDataSource(
        featureQualifier: StringQualifier? = null,
        crossinline onCreate: Scope.(SPRetrofitHelper) -> T
    ) = single(featureQualifier) {
        onCreate.invoke(
            this,
            getRetrofitHelper(exceptionHelper = null)
        )
    }

    /**
     * Create Data Source of [T] type under [featureQualifier] with [SPRetrofitHelper] which is created by [injectNetwork]
     *
     * @param T type of Data Source
     * @param featureQualifier scope of module
     * @param exceptionHelperQualifier custom exception helper qualifier for this data source
     * @param onCreate callback delegation to create data source with [SPRetrofitHelper] passed
     */
    inline fun <reified T> Module.createDataSourceWithExceptionHelper(
        featureQualifier: StringQualifier? = null,
        exceptionHelperQualifier: StringQualifier,
        crossinline onCreate: Scope.(SPRetrofitHelper) -> T
    ) = single(featureQualifier) {
        onCreate.invoke(
            this,
            getRetrofitHelper(get<SPNetworkExceptionHelper>(exceptionHelperQualifier))
        )
    }

    /**
     * Get Data Source of [T] type with [SPRetrofitHelper] which is created by [injectNetwork]
     *
     * @param T type of Data Source
     * @param onCreate callback delegation to create data source with [SPRetrofitHelper] passed
     */
    inline fun <reified T> Scope.getDataSource(
        crossinline onCreate: Scope.(SPRetrofitHelper) -> T
    ) = onCreate.invoke(this, retrofitHelper)

    /**
     * Provides [SPRetrofitHelper] with [exceptionHelper] or default [retrofitHelper]
     *
     * @param exceptionHelper provided custom exception helper
     * @return [SPRetrofitHelper]
     */
    @PublishedApi
    internal fun getRetrofitHelper(exceptionHelper: SPNetworkExceptionHelper?): SPRetrofitHelper {
        return if (exceptionHelper == null) {
            retrofitHelper
        } else {
            SPRetrofitHelperImpl(
                resultHandler = resultHandler,
                networkExceptionHandler = exceptionHandler,
                networkExceptionHelper = exceptionHelper
            )
        }
    }

    /**
     * Create Network exception helper of [T] type under [qualifier] which is created by [injectNetwork]
     *
     * @param T type of Network exception helper
     * @param qualifier qualifier
     */
    inline fun <reified T : SPNetworkExceptionHelper> Module.createExceptionHelper(
        qualifier: StringQualifier,
        crossinline onCreate: Scope.() -> T
    ) = single<SPNetworkExceptionHelper>(qualifier) { onCreate.invoke(this) }

    /**
     * Generates modernized base url based on the [path]
     *
     * @param path api path. example: credit_card/v2
     */
    protected fun getModernizedBaseUrl(path: String): String {
        if (path.contains(VERSION_REGEX)) {
            throw RuntimeException(
                "Path should not contain version information." +
                    " Move versioning in ServiceApi directly."
            )
        }
        return serverConfigApi.generalConfig.modernizedBaseUrl + path
    }

    companion object {
        private const val VERSION_REGEX_PATTERN = "\\/v\\d+\\/"
        private val VERSION_REGEX = Regex(VERSION_REGEX_PATTERN)
    }
}
