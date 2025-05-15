package no.nav.aap.behandlingsflyt.periodisering

import no.nav.aap.behandlingsflyt.flyt.utledType
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekst
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType.FØRSTEGANGSBEHANDLING
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType.IKKE_RELEVANT
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType.MELDEKORT
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType.REVURDERING
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.ÅrsakTilBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

class PerioderTilVurderingService(
    private val sakService: SakService,
    private val behandlingRepository: BehandlingRepository,
    private val unleashGateway: UnleashGateway,
) {
    constructor(repositoryProvider: RepositoryProvider): this(
        sakService = SakService(repositoryProvider),
        behandlingRepository = repositoryProvider.provide(),
        unleashGateway = GatewayProvider.provide(),
    )

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
            MELDEKORT in vurderingTyper -> MELDEKORT
            else -> IKKE_RELEVANT
        }
    }

    private fun årsakTilType(årsak: ÅrsakTilBehandling): VurderingType {
        return when (årsak) {
            ÅrsakTilBehandling.MOTTATT_SØKNAD ->
                FØRSTEGANGSBEHANDLING

            ÅrsakTilBehandling.MOTTATT_AKTIVITETSMELDING,
            ÅrsakTilBehandling.MOTTATT_LEGEERKLÆRING,
            ÅrsakTilBehandling.MOTTATT_AVVIST_LEGEERKLÆRING,
            ÅrsakTilBehandling.MOTTATT_DIALOGMELDING,
            ÅrsakTilBehandling.G_REGULERING,
            ÅrsakTilBehandling.REVURDER_MEDLEMSKAP,
            ÅrsakTilBehandling.REVURDER_BEREGNING,
            ÅrsakTilBehandling.REVURDER_YRKESSKADE,
            ÅrsakTilBehandling.REVURDER_LOVVALG,
            ÅrsakTilBehandling.REVURDER_SAMORDNING,
            ÅrsakTilBehandling.LOVVALG_OG_MEDLEMSKAP,
            ÅrsakTilBehandling.FORUTGAENDE_MEDLEMSKAP,
            ÅrsakTilBehandling.SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND,
            ÅrsakTilBehandling.BARNETILLEGG,
            ÅrsakTilBehandling.INSTITUSJONSOPPHOLD,
            ÅrsakTilBehandling.SAMORDNING_OG_AVREGNING,
            ÅrsakTilBehandling.REFUSJONSKRAV,
            ÅrsakTilBehandling.VURDER_RETTIGHETSPERIODE,
            ÅrsakTilBehandling.SØKNAD_TRUKKET,
            ÅrsakTilBehandling.UTENLANDSOPPHOLD_FOR_SOKNADSTIDSPUNKT ->
                REVURDERING

            ÅrsakTilBehandling.MOTTATT_MELDEKORT,
            ÅrsakTilBehandling.FASTSATT_PERIODE_PASSERT ->
                if (unleashGateway.isEnabled(BehandlingsflytFeature.FasttrackMeldekort))
                    MELDEKORT
                else
                    REVURDERING

            ÅrsakTilBehandling.MOTATT_KLAGE,
             ÅrsakTilBehandling.KLAGE_TRUKKET ->
                IKKE_RELEVANT // TODO: Verifiser at dette er korrekt.
        }
    }
}
