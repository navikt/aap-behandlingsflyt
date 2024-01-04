package no.nav.aap.behandlingsflyt.underveis.regler

import no.nav.aap.behandlingsflyt.tidslinje.Tidslinje

/**
 * Aktivitetskravene
 *
 * - MP
 * - Fravær
 *   - Aktivitet
 *   - etc
 */
class AktivitetRegel : UnderveisRegel {
    override fun vurder(input: UnderveisInput, resultat: Tidslinje<Vurdering>): Tidslinje<Vurdering> {
        // TODO
        return resultat
    }
}