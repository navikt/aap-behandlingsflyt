package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.komponenter.tidslinje.StandardSammenslåere
import no.nav.aap.komponenter.tidslinje.Tidslinje

class AapEtterRegel : UnderveisRegel {
    /**
     * Siden denne er etter [no.nav.aap.behandlingsflyt.behandling.underveis.regler.RettTilRegel], er de relevante
     * vilkårene inkludert i [input].
     */
    override fun vurder(input: UnderveisInput, resultat: Tidslinje<Vurdering>): Tidslinje<Vurdering> {
        val kunMedOppfylteVilkår = resultat.filter { it.verdi.ingenVilkårErAvslått() }

        val tidslinje = input.vilkårsresultat.rettighetstypeTidslinje()

        val filtrerBortPerioderUtenOppfylteVilkår =
            kunMedOppfylteVilkår.kombiner(tidslinje, StandardSammenslåere.kunHøyre())
        return resultat.leggTilVurderinger(filtrerBortPerioderUtenOppfylteVilkår, Vurdering::leggTilRettighetstype)
    }
}
