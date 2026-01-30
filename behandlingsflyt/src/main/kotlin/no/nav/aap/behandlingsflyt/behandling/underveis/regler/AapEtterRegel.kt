package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.behandlingsflyt.behandling.rettighetstype.vurderRettighetstypeOgKvoter
import no.nav.aap.komponenter.tidslinje.Tidslinje

class AapEtterRegel : UnderveisRegel {
    override fun vurder(input: UnderveisInput, resultat: Tidslinje<Vurdering>): Tidslinje<Vurdering> {
        if (input.forenkletKvoteFeature) {
            val kvoteOgRettighetstype = vurderRettighetstypeOgKvoter(input.vilkårsresultat, input.kvoter)

            return resultat
                .leggTilVurderinger(kvoteOgRettighetstype.mapNotNull { it.rettighetsType }, Vurdering::leggTilRettighetstype)
                .leggTilVurderinger(kvoteOgRettighetstype, Vurdering::leggTilVarighetVurdering)
        } else {
            val rettighetstypeTidslinje = input.vilkårsresultat.rettighetstypeTidslinje()
            return resultat.leggTilVurderinger(rettighetstypeTidslinje, Vurdering::leggTilRettighetstype)
        }
    }
}
