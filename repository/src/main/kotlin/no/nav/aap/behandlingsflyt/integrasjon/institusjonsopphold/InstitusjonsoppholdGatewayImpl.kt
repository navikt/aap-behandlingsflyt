package no.nav.aap.behandlingsflyt.integrasjon.institusjonsopphold

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Institusjonsopphold
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.InstitusjonsoppholdGateway
import no.nav.aap.behandlingsflyt.prometheus
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.request.GetRequest
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.komponenter.json.DefaultJsonMapper
import org.slf4j.LoggerFactory
import java.net.URI
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.jvm.javaClass

data class InstitusjonoppholdRequest(
    val personident: String
)

data class InstitusjonoppholdEnkelt(
    val oppholdId: Long
)

/**
 * Hentet [herfra](https://github.com/navikt/institusjon/blob/d176c942e599658c887c5fe970e358b62fea1c06/apps/inst2/src/main/java/no/nav/inst2/provider/rs/api/domain/EnkeltInstitusjonsopphold.java#L65).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class InstitusjonsoppholdJSON(
    private val oppholdId: Long? = null,

    private val tssEksternId: String? = null,

    val organisasjonsnummer: String? = null,

    val institusjonstype: String? = null,

    private val varighet: String? = null,

    val kategori: String? = null,

    val startdato: LocalDate? = null,

    val faktiskSluttdato: LocalDate? = null,

    val forventetSluttdato: LocalDate? = null,

    private val kilde: String? = null,

    private val registrertAv: String? = null,

    private val overfoert: Boolean? = null,

    private val endretAv: String? = null,

    private val endringstidspunkt: LocalDateTime? = null,

    val institusjonsnavn: String? = null,

    private val avdelingsnavn: String? = null,

)

object InstitusjonsoppholdGatewayImpl : InstitusjonsoppholdGateway {
    private val log = LoggerFactory.getLogger(javaClass)

    private val personOppholdUrl =
        URI.create(requiredConfigForKey("integrasjon.institusjonsopphold.url"))
    private val enkeltOppholdURL =
        URI.create(requiredConfigForKey("integrasjon.institusjonsoppholdenkelt.url"))
    private val config = ClientConfig(scope = requiredConfigForKey("integrasjon.institusjonsopphold.scope"))
    private val client = RestClient.withDefaultResponseHandler(
        config = config,
        tokenProvider = ClientCredentialsTokenProvider,
        prometheus = prometheus
    )

    /**
     * https://inst2-q2.dev.intern.nav.no/swagger-ui/index.html#/institusjonsopphold/institusjonsopphold/soek
     */
    private fun query(request: InstitusjonoppholdRequest): List<InstitusjonsoppholdJSON> {
        val httpRequest = PostRequest(
            body = request,
            additionalHeaders = listOfNotNull(
                Header("Nav-Consumer-Id", "aap-behandlingsflyt"),
                Header("Nav-Formaal", "ARBEIDSAVKLARINGSPENGER"),
                Header("Accept", "application/json")
            )
        )

        return requireNotNull(client.post(uri = personOppholdUrl, request = httpRequest, mapper = { body, _ ->
            DefaultJsonMapper.fromJson(body)
        }))
    }

    private fun query(request: InstitusjonoppholdEnkelt): InstitusjonsoppholdJSON {
        val httpRequest = GetRequest(
            additionalHeaders = listOfNotNull(
                Header("Nav-Consumer-Id", "aap-behandlingsflyt"),
                Header("Nav-Formaal", "ARBEIDSAVKLARINGSPENGER"),
                Header("Accept", "application/json")
            )
        )

        val enkeltOppholdUrlMedOppholdId = URI.create(
            "$enkeltOppholdURL/${request.oppholdId}?Med-Institusjonsinformasjon=true"
        )

        return requireNotNull(client.get(uri = enkeltOppholdUrlMedOppholdId, request = httpRequest, mapper = { body, _ ->
            DefaultJsonMapper.fromJson(body)
        }))
    }

    override fun innhent(person: Person): List<Institusjonsopphold> {
        val request = InstitusjonoppholdRequest(person.aktivIdent().identifikator)
        val oppholdRes = query(request)
        val institusjonsopphold = oppholdRes.map { opphold ->
            log.info("Rått organisasjonsnummer: ${opphold.organisasjonsnummer}")
            Institusjonsopphold.nyttOpphold(
                institusjonstype = requireNotNull(opphold.institusjonstype) { "Institusjonstype på institusjonsopphold må være satt." },
                kategori = opphold.kategori,
                startdato = requireNotNull(opphold.startdato) { "Startdato på institusjonsopphold må være satt." },
                sluttdato = opphold.faktiskSluttdato ?: opphold.forventetSluttdato,
                orgnr = opphold.organisasjonsnummer ?: "Ukjent",
                institusjonsnavn = opphold.institusjonsnavn ?: "Ukjent institusjon"
            )
        }

        return institusjonsopphold
    }

    override fun hentDataForHendelse(oppholdId: Long): Institusjonsopphold {
        val request = InstitusjonoppholdEnkelt(oppholdId)
        val oppholdRes = query(request)
        val institusjonsopphold =

            Institusjonsopphold.nyttOpphold(
                institusjonstype = requireNotNull(oppholdRes.institusjonstype) { "Institusjonstype på institusjonsopphold må være satt." },
                kategori = oppholdRes.kategori,
                startdato = requireNotNull(oppholdRes.startdato) { "Startdato på institusjonsopphold må være satt." },
                sluttdato = oppholdRes.faktiskSluttdato ?: oppholdRes.forventetSluttdato,
                orgnr = oppholdRes.organisasjonsnummer ?: "Ukjent",
                institusjonsnavn = oppholdRes.institusjonsnavn ?: "Ukjent institusjon"
            )

        return institusjonsopphold
    }
}