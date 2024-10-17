package com.space.core.network.service.serializers

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.space.core.network.service.errors.SPErrorWrapper
import com.space.core.network.service.response.SPResponseBaseErrorType
import java.lang.reflect.Type

/**
 * Deserializes [SPErrorWrapper] while preserving type from back-end and using [errorTypeDeserializer] to map it to enum
 */
class SPErrorWrapperDeserializer(private val errorTypeDeserializer: SPResponseErrorTypeDeserializer<*>) :
    JsonDeserializer<SPErrorWrapper>,
    SPGsonTypeAdapter {

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): SPErrorWrapper {
        val errorWrapper = gson.fromJson(json, SPErrorWrapper::class.java)
        val typeJson = json.asJsonObject?.get(TYPE)!!
        return errorWrapper.copy(
            errorCode = errorTypeDeserializer.deserialize(
                typeJson,
                typeOfT,
                context
            ) as SPResponseBaseErrorType
        )
    }

    companion object {
        private const val TYPE = "type"
        private val gson = Gson()

        internal fun GsonBuilder.registerErrorWrapperAdapter(
            errorTypeDeserializer: SPResponseErrorTypeDeserializer<*>
        ): GsonBuilder =
            registerTypeAdapter(
                SPErrorWrapper::class.java,
                SPErrorWrapperDeserializer(errorTypeDeserializer)
            )
    }
}
