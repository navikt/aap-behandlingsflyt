package no.nav.aap.behandlingsflyt.underveis.regler

import no.nav.aap.behandlingsflyt.tidslinje.Tidslinje

interface UnderveisRegel {

    fun vurder(input: UnderveisInput, resultat: Tidslinje<Vurdering>): Tidslinje<Vurdering>
}