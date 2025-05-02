package no.nav.aap.behandlingsflyt.periodisering

import no.nav.aap.behandlingsflyt.flyt.utledType
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekst
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType.FØRSTEGANGSBEHANDLING
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType.IKKE_RELEVANT
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType.FORLENGELSE
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType.REVURDERING
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.ÅrsakTilBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService

class PerioderTilVurderingService(
    private val sakService: SakService,
    private val behandlingRepository: BehandlingRepository,
) {

    fun utled(kontekst: FlytKontekst, stegType: StegType): VurderingTilBehandling {
        val sak = sakService.hent(kontekst.sakId)
        val behandling = behandlingRepository.hent(kontekst.behandlingId)

        if (kontekst.behandlingType == TypeBehandling.Førstegangsbehandling) {
            return VurderingTilBehandling(
                vurderingType = FØRSTEGANGSBEHANDLING,
                rettighetsperiode = sak.rettighetsperiode,
                årsakerTilBehandling = behandling.årsaker().map { it.type }.toSet()
            )
        }
        val flyt = utledType(behandling.typeBehandling()).flyt()
        val årsakerRelevantForSteg = flyt.årsakerRelevantForSteg(stegType)

        val relevanteÅrsak = behandling.årsaker()
            .map { årsak -> årsak.type }
            .filter { årsak -> årsakerRelevantForSteg.contains(årsak) }
            .toSet()

        return VurderingTilBehandling(
            vurderingType = prioritertType(relevanteÅrsak.map { årsakTilType(it) }.toSet()),
            rettighetsperiode = sak.rettighetsperiode,
            årsakerTilBehandling = relevanteÅrsak
        )
    }

    private fun prioritertType(vurderingTyper: Set<VurderingType>): VurderingType {
        return when {
            FØRSTEGANGSBEHANDLING in vurderingTyper -> FØRSTEGANGSBEHANDLING
            REVURDERING in vurderingTyper -> REVURDERING
            FORLENGELSE in vurderingTyper -> FORLENGELSE
            else -> IKKE_RELEVANT
        }
    }

    private fun årsakTilType(årsak: ÅrsakTilBehandling): VurderingType {
        return when (årsak) {
            ÅrsakTilBehandling.MOTTATT_SØKNAD -> FØRSTEGANGSBEHANDLING
            ÅrsakTilBehandling.MOTTATT_AKTIVITETSMELDING -> REVURDERING
            ÅrsakTilBehandling.MOTTATT_MELDEKORT -> REVURDERING
            ÅrsakTilBehandling.MOTTATT_LEGEERKLÆRING -> REVURDERING
            ÅrsakTilBehandling.MOTTATT_AVVIST_LEGEERKLÆRING -> REVURDERING
            ÅrsakTilBehandling.MOTTATT_DIALOGMELDING -> REVURDERING
            ÅrsakTilBehandling.G_REGULERING -> REVURDERING
            ÅrsakTilBehandling.REVURDER_MEDLEMSKAP -> REVURDERING
            ÅrsakTilBehandling.REVURDER_BEREGNING -> REVURDERING
            ÅrsakTilBehandling.REVURDER_YRKESSKADE -> REVURDERING
            ÅrsakTilBehandling.REVURDER_LOVVALG -> REVURDERING
            ÅrsakTilBehandling.REVURDER_SAMORDNING -> REVURDERING
            ÅrsakTilBehandling.MOTATT_KLAGE -> IKKE_RELEVANT // TODO: Verifiser at dette er korrekt.
            ÅrsakTilBehandling.LOVVALG_OG_MEDLEMSKAP -> REVURDERING
            ÅrsakTilBehandling.FORUTGAENDE_MEDLEMSKAP -> REVURDERING
            ÅrsakTilBehandling.SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND -> REVURDERING
            ÅrsakTilBehandling.BARNETILLEGG -> REVURDERING
            ÅrsakTilBehandling.INSTITUSJONSOPPHOLD -> REVURDERING
            ÅrsakTilBehandling.SAMORDNING_OG_AVREGNING -> REVURDERING
            ÅrsakTilBehandling.REFUSJONSKRAV -> REVURDERING
            ÅrsakTilBehandling.UTENLANDSOPPHOLD_FOR_SOKNADSTIDSPUNKT -> REVURDERING
            ÅrsakTilBehandling.FASTSATT_PERIODE_PASSERT -> REVURDERING
            ÅrsakTilBehandling.VURDER_RETTIGHETSPERIODE -> REVURDERING
            ÅrsakTilBehandling.SØKNAD_TRUKKET -> REVURDERING
        }
    }
}
