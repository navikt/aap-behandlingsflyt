package no.nav.aap.behandlingsflyt.integrasjon.ufore

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.Uføre
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.UføreRegisterGateway
import no.nav.aap.behandlingsflyt.prometheus
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.error.IkkeFunnetException
import no.nav.aap.komponenter.httpklient.httpclient.post
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.komponenter.verdityper.Prosent
import org.slf4j.LoggerFactory
import java.net.URI
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * @param uforegradFom Fra-tidspunkt for [uforegrad].
 * @param uforegrad Uføregrad i prosent. `null` om personen er registrert i systemet, men ikke har uføregrad.
 */
data class UførePeriode(
    val uforegradFom: LocalDate,
    val uforegradTom: LocalDate? = null,
    val uforegrad: Int,
    val uforetidspunkt: LocalDate? = null,
    val virkningstidspunkt: LocalDate
)

data class UføreHistorikkRespons(val uforeperioder: List<UførePeriode>)

object UføreGateway : UføreRegisterGateway {
    private val log = LoggerFactory.getLogger(javaClass)
    private val url = URI.create(requiredConfigForKey("integrasjon.pesys.url"))
    private val config = ClientConfig(scope = requiredConfigForKey("integrasjon.pesys.scope"))
    private val client = RestClient.withDefaultResponseHandler(
        config = config,
        tokenProvider = ClientCredentialsTokenProvider,
        prometheus = prometheus
    )

    private fun queryMedHistorikk(uføreRequest: UføreRequest): UføreHistorikkRespons? {
        val httpRequest = PostRequest(
            additionalHeaders = listOf(
                Header("fnr", uføreRequest.fnr),
                Header("Nav-Consumer-Id", "aap-behandlingsflyt"),
                Header("Accept", "application/json")
            ),
            body = uføreRequest
        )

        val uri = url.resolve("/api/uforetrygd/uforehistorikk/perioder")
        try {
            log.info("Henter uføregrad fra dato: ${uføreRequest.dato}")
            return client.post(
                uri = uri,
                request = httpRequest
            )
        } catch (e: IkkeFunnetException) {
            // Om personen ikke ble funnet i PESYS.
            log.info("Fant ikke person i PESYS. Returnerer null. URL brukt: $uri. Message: ${e.message}")
            return null
        }
    }

    override fun innhentMedHistorikk(person: Person, fraDato: LocalDate): Set<Uføre> {
        val datoString = fraDato.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val request =
            UføreRequest(person.identer().filter { it.aktivIdent }.map { it.identifikator }.first(), datoString)
        val uføreRes = queryMedHistorikk(request)
        val uføreperioder = uføreRes?.uforeperioder.orEmpty()

        return uføreperioder.map {
            Uføre(
                virkningstidspunkt = it.virkningstidspunkt,
                uføregrad = Prosent(it.uforegrad)
            )
        }.toSet()
    }
}
