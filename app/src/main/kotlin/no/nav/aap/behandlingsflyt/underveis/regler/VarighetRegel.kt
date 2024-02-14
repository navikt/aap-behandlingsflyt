package no.nav.aap.behandlingsflyt.underveis.regler

import no.nav.aap.tidslinje.Tidslinje

/**
 * Håndterer varighetsbestemmelsene (11-12 + unntak fra denne)
 *
 * - Varigheten på ordinær
 * - Unntak
 * - Dødsfall på bruker
 *
 */
class VarighetRegel : UnderveisRegel {
    override fun vurder(input: UnderveisInput, resultat: Tidslinje<Vurdering>): Tidslinje<Vurdering> {
        // TODO
        return resultat
    }
}