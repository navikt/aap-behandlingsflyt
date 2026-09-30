package no.nav.aap.behandlingsflyt.flyt

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode
import no.nav.aap.komponenter.json.DefaultJsonMapper
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

private val uuidMønster = Regex("[0-9a-fA-F]{8}(-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}")
private val identMønster = Regex("[0-9]{11}")
private val datoMønster = Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}")
private val tidspunktMønster = Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}(\\.[0-9]{1,9})?")
private val fastDato = LocalDate.of(2025, 9, 24)

/**
 * Flyttestene gir nye UUID-er, identer, saksnummer, datoer og tidspunkt ved hver kjøring.
 * Uten normalisering endres nesten hver linje, og ekte endringer i oppførsel drukner i støy.
 *
 * Tidspunkt erstattes med kl. 12 pluss et løpenummer i millisekunder. Løpenummeret bestemmes av
 * avklaringsbehovenes `tidsstempel`, og like tidsstempler får hvert sitt nummer. Det er ikke nok å
 * rangere unike verdier, fordi det varierer mellom kjøringer om to tidspunkt havner på samme
 * mikrosekund. Andre tidspunkt plasseres relativt til tidsstemplene. Verdier fra år 9000 og
 * senere beholdes.
 */
fun normaliserStatistikkFixture(rot: JsonNode, hendelsesDato: LocalDate): JsonNode {
    val mapper = DefaultJsonMapper.objectMapper()
    val tidsstempler = mutableListOf<LocalDateTime>()

    fun finnTidspunkt(node: JsonNode) {
        when (node) {
            is ObjectNode -> node.properties().iterator().forEachRemaining { (navn, verdi) ->
                if (navn == "tidsstempel" && verdi.isTextual && tidspunktMønster.matches(verdi.asText())) {
                    tidsstempler.add(LocalDateTime.parse(verdi.asText()))
                }
                finnTidspunkt(verdi)
            }

            is ArrayNode -> node.elements().forEach(::finnTidspunkt)
        }
    }
    finnTidspunkt(rot)

    val tidsstemplerPerDag = tidsstempler.groupBy { it.toLocalDate() }.mapValues { (_, tider) -> tider.sorted() }
    val dager = fastDato.toEpochDay() - hendelsesDato.toEpochDay()
    val saksnummer = rot.path("saksnummer").asText()
    val referanser = mutableMapOf<String, String>()
    val identer = mutableMapOf<String, String>()
    val brukteTidsstempler = mutableMapOf<LocalDateTime, Int>()

    fun normaliserTekst(verdi: String, felt: String?): String = when {
        uuidMønster.matches(verdi) -> referanser.getOrPut(verdi) {
            UUID.nameUUIDFromBytes("statistikk-fixture-${referanser.size}".toByteArray(StandardCharsets.UTF_8))
                .toString()
        }

        identMønster.matches(verdi) -> identer.getOrPut(verdi) {
            (identer.size + 1).toString().padStart(11, '0')
        }

        verdi == saksnummer -> "4TEST00"
        datoMønster.matches(verdi) -> LocalDate.parse(verdi).takeIf { it.year < 9000 }
            ?.plusDays(dager)?.toString() ?: verdi

        tidspunktMønster.matches(verdi) -> LocalDateTime.parse(verdi).let { tid ->
            if (tid.year >= 9000) verdi
            else {
                val forDagen = tidsstemplerPerDag[tid.toLocalDate()].orEmpty()
                val indeks = when (felt) {
                    // Like tidsstempler får fortløpende numre i dokumentrekkefølge.
                    "tidsstempel" -> forDagen.indexOf(tid) + 1 +
                            (brukteTidsstempler[tid] ?: 0).also { brukteTidsstempler[tid] = it + 1 }
                    // Settes når hendelsen lages, alltid etter siste endring.
                    "hendelsesTidspunkt", "tidspunktSisteEndring" -> forDagen.size + 1
                    // Må komme etter mottattTid også når det ikke finnes avklaringsbehov.
                    "vedtakstidspunkt" -> if (forDagen.isEmpty()) 1 else forDagen.count { it <= tid }
                    else -> forDagen.count { it <= tid }
                }
                LocalDateTime.of(tid.toLocalDate().plusDays(dager), java.time.LocalTime.NOON)
                    .plusNanos(indeks.toLong() * 1_000_000).toString()
            }
        }

        else -> verdi
    }

    fun normaliser(node: JsonNode, felt: String? = null): JsonNode = when (node) {
        is ObjectNode -> mapper.createObjectNode().also { objekt ->
            node.properties().iterator()
                .forEachRemaining { (navn, verdi) -> objekt.set<JsonNode>(navn, normaliser(verdi, navn)) }
        }

        is ArrayNode -> mapper.createArrayNode().also { array ->
            node.forEach { array.add(normaliser(it, felt)) }
        }

        else -> if (node.isTextual) TextNode(normaliserTekst(node.asText(), felt)) else node
    }

    return normaliser(rot)
}
