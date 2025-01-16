package no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.adapter

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.Uføre
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.UføreRegisterGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.error.IkkeFunnetException
import no.nav.aap.komponenter.httpklient.httpclient.get
import no.nav.aap.komponenter.httpklient.httpclient.request.GetRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.komponenter.miljo.Miljø
import no.nav.aap.komponenter.miljo.MiljøKode
import no.nav.aap.komponenter.verdityper.Prosent
import org.slf4j.LoggerFactory
import java.net.URI
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * @param uforegrad Uføregrad i prosent. `null` om personen er registrert i systemet, men ikke har uføregrad.
 */
data class UføreRespons(val uforegrad: Int?)

private val logger = LoggerFactory.getLogger(UføreGateway::class.java)

object UføreGateway : UføreRegisterGateway {
    private val url = URI.create(requiredConfigForKey("integrasjon.pesys.url"))
    private val config = ClientConfig(scope = requiredConfigForKey("integrasjon.pesys.scope"))
    private val client = RestClient.withDefaultResponseHandler(
        config = config,
        tokenProvider = ClientCredentialsTokenProvider,
    )

    private fun query(uføreRequest: UføreRequest): UføreRespons? {
        val httpRequest = GetRequest(
            additionalHeaders = listOf(
                Header("Nav-Personident", uføreRequest.fnr.first().toString()),
                Header("Nav-Consumer-Id", "aap-behandlingsflyt"),
                Header("Accept", "application/json")
            )
        )

        try {
            logger.info("Henter uføregrad for dato: ${uføreRequest.dato}")
            return client.get(
                uri = url.resolve("api/uforetrygd/uforegrad?dato=${uføreRequest.dato}"),
                request = httpRequest
            )
        } catch (e: IkkeFunnetException) {
            // Om personen ikke ble funnet.
            logger.info("Fant ikke person i PESYS. Returnerer null.")
            return null
        }
    }

    override fun innhent(person: Person, forDato: LocalDate): Uføre {
        val datoString = forDato.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val request =
            UføreRequest(person.identer().filter { it.aktivIdent }.map { it.identifikator }.first(), datoString)
        val uføreRes = query(request) ?: error("Respons skal aldri være null fra PESYS.")

        return Uføre(
            uføregrad = uføreRes.uforegrad?.let { Prosent(it) } ?: Prosent.`0_PROSENT`
        )
    }
}
