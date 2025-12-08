package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.meldeperiode.MeldeperiodeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.MeldepliktRepository
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.motor.ProvidersJobbSpesifikasjon
import java.time.LocalDate

class OpprettBehandlingFritakMeldepliktJobbUtfører(
    private val sakService: SakService,
    private val behandlingRepository: BehandlingRepository,
    private val meldeperiodeRepository: MeldeperiodeRepository,
    private val meldepliktRepository: MeldepliktRepository,
    private val sakOgBehandlingService: SakOgBehandlingService,
    private val prosesserBehandlingService: ProsesserBehandlingService,
    private val underveisRepository: UnderveisRepository,
) : JobbUtfører {

    override fun utfør(input: JobbInput) {
        val sak = sakService.hent(SakId(input.sakId()))

        if (skalHaFritakForPassertMeldeperiode(sak)) {
            val fritakMeldepliktBehandling = sakOgBehandlingService.finnEllerOpprettBehandling(
                sakId = sak.id,
                vurderingsbehovOgÅrsak = VurderingsbehovOgÅrsak(
                    årsak = ÅrsakTilOpprettelse.FRITAK_MELDEPLIKT,
                    vurderingsbehov = listOf(VurderingsbehovMedPeriode(type = Vurderingsbehov.FRITAK_MELDEPLIKT))
                ),
            )

            prosesserBehandlingService.triggProsesserBehandling(fritakMeldepliktBehandling)
        }
    }

    private fun skalHaFritakForPassertMeldeperiode(sak: Sak):Boolean {
        val nå = LocalDate.now()

        val sisteBehandling = behandlingRepository.finnSisteOpprettedeBehandlingFor(sak.id, listOf(TypeBehandling.Førstegangsbehandling,
            TypeBehandling.Revurdering)) ?: return false
        if (sisteBehandling.status().erÅpen() && Vurderingsbehov.FRITAK_MELDEPLIKT in sisteBehandling.vurderingsbehov().map { it.type }) {
            return false
        }
        val sisteIverksatteBehandling = sakOgBehandlingService.finnBehandlingMedSisteFattedeVedtak(sak.id) ?: return false
        val underveisPerioder = underveisRepository.hentHvisEksisterer(sisteIverksatteBehandling.id) ?: return false
        val aktuellPeriode = underveisPerioder.somTidslinje().helePerioden()

        // NB Sjekker 7 dager tilbake for å få med siste utbetaling som har fritak.
        val sistePasserteMeldeperiode = meldeperiodeRepository.hentMeldeperioder(sisteIverksatteBehandling.id, aktuellPeriode).firstOrNull { it.inneholder(nå.minusDays(7)) } ?: return false

        val meldepliktGrunnlag = meldepliktRepository.hentHvisEksisterer(sisteIverksatteBehandling.id) ?: return false

        return meldepliktGrunnlag.tilTidslinje().begrensetTil(sistePasserteMeldeperiode).segmenter().any { it.verdi.harFritak }
    }


    companion object : ProvidersJobbSpesifikasjon {
        override fun konstruer(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider): JobbUtfører {
            return OpprettBehandlingFritakMeldepliktJobbUtfører(
                sakService = SakService(repositoryProvider),
                behandlingRepository = repositoryProvider.provide(),
                meldeperiodeRepository = repositoryProvider.provide(),
                meldepliktRepository = repositoryProvider.provide(),
                sakOgBehandlingService = SakOgBehandlingService(repositoryProvider, gatewayProvider),
                prosesserBehandlingService = ProsesserBehandlingService(repositoryProvider, gatewayProvider),
                underveisRepository = repositoryProvider.provide(),
            )
        }

        override val type = "batch.OpprettBehandlingFritakMeldeplikt"
        override val navn = "Opprett behandling fordi bruker har fritak meldeplikt"
        override val beskrivelse = "Starter ny behandling hvis siste behandlig fritak for meldeplikt."
    }
}

