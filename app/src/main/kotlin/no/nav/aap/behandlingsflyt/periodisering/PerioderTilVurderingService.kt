package no.nav.aap.behandlingsflyt.periodisering

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.EndringType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Årsak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.tidslinje.JoinStyle
import no.nav.aap.tidslinje.Segment
import no.nav.aap.tidslinje.Tidslinje
import no.nav.aap.verdityper.Periode
import no.nav.aap.verdityper.flyt.FlytKontekst
import no.nav.aap.verdityper.flyt.StegType
import no.nav.aap.verdityper.flyt.Vurdering
import no.nav.aap.verdityper.flyt.VurderingType
import no.nav.aap.verdityper.sakogbehandling.TypeBehandling

class PerioderTilVurderingService(connection: DBConnection) {
    private val sakService: SakService = SakService(connection)
    private val behandlingRepository = BehandlingRepositoryImpl(connection)

    fun utled(kontekst: FlytKontekst, stegType: StegType): Set<Vurdering> {
        val sak = sakService.hent(kontekst.sakId)
        if (kontekst.behandlingType == TypeBehandling.Førstegangsbehandling) {
            // ved førstegangsbehandling skal hele perioden alltid vurderes for alle vilkår?

            return setOf(Vurdering(type = VurderingType.FØRSTEGANGSBEHANDLING, periode = sak.rettighetsperiode))
        }

        // TODO(" Sjekk vilkår/steg mot årsaker til vurdering (ligger på behandling)")
        // Skal regne ut gitt hva som har skjedd på en behandling og hvilke perioder som er relevant at vi vurderer
        val behandling = behandlingRepository.hent(kontekst.behandlingId)
        val årsaker = behandling.årsaker()

        var tidslinje = Tidslinje<VurderingType>()
        årsaker.map { årsak -> utledVurdering(årsak, sak.rettighetsperiode) }.map { Tidslinje(it.periode, it.type) }
            .forEach { segment ->
                tidslinje = tidslinje.kombiner(segment, JoinStyle.CROSS_JOIN { periode, venstreSegment, høyreSegment ->
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

        return tidslinje.segmenter().map { Vurdering(periode = it.periode, type = it.verdi) }.toSet()
    }

    private fun velgPrioritertVerdi(venstreVerdi: VurderingType, høyreVerdi: VurderingType): VurderingType {
        val typer = setOf(venstreVerdi, høyreVerdi)
        if (typer.size == 1) {
            return venstreVerdi
        }
        if (typer.contains(VurderingType.FØRSTEGANGSBEHANDLING)) {
            return VurderingType.FØRSTEGANGSBEHANDLING
        } else if (typer.contains(VurderingType.REVURDERING)) {
            return VurderingType.REVURDERING
        }
        return typer.first()
    }

    private fun utledVurdering(årsak: Årsak, rettighetsperiode: Periode): Vurdering {
        return when (årsak.type) {
            EndringType.MOTTATT_SØKNAD -> Vurdering(
                VurderingType.FØRSTEGANGSBEHANDLING,
                årsak.periode ?: rettighetsperiode
            )

            EndringType.MOTTATT_AKTIVITETSMELDING -> Vurdering(VurderingType.REVURDERING, requireNotNull(årsak.periode))
            EndringType.MOTTATT_MELDEKORT -> Vurdering(
                VurderingType.FORLENGELSE,
                requireNotNull(årsak.periode)
            ) // TODO: Vurdere om denne skal utlede mer komplekst (dvs har mottatt for denne perioden før)
            EndringType.MOTTATT_LEGEERKLÆRING -> Vurdering(VurderingType.FØRSTEGANGSBEHANDLING, rettighetsperiode)
        }
    }
}