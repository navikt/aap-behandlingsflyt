package no.nav.aap.json

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.IOException
import java.io.InputStream
import java.util.*


object DefaultJsonMapper {

    private val mapper = ObjectMapper()
        .registerModule(KotlinModule.Builder().build())
        .registerModule(JavaTimeModule())
        .setTimeZone(TimeZone.getTimeZone("Europe/Oslo"))
        .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .disable(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS)
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE)

    fun toJson(value: Any): String {
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(value)
        } catch (e: IOException) {
            throw SerializationException(e)
        }
    }

    fun <T> fromJson(value: String, toClass: Class<T>): T {
        try {
            return mapper.readValue(value, toClass)
        } catch (e: IOException) {
            throw DeserializationException(e)
        }
    }

   inline fun <reified T> fromJson(value: String): T {
        try {
            return objectMapper().readValue<T>(value)
        } catch (e: IOException) {
            throw DeserializationException(e)
        }
    }

    inline fun <reified T> streamFromJson(value: InputStream): T {
        try {
            return objectMapper().readValue<T>(value)
        } catch (e: IOException) {
            throw DeserializationException(e)
        }
    }

    fun objectMapper(): ObjectMapper {
        return mapper
    }
}
