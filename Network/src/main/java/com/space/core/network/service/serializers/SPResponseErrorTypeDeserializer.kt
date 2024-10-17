package com.space.core.network.service.serializers

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.space.core.network.service.response.SPNewResponseStatusCode
import com.space.core.network.service.response.SPResponseBaseErrorType
import java.lang.reflect.Type

/**
 * [JsonDeserializer] of [SPResponseBaseErrorType] for [T] generic type.
 *
 * Used to deserialize error type locally, if not found in provided local [enumClass],
 * then it tries to find it in general [SPNewResponseStatusCode]
 *
 * @param T type of local [enumClass]
 * @property enumClass [Class] of local error type
 */
class SPResponseErrorTypeDeserializer<T>(private val enumClass: Class<T>) :
    JsonDeserializer<T> where T : Enum<T>, T : SPResponseBaseErrorType {

    private var values: Map<Int, T>? = null

    override fun deserialize(
        json: JsonElement,
        type: Type,
        context: JsonDeserializationContext
    ): T? {
        return (
            values ?: enumClass.enumConstants?.associateBy { it.value }
                .also { it }
            )?.get(json.asString)
            ?: SPNewResponseStatusCode.fromValue(json.asString) as? T
    }
}
