package no.nav.aap.behandlingsflyt.periodisering

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.flyt.utledType
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekst
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.ÅrsakTilBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.komponenter.tidslinje.StandardSammenslåere
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import org.slf4j.LoggerFactory

class PerioderTilVurderingService(
    private val sakService: SakService,
    private val behandlingRepository: BehandlingRepository,
    private val vilkårsresultatRepository: VilkårsresultatRepository
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun utled(kontekst: FlytKontekst, stegType: StegType): VurderingTilBehandling {
        val sak = sakService.hent(kontekst.sakId)
        val behandling = behandlingRepository.hent(kontekst.behandlingId)

        if (kontekst.behandlingType == TypeBehandling.Førstegangsbehandling) {
            return VurderingTilBehandling(
                vurderingType = VurderingType.FØRSTEGANGSBEHANDLING,
                rettighetsperiode = sak.rettighetsperiode,
                årsakerTilBehandling = behandling.årsaker().map { it.type }.toSet()
            )
        }
        val flyt = utledType(behandling.typeBehandling()).flyt()
        val årsakerRelevantForSteg = flyt.årsakerRelevantForSteg(stegType)

        val årsaker = behandling.årsaker()
        val forlengelseÅrsaker =
            årsaker.map { årsak -> årsak.type }.filter { årsakTilType(it) == VurderingType.FORLENGELSE }.toSet()

        val relevanteÅrsak =
            årsaker.map { årsak -> årsak.type }.filter { årsak -> årsakerRelevantForSteg.contains(årsak) }.toSet()

        if (relevanteÅrsak.isEmpty() && forlengelseÅrsaker.isEmpty()) {
            return VurderingTilBehandling(
                vurderingType = VurderingType.IKKE_RELEVANT,
                rettighetsperiode = sak.rettighetsperiode,
                årsakerTilBehandling = relevanteÅrsak
            )
        } else if (relevanteÅrsak.isEmpty()) {
            // Ved forlengense så vil ikke nødvendigvis årsaken stå som relevant, men steget må forlenge vilkårsperioden i steget eventuelt
            return VurderingTilBehandling(
                vurderingType = VurderingType.FORLENGELSE,
                rettighetsperiode = sak.rettighetsperiode,
                forlengelsePeriode = utledForlengelsePeriode(
                    VurderingType.FORLENGELSE,
                    behandling.forrigeBehandlingId,
                    sak.rettighetsperiode
                ),
                årsakerTilBehandling = forlengelseÅrsaker
            )
        }

        // Steget har relevante årsaker og skal prioritere disse
        val vurderingstype = prioritertType(relevanteÅrsak.map { årsakTilType(it) }.toSet())

        return VurderingTilBehandling(
            vurderingType = vurderingstype,
            rettighetsperiode = sak.rettighetsperiode,
            forlengelsePeriode = utledForlengelsePeriode(
                vurderingstype,
                behandling.forrigeBehandlingId,
                sak.rettighetsperiode
            ),
            årsakerTilBehandling = relevanteÅrsak
        )
    }

    private fun utledForlengelsePeriode(
        vurderingstype: VurderingType,
        forrigeBehandlingId: BehandlingId?,
        rettighetsperiode: Periode
    ): Periode? {
        if (vurderingstype == VurderingType.FORLENGELSE) {
            val forrigeRettighetsperiode =
                vilkårsresultatRepository.hent(requireNotNull(forrigeBehandlingId)).finnVilkår(
                    Vilkårtype.ALDERSVILKÅRET
                ).tidslinje().helePerioden()
            val nyTidslinje = Tidslinje(rettighetsperiode, true)
            val forlengelsesTidslinje = nyTidslinje.kombiner(
                Tidslinje(forrigeRettighetsperiode, true),
                StandardSammenslåere.minus()
            )
            log.info("$rettighetsperiode - $forrigeRettighetsperiode ==> $forlengelsesTidslinje")
            if (forlengelsesTidslinje.isEmpty()) {
                // Er egentlig ikke noe å forlenge, men skal behandles som det
                return Periode(rettighetsperiode.tom, rettighetsperiode.tom)
            }
            val nyPeriode = forlengelsesTidslinje.helePerioden()

            return nyPeriode
        }
        return null
    }

    private fun prioritertType(vurderingTyper: Set<VurderingType>): VurderingType {
        if (vurderingTyper.isEmpty()) {
            throw IllegalStateException("Forventer minst en relevant årsak")
        }

        if (vurderingTyper.contains(VurderingType.FØRSTEGANGSBEHANDLING)) {
            return VurderingType.FØRSTEGANGSBEHANDLING
        } else if (vurderingTyper.contains(VurderingType.REVURDERING)) {
            return VurderingType.REVURDERING
        } else if (vurderingTyper.contains(VurderingType.FORLENGELSE)) {
            return VurderingType.FORLENGELSE
        }
        return VurderingType.IKKE_RELEVANT
    }


    private fun årsakTilType(årsak: ÅrsakTilBehandling): VurderingType {
        return when (årsak) {
            ÅrsakTilBehandling.MOTTATT_SØKNAD -> VurderingType.FØRSTEGANGSBEHANDLING
            ÅrsakTilBehandling.MOTTATT_AKTIVITETSMELDING -> VurderingType.REVURDERING
            ÅrsakTilBehandling.MOTTATT_MELDEKORT -> VurderingType.FORLENGELSE
            ÅrsakTilBehandling.MOTTATT_LEGEERKLÆRING -> VurderingType.REVURDERING
            ÅrsakTilBehandling.MOTTATT_AVVIST_LEGEERKLÆRING -> VurderingType.REVURDERING
            ÅrsakTilBehandling.MOTTATT_DIALOGMELDING -> VurderingType.REVURDERING
            ÅrsakTilBehandling.G_REGULERING -> VurderingType.REVURDERING
            ÅrsakTilBehandling.REVURDER_MEDLEMSKAP -> VurderingType.REVURDERING
            ÅrsakTilBehandling.REVURDER_BEREGNING -> VurderingType.REVURDERING
            ÅrsakTilBehandling.REVURDER_YRKESSKADE -> VurderingType.REVURDERING
            ÅrsakTilBehandling.REVURDER_LOVVALG -> VurderingType.REVURDERING
            ÅrsakTilBehandling.REVURDER_SAMORDNING -> VurderingType.REVURDERING
            ÅrsakTilBehandling.MOTATT_KLAGE -> VurderingType.IKKE_RELEVANT // TODO: Verifiser at dette er korrekt. 
        }
    }
}
