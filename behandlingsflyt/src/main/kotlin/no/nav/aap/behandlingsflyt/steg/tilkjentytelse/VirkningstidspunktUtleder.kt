package no.nav.aap.behandlingsflyt.steg.tilkjentytelse

import java.time.LocalDate
import no.nav.aap.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.faktagrunnlag.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.lookup.repository.RepositoryProvider

class VirkningstidspunktUtleder(
    private val vilkårsresultatRepository: VilkårsresultatRepository,
) {
    constructor(repositoryProvider: RepositoryProvider) : this(
        vilkårsresultatRepository = repositoryProvider.provide(),
    )

    fun utledVirkningsTidspunkt(behandlingId: BehandlingId): LocalDate? {
        val vilkårsResultat = vilkårsresultatRepository.hent(behandlingId)

        // Første periode med rett. Dette virker fordi rettighetstype-tidslinjen aldri har null-verdier.
        return vilkårsResultat.rettighetstypeTidslinje().segmenter().firstOrNull()?.periode?.fom
    }
}