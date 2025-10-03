package no.nav.aap.behandlingsflyt.behandling.tilkjentytelse

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import java.time.LocalDate

class VirkningstidspunktUtleder(
    private val vilkårsresultatRepository: VilkårsresultatRepository,
    ) {
    fun utledVirkningsTidspunkt(behandlingId: BehandlingId): LocalDate? {
        val vilkårsResultat = vilkårsresultatRepository.hent(behandlingId)
        
        return vilkårsResultat.rettighetstypeTidslinje().segmenter().firstOrNull()?.periode?.fom
    }
}