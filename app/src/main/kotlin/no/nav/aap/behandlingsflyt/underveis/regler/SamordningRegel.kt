package no.nav.aap.behandlingsflyt.underveis.regler

import no.nav.aap.tidslinje.Tidslinje

/**
 * Legger på perioder hvor bruker ikke har rett fordi hen mottar andre ytelser eller avkorter mot andre ytelser
 *
 * - Håndterer samordning mot andre ytelser
 *    - Avkorting
 *    - Prioritering mot andre fulle ytelser
 *
 */
class SamordningRegel : UnderveisRegel {
    override fun vurder(input: UnderveisInput, resultat: Tidslinje<Vurdering>): Tidslinje<Vurdering> {
        // TODO
        return resultat
    }
}