package no.nav.aap.behandlingsflyt.integrasjon.yrkesskade

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.Yrkesskade
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.adapter.YrkesskadeRegisterGateway
import no.nav.aap.behandlingsflyt.prometheus
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.komponenter.json.DefaultJsonMapper
import java.net.URI

/**
 * Se Swagger: https://yrkesskade-saker.intern.dev.nav.no/swagger-ui/index.html#/Saker%20API/hentSaker
 */
object YrkesskadeRegisterGatewayImpl : YrkesskadeRegisterGateway {
    private val url = URI.create(requiredConfigForKey("integrasjon.yrkesskade.url")).resolve("/api/v1/saker/")
    private val config = ClientConfig(
        scope = requiredConfigForKey("integrasjon.yrkesskade.scope"),
        additionalHeaders = listOf(Header("Nav-Consumer-Id", "aap-behandlingsflyt"))
    )
    private val client = RestClient.withDefaultResponseHandler(
        config = config,
        tokenProvider = ClientCredentialsTokenProvider,
        prometheus = prometheus
    )

    override fun innhent(person: Person, fødselsdato: Fødselsdato): List<Yrkesskade> {
        val identer = person.identer().map(Ident::identifikator)
        //TODO: fra når skal yrkesskade hentes
        val request = YrkesskadeRequest(identer, fødselsdato.toLocalDate())
        val httpRequest = PostRequest(body = request)
        val response = client.post(uri = url, request = httpRequest) { body, _ ->
            DefaultJsonMapper.fromJson<Yrkesskader>(body)
        }

        if (response == null) {
            return emptyList()
        }

        // https://github.com/navikt/yrkesskade/blob/main/libs/model-sakoversikt/src/main/kotlin/no/nav/yrkesskade/saksoversikt/model/SakerResultat.kt
        // Kun saker med status GODKJENT eller DELVIS_GODKJENT er yrkesskadesaker som skal med i vurderingen
        val gyldigeStatuser = listOf("GODKJENT", "DELVIS_GODKJENT")

        return response
            .saker
            .filter { it.resultat in gyldigeStatuser}
            .map { yrkesskade ->
                Yrkesskade(
                    ref = yrkesskade.saksreferanse,
                    saksnummer = yrkesskade.saksnr,
                    kildesystem = yrkesskade.kildesystem,
                    skadedato = requireNotNull(yrkesskade.skadedato)
                )
            }
    }
}
