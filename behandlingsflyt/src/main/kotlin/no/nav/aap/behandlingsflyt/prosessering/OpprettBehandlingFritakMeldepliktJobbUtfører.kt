package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.behandling.underveis.regler.MeldepliktStatus
import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Årsak
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.ÅrsakTilBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.motor.ProviderJobbSpesifikasjon
import java.time.LocalDate

class OpprettBehandlingFritakMeldepliktJobbUtfører(
    private val sakService: SakService,
    private val behandlingRepository: BehandlingRepository,
    private val underveisRepository: UnderveisRepository,
    private val sakOgBehandlingService: SakOgBehandlingService,
) : JobbUtfører {

    override fun utfør(input: JobbInput) {
        val sak = sakService.hent(SakId(input.sakId()))

        if (!sak.rettighetsperiode.inneholder(LocalDate.now())) {
            return
        }

        val behandling = behandlingRepository.finnSisteBehandlingFor(
            sak.id, listOf(
                TypeBehandling.Førstegangsbehandling,
                TypeBehandling.Revurdering
            )
        ) ?: return

        if (behandling.status().erÅpen() && ÅrsakTilBehandling.FRITAK_MELDEPLIKT in behandling.årsaker().map { it.type }) {
            return
        }

        val underveisperioder = underveisRepository.hentHvisEksisterer(behandling.id)
            ?.perioder
            ?: return

        val førsteAntatteMeldeperiode = underveisperioder
            .filter { it.meldepliktStatus == MeldepliktStatus.FREMTIDIG_OPPFYLT }
            .minByOrNull { it.meldePeriode }
            ?: return

        val startNesteMeldeperiode =
            førsteAntatteMeldeperiode.meldePeriode.fom

        if (LocalDate.now() < startNesteMeldeperiode) {
            return
        }

        sakOgBehandlingService.finnEllerOpprettBehandling(
            sak.id,
            listOf(
                Årsak(
                    type = ÅrsakTilBehandling.FRITAK_MELDEPLIKT,
                )
            )
        )
    }
    companion object : ProviderJobbSpesifikasjon {
        override fun konstruer(repositoryProvider: RepositoryProvider): JobbUtfører {
            return OpprettBehandlingFritakMeldepliktJobbUtfører(
                sakService = SakService(repositoryProvider),
                behandlingRepository = repositoryProvider.provide(),
                underveisRepository = repositoryProvider.provide(),
                sakOgBehandlingService = SakOgBehandlingService(repositoryProvider),
            )
        }

        override val type = "batch.OpprettBehandlingFritakMeldeplikt"
        override val navn = "Opprett behandling fordi bruker har fritak meldeplikt"
        override val beskrivelse = "Starter ny behandling hvis siste behandlig fritak for meldeplikt."
    }
}

