package no.nav.aap.behandlingsflyt.integrasjon.oppgave

import no.nav.aap.behandlingsflyt.hendelse.oppgavestyring.OppgavestyringGateway
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.BehandlingFlytStoppetHendelse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.TilbakekrevingsbehandlingOppdatertHendelse
import no.nav.aap.behandlingsflyt.kontrakt.oppgave.EnhetForPersonRequest
import no.nav.aap.behandlingsflyt.kontrakt.oppgave.EnhetNrDto
import no.nav.aap.behandlingsflyt.prometheus
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.post
import no.nav.aap.komponenter.httpklient.httpclient.request.GetRequest
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.oppgave.enhet.OppgaveEnhetResponse
import no.nav.aap.komponenter.httpklient.httpclient.get
import java.net.URI

object OppgavestyringGatewayImpl : OppgavestyringGateway {
    private val url = URI.create(requiredConfigForKey("integrasjon.oppgavestyring.url"))
    private val config = ClientConfig(scope = requiredConfigForKey("integrasjon.oppgavestyring.scope"))

    private val client = RestClient.withDefaultResponseHandler(
        config = config,
        tokenProvider = ClientCredentialsTokenProvider,
        prometheus = prometheus
    )

    override fun varsleHendelse(hendelse: BehandlingFlytStoppetHendelse) {
        client.post<_, Unit>(url.resolve("/oppdater-oppgaver"), PostRequest(body = hendelse))
    }

    override fun varsleTilbakekrevingHendelse(hendelse: TilbakekrevingsbehandlingOppdatertHendelse) {
        client.post<_, Unit>(url.resolve("/oppdater-tilbakekreving-oppgaver"), PostRequest(body = hendelse))
    }

    override fun finnNayEnhetForPerson(
        personIdent: String,
        relevanteIdenter: List<String>
    ): EnhetNrDto {
        val enhet: EnhetNrDto? = client.post<EnhetForPersonRequest, EnhetNrDto>(
            uri = url.resolve("/enhet/nay/person"),
            request = PostRequest(
                body = EnhetForPersonRequest(
                    personIdent = personIdent,
                    relevanteIdenter = relevanteIdenter
                )
            ),
            mapper = { body, _ ->
                DefaultJsonMapper.fromJson(body)
            }
        )

        return enhet ?: error("Fant ikke enhet for person")
    }

    override fun hentOppgaveEnhet(behandlingReferanse: BehandlingReferanse): OppgaveEnhetResponse {
        val request = GetRequest(
            additionalHeaders = listOf(
                Header("Accept", "application/json")
            )
        )
        return checkNotNull(
            client.get<OppgaveEnhetResponse>(
                uri = url.resolve("/${behandlingReferanse.referanse}/hent-oppgave-enhet"),
                request = request
            )
        ) {
            "Mangler response for enheter på oppgaver for behandling ${behandlingReferanse.referanse}"
        }
    }
}
