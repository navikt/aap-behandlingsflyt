package no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.adapter

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.adapter.InntektGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.Uføre
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.UføreRegisterGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.httpclient.ClientConfig
import no.nav.aap.httpclient.Header
import no.nav.aap.httpclient.RestClient
import no.nav.aap.httpclient.request.GetRequest
import no.nav.aap.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.requiredConfigForKey
import no.nav.aap.uføre.UføreRequest
import no.nav.aap.verdityper.Prosent
import java.net.URI
import java.time.format.DateTimeFormatter

object UføreGateway : UføreRegisterGateway {
    private val url = URI.create(requiredConfigForKey("integrasjon.pesys.url"))
    val config = ClientConfig(scope = requiredConfigForKey("integrasjon.pesys.scope"))
    private val client = RestClient(
        config = InntektGateway.config,
        tokenProvider = ClientCredentialsTokenProvider,
    )

    private fun query(uføreRequest: UføreRequest):Int? {
        val httpRequest = GetRequest(
            additionalHeaders = listOf(
                Header("Nav-Personident", uføreRequest.fnr.first().toString()),
                Header("Nav-Consumer-Id", "aap-behandlingsflyt"),
                Header("Accept", "application/json")
            )
        )

        return client.get(uri = url.resolve("vedtak/gradalderellerufore?fom=${uføreRequest.fom}&sakstype=${uføreRequest.sakstype}"), request = httpRequest)
    } //TODO: få inn request parameter på en bedre måte
    // /springapi/vedtak/gradalderellerufore
    override fun innhent(person: Person, Fødselsdato: Fødselsdato): Uføre
    {
        val fom = Fødselsdato.toLocalDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val request = UføreRequest(person.identer().map { it.identifikator }, fom) // TODO: fra når skal uføre hentes
        val uføreRes = query(request)

        if(uføreRes == null) return Uføre(uføregrad = Prosent(0))

        return Uføre(
            uføregrad = Prosent(uføreRes),
        )
    }

}