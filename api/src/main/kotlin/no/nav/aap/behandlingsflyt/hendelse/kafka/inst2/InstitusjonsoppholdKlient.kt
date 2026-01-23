package no.nav.aap.behandlingsflyt.hendelse.kafka.inst2

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpMessageBuilder
import io.ktor.http.contentType
import java.time.LocalDate
import java.time.LocalDateTime

class InstitusjonsoppholdKlient(
    private val httpKlient: HttpClient,
    private val url: String,
) {
    suspend fun hentDataForHendelse(oppholdId: Long) =
        retry<Institusjonsopphold> {
            httpKlient
                .get("$url/api/v1/person/institusjonsopphold/$oppholdId?Med-Institusjonsinformasjon=true") {
                    contentType(ContentType.Application.Json)
                    navConsumerId("aap-behandlingsflyt-institusjonsopphold")
                    header("Nav-Formaal", "AAP-YTELSER")
                }.body()
        }.let {
            when (it) {
                is RetryResult.Success -> it.content
                is RetryResult.Failure -> {
                    throw RuntimeException(
                        "Feil oppsto ved henting av institusjonsopphold (id=$oppholdId)",
                        it.samlaExceptions(),
                    )
                }
            }
        }
}

fun HttpMessageBuilder.navConsumerId(applicationName: String) = header(Headers.NAV_CONSUMER_ID, applicationName)

data class Institusjonsopphold(
    val oppholdId: Long,
    val tssEksternId: String,
    val institusjonsnavn: String? = null,
    val avdelingsnavn: String? = null,
    val organisasjonsnummer: String? = null,
    val institusjonstype: String? = null,
    val varighet: String? = null,
    val kategori: String? = null,
    val startdato: LocalDate,
    val faktiskSluttdato: LocalDate? = null,
    val forventetSluttdato: LocalDate? = null,
    val kilde: String? = null,
    val overfoert: Boolean? = null,
    val registrertAv: String? = null,
    val endretAv: String? = null,
    val endringstidspunkt: LocalDateTime? = null,
)
