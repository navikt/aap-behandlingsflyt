package no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.adapter

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Institusjonsopphold
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.InstitusjonsoppholdGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.httpclient.ClientConfig
import no.nav.aap.httpclient.Header
import no.nav.aap.httpclient.RestClient
import no.nav.aap.httpclient.get
import no.nav.aap.httpclient.request.GetRequest
import no.nav.aap.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.institusjon.InstitusjonoppholdRequest
import no.nav.aap.requiredConfigForKey
import java.net.URI

object InstitusjonsoppholdGateway : InstitusjonsoppholdGateway {
    private val url = URI.create(requiredConfigForKey("integrasjon.institusjonsopphold.url") + "?Med-Institusjonsinformasjon=true")
    private val config = ClientConfig(scope = requiredConfigForKey("integrasjon.institusjonsopphold.scope"))
    private val client = RestClient.withDefaultResponseHandler(
        config = config,
        tokenProvider = ClientCredentialsTokenProvider,
    )

    private fun query(request: InstitusjonoppholdRequest): List<no.nav.aap.institusjon.Institusjonsopphold> {
        val httpRequest = GetRequest(
            additionalHeaders = listOf(
                Header("Nav-Personident", request.foedselsnumre),
                Header("Nav-Consumer-Id", "aap-behandlingsflyt"),
                Header("Accept", "application/json")
            )
        )
        return requireNotNull(client.get(uri = url, request = httpRequest))
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
                requireNotNull(opphold.organisasjonsnummer),
                requireNotNull(opphold.institusjonsnavn)
            )
        }
        return institusjonsopphold
    }
}