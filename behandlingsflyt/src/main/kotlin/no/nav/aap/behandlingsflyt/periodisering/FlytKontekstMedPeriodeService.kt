package no.nav.aap.behandlingsflyt.periodisering

import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekst
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType.FØRSTEGANGSBEHANDLING
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType.IKKE_RELEVANT
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType.MELDEKORT
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType.REVURDERING
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType.EFFEKTUER_AKTIVITETSPLIKT
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType.EFFEKTUER_AKTIVITETSPLIKT_11_9
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

class FlytKontekstMedPeriodeService(
    private val sakService: SakService,
    private val behandlingRepository: BehandlingRepository,
) {
    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        sakService = SakService(repositoryProvider),
        behandlingRepository = repositoryProvider.provide(),
    )

    fun utled(kontekst: FlytKontekst, stegType: StegType): FlytKontekstMedPerioder {
        val sak = sakService.hent(kontekst.sakId)
        val behandling = behandlingRepository.hent(kontekst.behandlingId)

        if (kontekst.behandlingType == TypeBehandling.Førstegangsbehandling) {
            return FlytKontekstMedPerioder(
                sakId = kontekst.sakId,
                behandlingId = kontekst.behandlingId,
                forrigeBehandlingId = kontekst.forrigeBehandlingId,
                behandlingType = kontekst.behandlingType,
                vurderingType = FØRSTEGANGSBEHANDLING,
                vurderingTypeRelevantForSteg = FØRSTEGANGSBEHANDLING,
                rettighetsperiode = sak.rettighetsperiode,
                vurderingsbehovRelevanteForSteg = behandling.vurderingsbehov().map { it.type }.toSet()
            )
        }
        val flyt = behandling.flyt()
        val vurderingsbehovRelevanteForSteg = flyt.vurderingsbehovRelevantForSteg(stegType)

        val relevanteVurderingsbehov = behandling.vurderingsbehov()
            .map { vurderingsbehov -> vurderingsbehov.type }
            .filter { vurderingsbehov -> vurderingsbehovRelevanteForSteg.contains(vurderingsbehov) }
            .toSet()

        return FlytKontekstMedPerioder(
            sakId = kontekst.sakId,
            behandlingId = kontekst.behandlingId,
            forrigeBehandlingId = kontekst.forrigeBehandlingId,
            behandlingType = kontekst.behandlingType,
            vurderingType = prioritertType(behandling.vurderingsbehov().map { vurderingsbehovTilType(it.type) }
                .toSet()),
            vurderingTypeRelevantForSteg = prioritertType(relevanteVurderingsbehov.map { vurderingsbehovTilType(it) }
                .toSet()),
            rettighetsperiode = sak.rettighetsperiode,
            vurderingsbehovRelevanteForSteg = relevanteVurderingsbehov
        )
    }

    private fun prioritertType(vurderingTyper: Set<VurderingType>): VurderingType {
        return when {
            FØRSTEGANGSBEHANDLING in vurderingTyper -> FØRSTEGANGSBEHANDLING
            REVURDERING in vurderingTyper -> REVURDERING
            MELDEKORT in vurderingTyper -> MELDEKORT
            EFFEKTUER_AKTIVITETSPLIKT in vurderingTyper -> EFFEKTUER_AKTIVITETSPLIKT
            EFFEKTUER_AKTIVITETSPLIKT_11_9 in vurderingTyper -> EFFEKTUER_AKTIVITETSPLIKT_11_9
            else -> IKKE_RELEVANT
        }
    }

    private fun vurderingsbehovTilType(vurderingsbehov: Vurderingsbehov): VurderingType {
        return when (vurderingsbehov) {
            Vurderingsbehov.MOTTATT_SØKNAD ->
                FØRSTEGANGSBEHANDLING

            Vurderingsbehov.HELHETLIG_VURDERING,
            Vurderingsbehov.MOTTATT_AKTIVITETSMELDING,
            Vurderingsbehov.MOTTATT_LEGEERKLÆRING,
            Vurderingsbehov.MOTTATT_AVVIST_LEGEERKLÆRING,
            Vurderingsbehov.MOTTATT_DIALOGMELDING,
            Vurderingsbehov.G_REGULERING,
            Vurderingsbehov.REVURDER_MEDLEMSKAP,
            Vurderingsbehov.REVURDER_BEREGNING,
            Vurderingsbehov.REVURDER_YRKESSKADE,
            Vurderingsbehov.REVURDER_LOVVALG,
            Vurderingsbehov.REVURDER_SAMORDNING,
            Vurderingsbehov.REVURDER_STUDENT,
            Vurderingsbehov.LOVVALG_OG_MEDLEMSKAP,
            Vurderingsbehov.FORUTGAENDE_MEDLEMSKAP,
            Vurderingsbehov.SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND,
            Vurderingsbehov.BARNETILLEGG,
            Vurderingsbehov.INSTITUSJONSOPPHOLD,
            Vurderingsbehov.SAMORDNING_OG_AVREGNING,
            Vurderingsbehov.REVURDER_SAMORDNING_ANDRE_FOLKETRYGDYTELSER,
            Vurderingsbehov.REVURDER_SAMORDNING_UFØRE,
            Vurderingsbehov.REVURDER_SAMORDNING_ANDRE_STATLIGE_YTELSER,
            Vurderingsbehov.REVURDER_SAMORDNING_ARBEIDSGIVER,
            Vurderingsbehov.REVURDER_SAMORDNING_TJENESTEPENSJON,
            Vurderingsbehov.REFUSJONSKRAV,
            Vurderingsbehov.VURDER_RETTIGHETSPERIODE,
            Vurderingsbehov.SØKNAD_TRUKKET,
            Vurderingsbehov.DØDSFALL_BRUKER,
            Vurderingsbehov.DØDSFALL_BARN,
            Vurderingsbehov.REVURDERING_AVBRUTT,
            Vurderingsbehov.ARBEIDSOPPTRAPPING
                ->
                REVURDERING

            Vurderingsbehov.REVURDER_MANUELL_INNTEKT,
            Vurderingsbehov.REVURDER_MELDEPLIKT_RIMELIG_GRUNN,
            Vurderingsbehov.OVERGANG_UFORE,
            Vurderingsbehov.OVERGANG_ARBEID,
            Vurderingsbehov.OPPHOLDSKRAV,
            Vurderingsbehov.UTENLANDSOPPHOLD_FOR_SOKNADSTIDSPUNKT ->
                REVURDERING

            Vurderingsbehov.MOTTATT_MELDEKORT,
            Vurderingsbehov.FASTSATT_PERIODE_PASSERT,
            Vurderingsbehov.FRITAK_MELDEPLIKT -> MELDEKORT

            Vurderingsbehov.MOTATT_KLAGE,
            Vurderingsbehov.KLAGE_TRUKKET, Vurderingsbehov.MOTTATT_KABAL_HENDELSE,
                ->
                IKKE_RELEVANT // TODO: Verifiser at dette er korrekt.
            Vurderingsbehov.OPPFØLGINGSOPPGAVE -> IKKE_RELEVANT
            Vurderingsbehov.AKTIVITETSPLIKT_11_7, Vurderingsbehov.AKTIVITETSPLIKT_11_9 -> IKKE_RELEVANT
            Vurderingsbehov.EFFEKTUER_AKTIVITETSPLIKT -> EFFEKTUER_AKTIVITETSPLIKT
            Vurderingsbehov.EFFEKTUER_AKTIVITETSPLIKT_11_9 -> EFFEKTUER_AKTIVITETSPLIKT_11_9
        }
    }
}
