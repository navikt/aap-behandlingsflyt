package no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.adapter

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.Yrkesskade
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.httpclient.ClientConfig
import no.nav.aap.httpclient.Header
import no.nav.aap.httpclient.RestClient
import no.nav.aap.httpclient.request.PostRequest
import no.nav.aap.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.requiredConfigForKey
import no.nav.aap.verdityper.sakogbehandling.Ident
import no.nav.aap.yrkesskade.YrkesskadeModell
import no.nav.aap.yrkesskade.YrkesskadeRequest
import no.nav.aap.yrkesskade.Yrkesskader
import java.net.URI
import java.time.LocalDate

object YrkesskadeRegisterGateway {
    @Deprecated("Kun for test")
    private val yrkesskaderTestMap = mutableMapOf<Ident, YrkesskadeModell>()

    @Deprecated("Kun for test")
    fun puttInnTestPerson(ident: Ident, yrkesskadeDato: LocalDate) {
        yrkesskaderTestMap[ident] = YrkesskadeModell(
            kommunenr = "0301",
            saksblokk = "1",
            saksnr = 123456,
            sakstype = "YRK",
            mottattdato = LocalDate.now(),
            resultat = "I",
            resultattekst = "Innvilget",
            vedtaksdato = LocalDate.now(),
            skadeart = "YRK",
            diagnose = "YRK",
            skadedato = yrkesskadeDato,
            kildetabell = "YRK",
            kildesystem = "YRK",
            saksreferanse = "YRK"
        )
    }

    private val url = URI.create(requiredConfigForKey("integrasjon.yrkesskade.url")).resolve("/api/v1/saker/")
    private val client = RestClient(
        config = ClientConfig(
            scope = requiredConfigForKey("integrasjon.yrkesskade.scope"),
            additionalHeaders = listOf(Header("Nav-Consumer-Id", "aap-behandlingsflyt"))
        ), //TODO: bruk env var
        tokenProvider = ClientCredentialsTokenProvider
    )

    private fun query(request: YrkesskadeRequest): Yrkesskader? {
        val funnetIdent = yrkesskaderTestMap.entries.firstOrNull { (key) ->
            key.identifikator in request.foedselsnumre
        }

        if (funnetIdent != null) {
            return Yrkesskader(listOf(funnetIdent.value))
        }

        val httpRequest = PostRequest(body = request)
        return client.post(uri = url, request = httpRequest)
    }

    fun innhent(person: Person, fødselsdato: Fødselsdato): List<Yrkesskade> {
        val identer = person.identer().map { it.identifikator }
        val request = YrkesskadeRequest(identer, fødselsdato.toLocalDate()) //TODO: fra når skal yrkesskade hentes
        val response: Yrkesskader? = query(request)

        val skader = response?.skader?.map { Yrkesskade(it.saksreferanse, it.skadedato) } ?: emptyList()

        return skader
    }

}