package no.nav.aap.behandlingsflyt.steg.underveis.regler

import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.underveis.Vurdering

interface UnderveisRegel {
    fun vurder(input: UnderveisInput, resultat: Tidslinje<Vurdering>): Tidslinje<Vurdering>
}

