package no.nav.aap.behandlingsflyt.behandling.brev

import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.TypeBrev
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsvurdering
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.komponenter.tidslinje.JoinStyle
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode

class BrevUtlederService(
    private val behandlingRepository: BehandlingRepository,
    private val vilkårsresultatRepository: VilkårsresultatRepository,
) {

    fun utledBehovForMeldingOmVedtak(behandlingId: BehandlingId): BrevBehov {
        val behandling = behandlingRepository.hent(behandlingId)

        if (behandling.typeBehandling() != TypeBehandling.Førstegangsbehandling) {
            // TODO: Sjekk på behandlingen og utled hva som har skjedd for å avgjøre om det skal sendes et brev
            return BrevBehov(null)
        }

        val vilkårsresultat = vilkårsresultatRepository.hent(behandlingId)

        val oppfyltePerioder = finnOppfyltePerioder(vilkårsresultat)

        return if (oppfyltePerioder.isNotEmpty()) {
            // FIX LOGIKK
            // felles logikk her for når en behandling er innvilget
            // ved avslag: trenger prioritering på vilkår
            BrevBehov(TypeBrev.VEDTAK_INNVILGELSE)
        } else {
            BrevBehov(TypeBrev.VEDTAK_AVSLAG)
        }
    }

    private fun finnOppfyltePerioder(vilkårsresultat: Vilkårsresultat): List<Periode> {
        return vilkårsresultat.alle().map { vilkår ->
            Tidslinje(vilkår.vilkårsperioder().map { Segment(it.periode, Vilkårsvurdering(it)) })
        }.fold(Tidslinje<Boolean>()) { resultatTidslinje, vilkårsvurderingTidslinje ->
            resultatTidslinje.kombiner(
                vilkårsvurderingTidslinje,
                JoinStyle.OUTER_JOIN { periode, erOppfylt, vilkårsvurdering ->
                    if (vilkårsvurdering == null) {
                        erOppfylt
                    } else {
                        Segment(
                            periode,
                            vilkårsvurdering.verdi.erOppfylt() && erOppfylt?.verdi != false
                        )
                    }
                })
        }.segmenter().filter { it.verdi }.map { it.periode }
    }
}
