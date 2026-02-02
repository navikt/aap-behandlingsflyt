package no.nav.aap.behandlingsflyt.help

import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.komponenter.type.Periode
import java.time.LocalDate

class FlytKontekstMedPerioderBuilder {
    var sakId: SakId? = SakId(1L)
    var behandlingId: BehandlingId? = BehandlingId(1L)
    var behandlingType: TypeBehandling = TypeBehandling.Revurdering
    var forrigeBehandlingId: BehandlingId? = null
    var vurderingType: VurderingType? = null
    var rettighetsperiode: Periode = Periode(LocalDate.now().minusDays(1), LocalDate.now().plusYears(1))
    var vurderingsbehovRelevanteForSteg: Set<Vurderingsbehov> = setOf(Vurderingsbehov.MOTTATT_MELDEKORT)
    var vurderingsbehovRelevanteForStegMedPerioder: Set<VurderingsbehovMedPeriode>? = null

    var behandling: Behandling? = null

    fun build(): FlytKontekstMedPerioder {
        val typeBehandling = behandling?.typeBehandling() ?: behandlingType
        val vurderingType = vurderingType
            ?: when (typeBehandling) {
                TypeBehandling.Førstegangsbehandling -> VurderingType.FØRSTEGANGSBEHANDLING
                TypeBehandling.Revurdering -> VurderingType.REVURDERING
                else -> VurderingType.IKKE_RELEVANT
            }
        if (behandling != null) {
            val behandling1 = behandling!!
            return FlytKontekstMedPerioder(
                sakId = behandling1.sakId,
                behandlingId = behandling1.id,
                forrigeBehandlingId = behandling1.forrigeBehandlingId,
                behandlingType = behandling1.typeBehandling(),
                vurderingType = vurderingType,
                rettighetsperiode = rettighetsperiode,
                vurderingsbehovRelevanteForSteg = vurderingsbehovRelevanteForSteg,
                vurderingsbehovRelevanteForStegMedPerioder = vurderingsbehovRelevanteForStegMedPerioder
                    ?: vurderingsbehovRelevanteForSteg.map { VurderingsbehovMedPeriode(it) }.toSet()
            )
        }
        return FlytKontekstMedPerioder(
            sakId = sakId!!,
            behandlingId = behandlingId!!,
            behandlingType = behandlingType,
            forrigeBehandlingId = forrigeBehandlingId,
            vurderingType = vurderingType,
            rettighetsperiode = rettighetsperiode,
            vurderingsbehovRelevanteForSteg = vurderingsbehovRelevanteForSteg,
            vurderingsbehovRelevanteForStegMedPerioder = vurderingsbehovRelevanteForStegMedPerioder
                ?: vurderingsbehovRelevanteForSteg.map { VurderingsbehovMedPeriode(it) }.toSet()
        )
    }
}

fun flytKontekstMedPerioder(init: FlytKontekstMedPerioderBuilder.() -> Unit): FlytKontekstMedPerioder =
    FlytKontekstMedPerioderBuilder().apply(init).build()