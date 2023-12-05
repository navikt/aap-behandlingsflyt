package no.nav.aap.behandlingsflyt.underveis.regler

import no.nav.aap.behandlingsflyt.flyt.vilkår.Utfall
import no.nav.aap.behandlingsflyt.underveis.tidslinje.Segment
import no.nav.aap.behandlingsflyt.underveis.tidslinje.Tidslinje

/**
 * Setter opp tidslinjen hvor bruker har grunnleggende rett til ytelsen
 *
 * - Alder (18 - 67)
 * - Perioder med ytelse (Sykdom, Bistand, Sykepengeerstatning, Student)
 * - Varigheten (11-12)
 *   - 3 år per "krav"
 *
 */
class RettTilRegel : UnderveisRegel {
    override fun vurder(input: UnderveisInput, resultat: Tidslinje<Vurdering>) {
        input.relevanteVilkår.forEach { vilkår ->
            val vilkårstidslinje = Tidslinje(vilkår.vilkårsperioder().map { Segment(it.periode, Vurdering(it.utfall == Utfall.OPPFYLT)) })
        }
    }
}