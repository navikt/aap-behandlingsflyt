package no.nav.aap.etterlevelse.sporing

import no.nav.aap.behandlingsflyt.utils.Diff
import no.nav.aap.komponenter.tidslinje.Tidslinje

/** Sporing av vilkårlig, periodisert kode.
 *
 * Sporingen tar vare på git-sha og en json-variant av input, slik at
 * utviklere kan finne tilbake til nøyaktig kode og nøyaktig input som
 * ble brukt i en beregning.
 **/
interface Sporbar<Input, Resultat> {
    /** Unik nøkkel som skiller forskjellig sporbar kode fra hverandre. */
    val sporingsnøkkel: Sporingsnøkkel

    /** Koden som skal spores. For at dette gir mening, så må koden være uten side-effekter.
     * Spesielt viktig at feature-toggles hentes før koden kjører og at feature-toggelet sendes med som en del av [input]. Og motsatt
     * viktig at [kjørMedSporing] ikke kaller unleash-gateway, eller andre gateways eller repositories.
     **/
    fun kjørMedSporing(resultatForrigeBehandling: Tidslinje<Resultat>, input: Input): Tidslinje<Diff<Resultat>>
}
