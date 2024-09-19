package no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.adapter

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.Yrkesskade
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.komponenter.httpklient.json.DefaultJsonMapper
import no.nav.aap.verdityper.sakogbehandling.Ident
import no.nav.aap.yrkesskade.YrkesskadeRequest
import no.nav.aap.yrkesskade.Yrkesskader
import java.net.URI

object YrkesskadeRegisterGateway {
    private val url = URI.create(requiredConfigForKey("integrasjon.yrkesskade.url")).resolve("/api/v1/saker/")
    private val config = ClientConfig(
        scope = requiredConfigForKey("integrasjon.yrkesskade.scope"),
        additionalHeaders = listOf(Header("Nav-Consumer-Id", "aap-behandlingsflyt"))
    )
    private val client = RestClient.withDefaultResponseHandler(
        config = config,
        tokenProvider = ClientCredentialsTokenProvider
    )

    private fun query(request: YrkesskadeRequest): Yrkesskader? {
        val httpRequest = PostRequest(body = request)
        return client.post(uri = url, request = httpRequest) { body, _ -> DefaultJsonMapper.fromJson(body) }
    }

    fun innhent(person: Person, fødselsdato: Fødselsdato): List<Yrkesskade> {
        val identer = person.identer().map(Ident::identifikator)
        //TODO: fra når skal yrkesskade hentes
        val request = YrkesskadeRequest(identer, fødselsdato.toLocalDate())
        val response: Yrkesskader? = query(request)

        if (response == null) {
            return emptyList()
        }

        return response.skader.map { yrkesskade -> Yrkesskade(yrkesskade.saksreferanse, yrkesskade.skadedato) }
    }
}
