package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.datadeling.SakStatus
import no.nav.aap.behandlingsflyt.hendelse.datadeling.ApiInternGateway
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.BehandlingFlytStoppetHendelse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryRegistry
import no.nav.aap.motor.Jobb
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører

class DatadelingSakStatusJobbUtfører(
    private val apiInternGateway: ApiInternGateway,
    private val sakRepository: SakRepository,
    private val behandlingRepository: BehandlingRepository,
) : JobbUtfører {
    override fun utfør(input: JobbInput) {
        val hendelse = input.payload<BehandlingFlytStoppetHendelse>()
        val behandling = behandlingRepository.hent(hendelse.referanse)
        val sak = sakRepository.hent(behandling.sakId)

        apiInternGateway.sendSakStatus(
            sak.person.aktivIdent().identifikator,
            SakStatus.fromKelvin(sak.saksnummer.toString(), sak.status(), sak.rettighetsperiode)
        )
    }

    companion object : Jobb {
        override fun beskrivelse(): String {

            return "Sender status på sak til api-intern."
        }

        override fun konstruer(connection: DBConnection): JobbUtfører {
            val repositoryProvider = RepositoryRegistry.provider(connection)
            val behandlingRepository: BehandlingRepository = repositoryProvider.provide<BehandlingRepository>()
            val sakRepository: SakRepository = repositoryProvider.provide<SakRepository>()

            return DatadelingSakStatusJobbUtfører(
                GatewayProvider.provide(),
                sakRepository,
                behandlingRepository
            )
        }

        override fun navn(): String {
            return "DatadelingSakStatusJobbUtfører"
        }

        override fun type(): String {
            return "flyt.DatadelingSakStatus"
        }

    }
}