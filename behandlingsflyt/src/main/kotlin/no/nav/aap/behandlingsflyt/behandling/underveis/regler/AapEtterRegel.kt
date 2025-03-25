package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.komponenter.tidslinje.Tidslinje

class AapEtterRegel : UnderveisRegel {
    override fun vurder(input: UnderveisInput, resultat: Tidslinje<Vurdering>): Tidslinje<Vurdering> {
        val rettighetstypeTidslinje = input.vilkårsresultat.rettighetstypeTidslinje()
        return resultat.leggTilVurderinger(rettighetstypeTidslinje, Vurdering::leggTilRettighetstype)
    }
}
