package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.hendelse.datadeling.ApiInternGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.Jobb
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører

class DatadelingJobbUtfører(
    private val apiInternGateway: ApiInternGateway,
    private val behandlingRepository: BehandlingRepository,
    private val sakRepository: SakRepository,
    private val underveisRepository: UnderveisRepository,
): JobbUtfører {
    override fun utfør(input: JobbInput) {
        val behandling = behandlingRepository.hent(BehandlingId(input.behandlingId()))
        val sak = sakRepository.hent(behandling.sakId)
        val personIdent = sak.person.aktivIdent().identifikator

        val underveisGrunnlag = underveisRepository.hent(behandling.id)

        val perioder = underveisGrunnlag.perioder.map { periode ->
            periode.meldePeriode
        }.toSet().toList()


        try {
            apiInternGateway.sendPerioder(personIdent, perioder)
        } catch (e: Exception) {
            throw RuntimeException("Feil ved sending av perioder til datadeling", e)
        }

    }

    companion object : Jobb {
        override fun beskrivelse(): String {

            return "Sender meldekort perioder og vedtaksdata til api-intern."
        }

        override fun konstruer(connection: DBConnection): JobbUtfører {
            val repositoryProvider = RepositoryProvider(connection)
            val behandlingRepository: BehandlingRepository = repositoryProvider.provide<BehandlingRepository>()
            val sakRepository: SakRepository = repositoryProvider.provide<SakRepository>()
            val underveisRepository: UnderveisRepository = repositoryProvider.provide<UnderveisRepository>()

            return DatadelingJobbUtfører(
                ApiInternGateway(),
                behandlingRepository,
                sakRepository,
                underveisRepository
            )
        }

        override fun navn(): String {
            return "DatadelingJobbUtfører"
        }

        override fun type(): String {
            return "flyt.datadeling"
        }

    }
}