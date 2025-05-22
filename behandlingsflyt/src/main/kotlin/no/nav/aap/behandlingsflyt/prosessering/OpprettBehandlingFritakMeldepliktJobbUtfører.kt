package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.behandling.underveis.regler.MeldepliktStatus
import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.prosessering.ProsesserBehandlingJobbUtfører.Companion.skjedulerProsesserBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Årsak
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.ÅrsakTilBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.motor.ProviderJobbSpesifikasjon
import java.time.LocalDate

class OpprettBehandlingFritakMeldepliktJobbUtfører(
    private val sakService: SakService,
    private val underveisRepository: UnderveisRepository,
    private val sakOgBehandlingService: SakOgBehandlingService,
    private val flytJobbRepository: FlytJobbRepository,
) : JobbUtfører {

    override fun utfør(input: JobbInput) {
        val sak = sakService.hent(SakId(input.sakId()))

        if (skalHaFritakForPassertMeldeperiode(sak)) {
            val fritakMeldepliktBehandling = sakOgBehandlingService.finnEllerOpprettBehandling(
                sak.id,
                listOf(Årsak(type = ÅrsakTilBehandling.FRITAK_MELDEPLIKT))
            )

            flytJobbRepository.skjedulerProsesserBehandling(fritakMeldepliktBehandling)
        }
    }

    private fun skalHaFritakForPassertMeldeperiode(sak: Sak):Boolean {
        val nå = LocalDate.now()
        if (!sak.rettighetsperiode.inneholder(nå)) {
            return false
        }

        val behandling = sakOgBehandlingService.finnSisteYtelsesbehandlingFor(sak.id) ?: return false

        if (behandling.status().erÅpen() && ÅrsakTilBehandling.FRITAK_MELDEPLIKT in behandling.årsaker().map { it.type }) {
            return false
        }

        val underveisperioder = underveisRepository.hentHvisEksisterer(behandling.id)
            ?.perioder
            ?: return false

        val førsteAntatteMeldeperiode = underveisperioder
            .filter { it.meldepliktStatus == MeldepliktStatus.FRITAK }
            .minByOrNull { it.meldePeriode }
            ?: return false

        val startNesteMeldeperiode = førsteAntatteMeldeperiode.meldePeriode.fom

        return nå >= startNesteMeldeperiode
    }


    companion object : ProviderJobbSpesifikasjon {
        override fun konstruer(repositoryProvider: RepositoryProvider): JobbUtfører {
            return OpprettBehandlingFritakMeldepliktJobbUtfører(
                sakService = SakService(repositoryProvider),
                underveisRepository = repositoryProvider.provide(),
                sakOgBehandlingService = SakOgBehandlingService(repositoryProvider),
                flytJobbRepository = repositoryProvider.provide(),
            )
        }

        override val type = "batch.OpprettBehandlingFritakMeldeplikt"
        override val navn = "Opprett behandling fordi bruker har fritak meldeplikt"
        override val beskrivelse = "Starter ny behandling hvis siste behandlig fritak for meldeplikt."
    }
}

