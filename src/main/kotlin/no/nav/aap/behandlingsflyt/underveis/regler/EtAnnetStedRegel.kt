package no.nav.aap.behandlingsflyt.underveis.regler

import no.nav.aap.behandlingsflyt.tidslinje.Tidslinje

/**
 * Legger på perioder hvor bruker ikke har rett fordi hen er et annet sted
 *
 * - Utland
 * - Institusjon
 *
 */
class EtAnnetStedRegel : UnderveisRegel {
    override fun vurder(input: UnderveisInput, resultat: Tidslinje<Vurdering>): Tidslinje<Vurdering> {
        // TODO
        return resultat
    }
}