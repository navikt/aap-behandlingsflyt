package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.behandlingsflyt.behandling.oppholdskrav.OppholdskravGrunnlag
import no.nav.aap.behandlingsflyt.behandling.oppholdskrav.OppholdskravTidslinjeData
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode

/**
 * Vurder om medlemmet oppfyller oppholdskravet. Implementasjon av:
 * - [Folketrygdloven § 11-3](https://lovdata.no/lov/1997-02-28-19/§11-3)
 */
class OppholdskravRegel : UnderveisRegel {
    override fun vurder(input: UnderveisInput, resultat: Tidslinje<Vurdering>): Tidslinje<Vurdering> {
        return resultat.leggTilVurderinger(input.oppholdskravGrunnlag.tilUnderveisTidslinje(input.rettighetsperiode), Vurdering::leggTilOppholdskravVurdering)
    }
}

fun OppholdskravGrunnlag.tilUnderveisTidslinje(rettighetsperiode: Periode): Tidslinje<OppholdskravUnderveisVurdering> =
        this.tidslinje()
            .filter { !it.verdi.oppfylt }
            .map { OppholdskravUnderveisVurdering(it) }
            .begrensetTil(rettighetsperiode)


class OppholdskravUnderveisVurdering(
    val vurdering: OppholdskravTidslinjeData,
) {
    val vilkårsvurdering: Vilkårsvurdering
        get() = Vilkårsvurdering.BRUDD_OPPHOLDSKRAV_11_3_STANS


    enum class Vilkårsvurdering {
        BRUDD_OPPHOLDSKRAV_11_3_STANS,
        BRUDD_OPPHOLDSKRAV_11_3_OPPHØR;
    }
}