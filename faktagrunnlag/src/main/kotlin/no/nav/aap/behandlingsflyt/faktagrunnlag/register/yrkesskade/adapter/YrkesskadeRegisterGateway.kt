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
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import java.net.URI
import java.time.LocalDate

class YrkesskadeRequest(
    val foedselsnumre: List<String>,
    val fomDato: LocalDate
)

class Yrkesskader(
    //FIXME: Kan denne være null?? Når da? Ser ut som at yrkesskade-saker alltid returnerer en liste med mindre det er en feil i responsen
    val skader: List<YrkesskadeModell>?
)


class YrkesskadeModell(
    val kommunenr: String,
    val saksblokk: String,
    val saksnr: Int,
    val sakstype: String,
    val mottattdato: LocalDate,
    val resultat: String,
    val resultattekst: String,
    val vedtaksdato: LocalDate,
    val skadeart: String,
    val diagnose: String,
    val skadedato: LocalDate,
    val kildetabell: String,
    val kildesystem: String,
    val saksreferanse: String
)


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

    private fun query(request: YrkesskadeRequest, oppgittYrkesskade: YrkesskadeModell?): Yrkesskader? {

        if (oppgittYrkesskade != null) {
            return Yrkesskader(listOf(oppgittYrkesskade))
        }

        val httpRequest = PostRequest(body = request)
        return client.post(uri = url, request = httpRequest) { body, _ -> DefaultJsonMapper.fromJson(body) }
    }

    fun innhent(person: Person, fødselsdato: Fødselsdato, oppgittYrkesskade: YrkesskadeModell?): List<Yrkesskade> {
        val identer = person.identer().map(Ident::identifikator)
        //TODO: fra når skal yrkesskade hentes
        val request = YrkesskadeRequest(identer, fødselsdato.toLocalDate())
        val response: Yrkesskader? = query(request, oppgittYrkesskade)

        if (response == null) {
            return emptyList()
        }

        //FIXME: Kan denne være null?? Når da? Ser ut som at yrkesskade-saker alltid returnerer en liste med mindre det er en feil i responsen
        val skader = response.skader

        if (skader == null) {
            return emptyList()
        }

        return skader.map { yrkesskade -> Yrkesskade(yrkesskade.saksreferanse, yrkesskade.skadedato) }
    }
}
