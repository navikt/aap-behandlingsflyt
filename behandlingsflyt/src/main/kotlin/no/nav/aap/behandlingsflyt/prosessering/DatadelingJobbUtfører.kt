package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.meldeperiode.MeldeperiodeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.hendelse.datadeling.ApiInternGateway
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.BehandlingFlytStoppetHendelse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.Jobb
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører

class DatadelingJobbUtfører(
    private val apiInternGateway: ApiInternGateway,
    private val behandlingRepository: BehandlingRepository,
    private val sakRepository: SakRepository,
    private val meldeperiodeRepository: MeldeperiodeRepository
) : JobbUtfører {
    override fun utfør(input: JobbInput) {
        val hendelse = input.payload<BehandlingFlytStoppetHendelse>()
        val behandling = behandlingRepository.hent(hendelse.referanse)
        val sak = sakRepository.hent(behandling.sakId)
        val personIdent = sak.person.aktivIdent().identifikator

        val perioder = meldeperiodeRepository.hentHvisEksisterer(behandling.id)

        if (perioder == null) {
            return
        }
        apiInternGateway.sendPerioder(personIdent, perioder)

    }

    companion object : Jobb {
        override fun beskrivelse(): String {

            return "Sender meldekort perioder og vedtaksdata til api-intern."
        }

        override fun konstruer(connection: DBConnection): JobbUtfører {
            val repositoryProvider = RepositoryProvider(connection)
            val behandlingRepository: BehandlingRepository = repositoryProvider.provide<BehandlingRepository>()
            val sakRepository: SakRepository = repositoryProvider.provide<SakRepository>()
            val meldeperiodeRepository: MeldeperiodeRepository = repositoryProvider.provide<MeldeperiodeRepository>()

            return DatadelingJobbUtfører(
                GatewayProvider.provide(),
                behandlingRepository,
                sakRepository,
                meldeperiodeRepository
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