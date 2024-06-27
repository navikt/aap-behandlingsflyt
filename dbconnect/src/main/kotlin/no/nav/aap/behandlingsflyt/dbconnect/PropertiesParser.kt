package no.nav.aap.behandlingsflyt.dbconnect

import java.io.IOException
import java.io.StringReader
import java.io.StringWriter
import java.util.*


internal object PropertiesParser {

    internal fun toSql(properties: Properties?): String? {
        if (properties == null || properties.isEmpty) {
            return null
        }
        val sw = StringWriter(512)

        // custom istdf Properties.store slik at vi ikke får med default timestamp
        properties.forEach { k, v ->
            sw.append(k as String).append('=').append((v as String).lines().joinToString("")).append('\n')
        }
        return sw.toString()
    }

    internal fun fromSql(dbData: String?): Properties? {
        if (dbData != null) {
            val props = Properties()
            try {
                props.load(StringReader(dbData))
            } catch (e: IOException) {
                throw IllegalArgumentException("Kan ikke lese properties til string:$props", e)
            }
            return props
        }
        return null
    }
}