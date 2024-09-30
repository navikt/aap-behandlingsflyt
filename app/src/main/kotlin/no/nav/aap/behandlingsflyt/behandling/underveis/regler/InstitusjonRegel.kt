package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.tidslinje.Tidslinje

/**
 * - Hva med Utland?
 */
class InstitusjonRegel : UnderveisRegel {
    override fun vurder(input: UnderveisInput, resultat: Tidslinje<Vurdering>): Tidslinje<Vurdering> {
        // TODO

        /*
            erpåinstitusjon (50% reduksjon)
                forsørgerEktefelle (INGEN REDUKSJON )
                HAR DU BARNETILLEG FJERNES ALLE REDUKSJONER PÅ DENNE
        * */
        return resultat
    }
}