package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.behandling.underveis.regler.MeldepliktStatus
import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Årsak
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.ÅrsakTilBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.motor.ProviderJobbSpesifikasjon
import java.time.LocalDate
import java.time.LocalDateTime

class OpprettBehandlingFastsattPeriodePassertJobbUtfører(
    private val sakService: SakService,
    private val underveisRepository: UnderveisRepository,
    private val sakOgBehandlingService: SakOgBehandlingService,
    private val prosesserBehandlingService: ProsesserBehandlingService,
) : JobbUtfører {

    override fun utfør(input: JobbInput) {
        val sak = sakService.hent(SakId(input.sakId()))

        if (!sak.rettighetsperiode.inneholder(LocalDate.now())) {
            return
        }

        val behandling = sakOgBehandlingService.finnSisteYtelsesbehandlingFor(sak.id) ?: return

        if (behandling.status().erÅpen() && ÅrsakTilBehandling.FASTSATT_PERIODE_PASSERT in behandling.årsaker().map { it.type }) {
            return
        }

        val underveisperioder = underveisRepository.hentHvisEksisterer(behandling.id)
            ?.perioder
            ?: return

        val førsteAntatteMeldeperiode = underveisperioder
            .filter { it.meldepliktStatus == MeldepliktStatus.FREMTIDIG_OPPFYLT }
            .minByOrNull { it.meldePeriode }
            ?: return

        val tidspunktForKjøringVedManglendeMeldekort =
            førsteAntatteMeldeperiode.meldePeriode.fom.plusDays(8).atStartOfDay().plusHours(2)

        if (LocalDateTime.now() < tidspunktForKjøringVedManglendeMeldekort) {
            return
        }

        val fastsattPeriodePassertBehandling = sakOgBehandlingService.finnEllerOpprettBehandlingFasttrack(
            sak.id,
            listOf(Årsak(type = ÅrsakTilBehandling.FASTSATT_PERIODE_PASSERT))
        )

        prosesserBehandlingService.triggProsesserBehandling(fastsattPeriodePassertBehandling)
    }

    companion object : ProviderJobbSpesifikasjon {
        override fun konstruer(repositoryProvider: RepositoryProvider): JobbUtfører {
            return OpprettBehandlingFastsattPeriodePassertJobbUtfører(
                sakService = SakService(repositoryProvider),
                underveisRepository = repositoryProvider.provide(),
                sakOgBehandlingService = SakOgBehandlingService(repositoryProvider),
                prosesserBehandlingService = ProsesserBehandlingService(repositoryProvider),
            )
        }

        override val type = "batch.OpprettBehandlingFastsattPeriodePassert"
        override val navn = "Opprett behandling fordi fastsatt dag er passert"
        override val beskrivelse =
            """
                Starter ny behandling hvis siste behandlig har antatt at meldeplikten er oppfylt, men
                fastsatt dag er passert, og meldekort nå mangler.
            """.trimIndent()
    }
}
