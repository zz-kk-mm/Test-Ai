package com.space.core.network.service.builder

import com.google.gson.reflect.TypeToken
import com.space.core.common.koin.inject
import com.space.core.network.service.entity.SPNetworkErrorEntity
import com.space.ui.old_components.loading_dialog.core.SPLoadingManager
import java.lang.reflect.Type
import kotlinx.coroutines.CoroutineScope

/**
 * @see SPNetworkBuilder
 *
 * @param T generic of data which will be returned after successful API execution
 * @param E generic of error data which will be returned after failed API execution
 * @property loadingLambda returns boolean to handle loader turning ON/OFF
 * @property loadKoinModule returns boolean to handle load and unload of koin module
 * @property executionLambda executes request and return Generic data, which depends on Response
 * @property success lambda returns response data if network call will be successful
 * @property suspendSuccess lambda returns response data if network call will be successful, but it is a suspend function,
 * Consequently we can execute db operation there, also before its finishing network builder wont execute next blocks [finishLambda] and [loadingLambda]
 * @property errorLambda handle errors and which returns error code and message
 * @property startLambda will be executed before network request execution
 * @property finishLambda will be executed at the final stage
 */
class SPNetworkBuilder<T, E> {
    val loadingManager by inject<SPLoadingManager>()
    internal var loadingLambda: (
        (loaderVisibility: Boolean, loadingManager: SPLoadingManager) -> Unit
    )? = null
    internal var loadKoinModule: ((Boolean) -> Unit)? = null
    internal var executionLambda: (suspend CoroutineScope.() -> T)? = null
    var errorLambda: (suspend SPNetworkErrorEntity.(E) -> Unit)? = null
    var errorConnection: (suspend () -> Unit)? = null
    internal var startLambda: (suspend () -> Unit)? = null
    internal var finishLambda: (suspend () -> Unit)? = null
    internal var success: ((result: T) -> Unit)? = null
    internal var suspendSuccess: (suspend (T) -> Unit)? = null

    /**
     * Type of error data, which can be used in [SPErrorWrapper] to serialize/deserialize error data
     */
    lateinit var errorTypeToken: Type
}

fun <T, E> SPNetworkBuilder<T, E>.loading(
    loading: (visibility: Boolean, loadingManager: SPLoadingManager) -> Unit
) {
    this.loadingLambda = loading
}

fun <T, E> SPNetworkBuilder<T, E>.execute(execution: suspend CoroutineScope.() -> T) {
    this.executionLambda = execution
}

inline fun <T, reified E> SPNetworkBuilder<T, E>.error(
    noinline error: suspend SPNetworkErrorEntity.(error: E) -> Unit
) {
    this.errorTypeToken = object : TypeToken<E>() {}.type
    this.errorLambda = error
}

fun <T, E> SPNetworkBuilder<T, E>.onFinish(onFinish: suspend () -> Unit) {
    this.finishLambda = onFinish
}

fun <T, E> SPNetworkBuilder<T, E>.onStart(onStart: suspend () -> Unit) {
    this.startLambda = onStart
}

fun <T, E> SPNetworkBuilder<T, E>.onConnectionError(errorConnection: suspend () -> Unit) {
    this.errorConnection = errorConnection
}

fun <T, E> SPNetworkBuilder<T, E>.success(success: (T) -> Unit) {
    this.success = success
}

fun <T, E> SPNetworkBuilder<T, E>.suspendSuccess(success: suspend (T) -> Unit) {
    this.suspendSuccess = success
}

fun <T, E> SPNetworkBuilder<T, E>.loadKoinModule(loadKoinModule: (loadKoinModule: Boolean) -> Unit) {
    this.loadKoinModule = loadKoinModule
}
