package no.nav.aap.behandlingsflyt.prosessering.datadeling

import no.nav.aap.behandlingsflyt.hendelse.datadeling.ApiInternGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.motor.ProvidersJobbSpesifikasjon

class DatadelingNySøknadJobbUtfører(
    private val apiInternGateway: ApiInternGateway,
) : JobbUtfører {
    override fun utfør(input: JobbInput) {
        val personIdent = input.payload()

        apiInternGateway.varsleNySøknadForPerson(personIdent)
    }

    companion object : ProvidersJobbSpesifikasjon {
        override val navn = "Ny søknad til API-intern"
        override val type = "flyt.Datadeling.NySøknad"
        override val beskrivelse = """
                Kaller endepunkt i api-intern for å informere om ny søknad.
                """.trimIndent()

        override fun konstruer(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider): JobbUtfører {
            return DatadelingNySøknadJobbUtfører(
                apiInternGateway = gatewayProvider.provide(ApiInternGateway::class)
            )
        }

        fun nyJobb(
            personIdent: Ident,
            sakId: Long
        ) = JobbInput(DatadelingNySøknadJobbUtfører).apply {
            medPayload(personIdent.identifikator)
            forSak(sakId)
        }
    }
}