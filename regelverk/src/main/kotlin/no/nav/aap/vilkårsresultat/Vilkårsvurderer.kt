package no.nav.aap.vilkårsresultat

import no.nav.aap.misc.Faktagrunnlag
import no.nav.aap.komponenter.tidslinje.Tidslinje

interface Vilkårsvurderer<F: Faktagrunnlag> {
    val vilkårtype: Vilkårtype

    /** Vurder vilkåret på bakgrunn av [faktagrunnlag] .
     *
     * Det er viktig at funksjonen [vurder] er 100% definert fra input, slik at
     * vi kan kjøre koden på nytt i etterkant (basert på git-sha og faktagrunnlaget
     * som blir lagret som json).
     *
     * Det betyr at feature toggles må sjekkes på utsiden av [vurder] og legges
     * inn som en verdi i [faktagrunnlag].
     *
     * @return den nye vurderingen av vilkåret – perioder som ikke vurderes "slettes" fra vilkårsresultatet
     */
    fun vurder(faktagrunnlag: F): Tidslinje<Vilkårsvurdering>
}