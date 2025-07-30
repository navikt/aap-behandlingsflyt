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
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.komponenter.json.DefaultJsonMapper
import java.net.URI
import java.time.LocalDate
import java.time.LocalDateTime

data class InstitusjonoppholdRequest(
    val foedselsnumre: String
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
    private val url =
        URI.create(requiredConfigForKey("integrasjon.institusjonsopphold.url") + "?Med-Institusjonsinformasjon=true")
    private val config = ClientConfig(scope = requiredConfigForKey("integrasjon.institusjonsopphold.scope"))
    private val client = RestClient.withDefaultResponseHandler(
        config = config,
        tokenProvider = ClientCredentialsTokenProvider,
        prometheus = prometheus
    )

    /**
     * https://inst2-q2.dev.intern.nav.no/swagger-ui/index.html#/institusjonsopphold/institusjonsopphold
     */
    private fun query(request: InstitusjonoppholdRequest): List<InstitusjonsoppholdJSON> {
        val httpRequest = GetRequest(
            additionalHeaders = listOf(
                Header("Nav-Personident", request.foedselsnumre),
                Header("Nav-Consumer-Id", "aap-behandlingsflyt"),
                Header("Accept", "application/json")
            )
        )
        return requireNotNull(client.get(uri = url, request = httpRequest, mapper = { body, _ ->
            DefaultJsonMapper.fromJson(body)
        }))
    }

    override fun innhent(person: Person): List<Institusjonsopphold> {
        val request = InstitusjonoppholdRequest(person.aktivIdent().identifikator)
        val oppholdRes = query(request)

        val institusjonsopphold = oppholdRes.map { opphold ->
            Institusjonsopphold.nyttOpphold(
                requireNotNull(opphold.institusjonstype),
                requireNotNull(opphold.kategori),
                requireNotNull(opphold.startdato),
                opphold.faktiskSluttdato ?: opphold.forventetSluttdato,
                opphold.organisasjonsnummer,
                opphold.institusjonsnavn ?: "Ukjent institusjon"
            )
        }
        return institusjonsopphold
    }
}