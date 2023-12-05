package no.nav.aap.behandlingsflyt.underveis.regler

import no.nav.aap.behandlingsflyt.underveis.tidslinje.Tidslinje

/**
 * Legger på perioder hvor bruker ikke har rett fordi hen er et annet sted
 *
 * - Utland
 * - Institusjon
 *
 */
class EtAnnetStedRegel : UnderveisRegel {
    override fun vurder(input: UnderveisInput, resultat: Tidslinje<Vurdering>) {
        TODO("Not yet implemented")
    }
}