package no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

public class FlexibleLocalDateDeserializer : StdDeserializer<LocalDate>(LocalDate::class.java) {

    private val formatters = listOf(
        DateTimeFormatter.ISO_LOCAL_DATE,
        DateTimeFormatter.ofPattern("dd.MM.yyyy"),
    )

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): LocalDate {
        val value = p.text.trim()

        if (value.isEmpty()) {
            return ctxt.handleUnexpectedToken(LocalDate::class.java, p) as LocalDate
        }

        for (formatter in formatters) {
            try {
                return LocalDate.parse(value, formatter)
            } catch (_: DateTimeParseException) {
            }
        }

        throw ctxt.weirdStringException(
            value,
            LocalDate::class.java,
            "Expected date format yyyy-MM-dd or dd.MM.yyyy"
        )
    }

}