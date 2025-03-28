package no.nav.aap.behandlingsflyt.behandling.tilkjentytelse

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import java.time.LocalDate

class VirkningstidspunktUtleder(
    private val vilkårsresultatRepository: VilkårsresultatRepository,

) {
    fun utledVirkningsTidspunkt(behandlingId: BehandlingId): LocalDate? {
        val vilkårsResultat = vilkårsresultatRepository.hent(behandlingId)
        val bistandsvilkåret = vilkårsResultat.finnVilkår(Vilkårtype.BISTANDSVILKÅRET)
        if (!bistandsvilkåret.harPerioderSomErOppfylt()) return null
        return vilkårsResultat.rettighetstypeTidslinje().firstOrNull()?.periode?.fom
    }
}