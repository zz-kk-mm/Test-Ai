package com.space.core.network.ext

import com.google.gson.GsonBuilder
import com.space.core.network.service.serializers.SPGsonDefaultTypeAdapters

fun GsonBuilder.registerDefaultTypeAdapters(
    defaultTypeSerializers: SPGsonDefaultTypeAdapters
): GsonBuilder {
    defaultTypeSerializers.get().forEach { registerTypeAdapter(it.type, it.adapter) }
    return this
}
