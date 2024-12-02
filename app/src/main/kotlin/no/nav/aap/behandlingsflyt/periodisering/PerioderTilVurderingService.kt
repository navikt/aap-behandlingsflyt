package no.nav.aap.behandlingsflyt.periodisering

import no.nav.aap.behandlingsflyt.flyt.utledType
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Årsak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.komponenter.tidslinje.JoinStyle
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekst
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.ÅrsakTilBehandling

class PerioderTilVurderingService(
    private val sakService: SakService, private val behandlingRepository: BehandlingRepository
) {

    fun utled(kontekst: FlytKontekst, stegType: StegType): Set<Vurdering> {
        val sak = sakService.hent(kontekst.sakId)
        if (kontekst.behandlingType == TypeBehandling.Førstegangsbehandling) {
            // ved førstegangsbehandling skal hele perioden alltid vurderes for alle vilkår?

            return setOf(
                Vurdering(
                    type = VurderingType.FØRSTEGANGSBEHANDLING,
                    årsaker = listOf(ÅrsakTilBehandling.MOTTATT_SØKNAD),
                    periode = sak.rettighetsperiode
                )
            )
        }

        // TODO(" Sjekk vilkår/steg mot årsaker til vurdering (ligger på behandling)")
        // Skal regne ut gitt hva som har skjedd på en behandling og hvilke perioder som er relevant at vi vurderer
        val behandling = behandlingRepository.hent(kontekst.behandlingId)
        val flyt = utledType(behandling.typeBehandling()).flyt()
        val årsakerRelevantForSteg = flyt.årsakerRelevantForSteg(stegType)
        val årsaker = behandling.årsaker()

        val relevanteÅrsak = årsaker.filter { årsak -> årsakerRelevantForSteg.contains(årsak.type) }

        var tidslinje = Tidslinje<VurderingValue>()
        relevanteÅrsak.map { årsak -> utledVurdering(årsak, sak.rettighetsperiode) }.map {
            Tidslinje(
                it.periode,
                VurderingValue(it.type, it.årsaker)
            )
        }.forEach { segment ->
            tidslinje = tidslinje.kombiner(segment, JoinStyle.OUTER_JOIN { periode, venstreSegment, høyreSegment ->
                val venstreVerdi = venstreSegment?.verdi
                val høyreVerdi = høyreSegment?.verdi

                if (venstreVerdi != null && høyreVerdi != null) {
                    val prioritertVerdi = velgPrioritertVerdi(venstreVerdi, høyreVerdi)
                    Segment(periode, prioritertVerdi)
                } else if (venstreVerdi != null) {
                    Segment(periode, venstreVerdi)
                } else if (høyreVerdi != null) {
                    Segment(periode, høyreVerdi)
                } else {
                    null
                }
            })
        }

        return tidslinje.komprimer().segmenter()
            .map { Vurdering(type = it.verdi.type, årsaker = it.verdi.årsaker, periode = it.periode) }.toSet()
    }

    private fun velgPrioritertVerdi(venstreVerdi: VurderingValue, høyreVerdi: VurderingValue): VurderingValue {
        val årsaker = (venstreVerdi.årsaker + høyreVerdi.årsaker).toSet()
        val typer = setOf(venstreVerdi.type, høyreVerdi.type)
        if (typer.size == 1) {
            return venstreVerdi
        }
        if (typer.contains(VurderingType.FØRSTEGANGSBEHANDLING)) {
            return VurderingValue(VurderingType.FØRSTEGANGSBEHANDLING, årsaker.toList())
        } else if (typer.contains(VurderingType.REVURDERING)) {
            return VurderingValue(VurderingType.REVURDERING, årsaker.toList())
        }
        return return VurderingValue(typer.first(), årsaker.toList())
    }

    private fun utledVurdering(årsak: Årsak, rettighetsperiode: Periode): Vurdering {
        return when (årsak.type) {
            ÅrsakTilBehandling.MOTTATT_SØKNAD -> Vurdering(
                VurderingType.FØRSTEGANGSBEHANDLING,
                listOf(årsak.type),
                årsak.periode ?: rettighetsperiode
            )

            ÅrsakTilBehandling.MOTTATT_AKTIVITETSMELDING -> Vurdering(
                VurderingType.REVURDERING,
                listOf(årsak.type),
                requireNotNull(årsak.periode)
            )

            ÅrsakTilBehandling.MOTTATT_MELDEKORT -> Vurdering(
                VurderingType.FORLENGELSE,
                listOf(årsak.type),
                requireNotNull(årsak.periode)
            ) // TODO: Vurdere om denne skal utlede mer komplekst (dvs har mottatt for denne perioden før)

            ÅrsakTilBehandling.MOTTATT_LEGEERKLÆRING -> Vurdering(
                VurderingType.REVURDERING,
                listOf(årsak.type),
                rettighetsperiode
            )

            ÅrsakTilBehandling.MOTTATT_AVVIST_LEGEERKLÆRING -> Vurdering(
                VurderingType.REVURDERING,
                listOf(årsak.type),
                rettighetsperiode
            )

            ÅrsakTilBehandling.MOTTATT_DIALOGMELDING -> Vurdering(
                VurderingType.REVURDERING,
                listOf(årsak.type),
                rettighetsperiode
            )

            ÅrsakTilBehandling.G_REGULERING -> Vurdering(
                VurderingType.REVURDERING,
                listOf(årsak.type),
                requireNotNull(årsak.periode)
            )
        }
    }
}