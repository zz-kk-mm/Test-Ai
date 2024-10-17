package com.space.core.network.test.api

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class ModernApi(
    val providerName: String,
    val consumerName: String,
    val requestPrefix: SPPactApiPrefix = SPPactApiPrefix.API
)
