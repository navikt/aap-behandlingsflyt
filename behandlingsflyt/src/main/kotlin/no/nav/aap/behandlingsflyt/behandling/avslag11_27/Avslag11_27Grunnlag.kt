package no.nav.aap.behandlingsflyt.behandling.avslag11_27

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.KravGrunnlag
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.filterNotNull
import no.nav.aap.komponenter.tidslinje.orEmpty

class Avslag11_27Grunnlag(
    val vurderinger: List<Avslag11_27Vurdering>
) {
    fun nyesteVurderingPerKrav(): List<Avslag11_27Vurdering> {
        return vurderinger
            .groupBy { it.referanse }
            .values
            .map { vurderingerForKrav -> vurderingerForKrav.maxBy { it.vurdertTidspunkt } }
    }

    fun tilTidslinje(kravGrunnlag: KravGrunnlag?): Tidslinje<Avslag11_27Vurdering> {
        val nyesteVurderingPerKrav = nyesteVurderingPerKrav().associateBy { it.referanse }

        return kravGrunnlag?.kravtidslinje()
            ?.map { krav -> nyesteVurderingPerKrav[krav.referanse] }
            ?.komprimer()
            ?.filterNotNull()
            .orEmpty()
    }
}
