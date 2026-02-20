package no.nav.aap.behandlingsflyt.periodisering

import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekst
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType.AUTOMATISK_BREV
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType.EFFEKTUER_AKTIVITETSPLIKT
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType.EFFEKTUER_AKTIVITETSPLIKT_11_9
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType.FØRSTEGANGSBEHANDLING
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType.IKKE_RELEVANT
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType.MELDEKORT
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType.MIGRER_RETTIGHETSPERIODE
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType.REVURDERING
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType.UTVID_VEDTAKSLENGDE
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

        val flyt = behandling.flyt()
        val vurderingsbehovRelevanteForSteg = flyt.vurderingsbehovRelevantForSteg(stegType)

        val relevanteVurderingsbehovMedPerioder = behandling.vurderingsbehov()
            .filter { vurderingsbehov -> vurderingsbehovRelevanteForSteg.contains(vurderingsbehov.type) }
            .toSet()

        return FlytKontekstMedPerioder(
            sakId = kontekst.sakId,
            behandlingId = kontekst.behandlingId,
            forrigeBehandlingId = kontekst.forrigeBehandlingId,
            behandlingType = kontekst.behandlingType,
            vurderingType = prioritertType(
                vurderingTyper = behandling.vurderingsbehov().map { vurderingsbehovTilType(it.type) }.toSet(),
                typeBehandling = kontekst.behandlingType
            ),
            rettighetsperiode = sak.rettighetsperiode,
            vurderingsbehovRelevanteForStegMedPerioder = relevanteVurderingsbehovMedPerioder
        )
    }

    private fun prioritertType(vurderingTyper: Set<VurderingType>, typeBehandling: TypeBehandling): VurderingType {
        if (typeBehandling == TypeBehandling.Førstegangsbehandling) {
            return FØRSTEGANGSBEHANDLING
        }
        return when {
            FØRSTEGANGSBEHANDLING in vurderingTyper -> FØRSTEGANGSBEHANDLING
            REVURDERING in vurderingTyper -> REVURDERING
            MELDEKORT in vurderingTyper -> MELDEKORT
            EFFEKTUER_AKTIVITETSPLIKT in vurderingTyper -> EFFEKTUER_AKTIVITETSPLIKT
            EFFEKTUER_AKTIVITETSPLIKT_11_9 in vurderingTyper -> EFFEKTUER_AKTIVITETSPLIKT_11_9
            UTVID_VEDTAKSLENGDE in vurderingTyper -> UTVID_VEDTAKSLENGDE
            MIGRER_RETTIGHETSPERIODE in vurderingTyper -> MIGRER_RETTIGHETSPERIODE
            AUTOMATISK_BREV in vurderingTyper -> AUTOMATISK_BREV
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
            Vurderingsbehov.REVURDER_SYKEPENGEERSTATNING,
            Vurderingsbehov.BARNETILLEGG,
            Vurderingsbehov.INSTITUSJONSOPPHOLD,
            Vurderingsbehov.SAMORDNING_OG_AVREGNING,
            Vurderingsbehov.REVURDER_SAMORDNING_ANDRE_FOLKETRYGDYTELSER,
            Vurderingsbehov.REVURDER_SAMORDNING_UFØRE,
            Vurderingsbehov.REVURDER_SAMORDNING_ANDRE_STATLIGE_YTELSER,
            Vurderingsbehov.REVURDER_SAMORDNING_ARBEIDSGIVER,
            Vurderingsbehov.REVURDER_SAMORDNING_TJENESTEPENSJON,
            Vurderingsbehov.REVURDER_SYKESTIPEND,
            Vurderingsbehov.REFUSJONSKRAV,
            Vurderingsbehov.VURDER_RETTIGHETSPERIODE,
            Vurderingsbehov.SØKNAD_TRUKKET,
            Vurderingsbehov.DØDSFALL_BRUKER,
            Vurderingsbehov.DØDSFALL_BARN,
            Vurderingsbehov.REVURDER_FRITAK_MELDEPLIKT,
            Vurderingsbehov.REVURDERING_AVBRUTT ->
                REVURDERING

            Vurderingsbehov.REVURDER_MANUELL_INNTEKT,
            Vurderingsbehov.REVURDER_MELDEPLIKT_RIMELIG_GRUNN,
            Vurderingsbehov.OVERGANG_UFORE,
            Vurderingsbehov.OVERGANG_ARBEID,
            Vurderingsbehov.OPPHOLDSKRAV,
            Vurderingsbehov.ETABLERING_EGEN_VIRKSOMHET,
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
            Vurderingsbehov.UTVID_VEDTAKSLENGDE -> UTVID_VEDTAKSLENGDE
            Vurderingsbehov.MIGRER_RETTIGHETSPERIODE -> MIGRER_RETTIGHETSPERIODE
            Vurderingsbehov.BARNETILLEGG_SATS_REGULERING -> AUTOMATISK_BREV
        }
    }
}
