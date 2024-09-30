package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.tidslinje.Tidslinje

class SoningRegel : UnderveisRegel {
    override fun vurder(input: UnderveisInput, resultat: Tidslinje<Vurdering>): Tidslinje<Vurdering> {
        // TODO
        /*
        *
        erpåsoning
            typesoning
                erpåfotlenke
                erbakmur
        *
        * */
        return resultat
    }
}