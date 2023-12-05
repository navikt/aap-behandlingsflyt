package no.nav.aap.behandlingsflyt.underveis.regler

import no.nav.aap.behandlingsflyt.underveis.tidslinje.Tidslinje

/**
 * Graderer arbeid der hvor det ikke er avslått pga en regel tidliger i løpet
 *
 * - Arbeid fra meldeplikt
 */
class GraderingArbeidRegel : UnderveisRegel {
    override fun vurder(input: UnderveisInput, resultat: Tidslinje<Vurdering>) {
        TODO("Not yet implemented")
    }
}