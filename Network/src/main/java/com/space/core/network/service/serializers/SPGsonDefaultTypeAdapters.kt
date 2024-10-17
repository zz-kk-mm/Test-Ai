package com.space.core.network.service.serializers

import java.lang.reflect.Type

interface SPGsonDefaultTypeAdapters {

    /**
     * Returns list of default type adapters for [GsonBuilder]
     *
     * @return list of default type adapters
     */
    fun get(): List<Adapter>

    data class Adapter(val type: Type, val adapter: SPGsonTypeAdapter)
}
