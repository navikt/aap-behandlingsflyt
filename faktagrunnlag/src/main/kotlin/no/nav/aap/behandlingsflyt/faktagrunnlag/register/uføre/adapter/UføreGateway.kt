package no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.adapter

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.Uføre
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.UføreRegisterGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.httpclient.ClientConfig
import no.nav.aap.httpclient.Header
import no.nav.aap.httpclient.RestClient
import no.nav.aap.httpclient.get
import no.nav.aap.httpclient.request.GetRequest
import no.nav.aap.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.requiredConfigForKey
import no.nav.aap.uføre.UføreRequest
import no.nav.aap.verdityper.Prosent
import java.net.URI
import java.time.format.DateTimeFormatter

object UføreGateway : UføreRegisterGateway {
    private val url = URI.create(requiredConfigForKey("integrasjon.pesys.url"))
    private val config = ClientConfig(scope = requiredConfigForKey("integrasjon.pesys.scope"))
    private val client = RestClient.withDefaultResponseHandler(
        config = config,
        tokenProvider = ClientCredentialsTokenProvider,
    )

    private fun query(uføreRequest: UføreRequest): Int? {
        val httpRequest = GetRequest(
            additionalHeaders = listOf(
                Header("Nav-Personident", uføreRequest.fnr.first().toString()),
                Header("Nav-Consumer-Id", "aap-behandlingsflyt"),
                Header("Accept", "application/json")
            )
        )

        return client.get(
            uri = url.resolve("vedtak/gradalderellerufore?fom=${uføreRequest.fom}&sakstype=${uføreRequest.sakstype}"),
            request = httpRequest
        )
    } //TODO: få inn request parameter på en bedre måte

    // /springapi/vedtak/gradalderellerufore
    // https://github.com/navikt/pensjon-pen/blob/16fd07f36c6bfaaeeb1e5e139834f07f9c59b0e2/pen-app/src/main/java/no/nav/pensjon/pen_app/provider/api/vedtak/VedtakController.kt#L288
    override fun innhent(person: Person, fødselsdato: Fødselsdato): Uføre {
        //FIXME: Fjerne mock respons
        if (true) {
            return Uføre(Prosent.`0_PROSENT`)
        }

        val fom = fødselsdato.toLocalDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val request = UføreRequest(person.identer().map { it.identifikator }, fom) // TODO: fra når skal uføre hentes
        val uføreRes = query(request)

        if (uføreRes == null) return Uføre(uføregrad = Prosent.`0_PROSENT`)

        return Uføre(
            uføregrad = Prosent(uføreRes),
        )
    }
}
