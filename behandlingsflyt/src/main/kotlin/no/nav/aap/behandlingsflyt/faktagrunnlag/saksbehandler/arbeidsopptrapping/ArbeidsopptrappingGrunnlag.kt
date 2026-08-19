package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsopptrapping

import no.nav.aap.behandlingsflyt.behandling.vilkår.Varighetsvurdering
import no.nav.aap.behandlingsflyt.behandling.vilkår.mapMedDatoTilDatoVarighet
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.gjeldendeVurderinger
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.filterNotNull
import no.nav.aap.komponenter.tidslinje.orEmpty
import no.nav.aap.komponenter.type.Periode

data class ArbeidsopptrappingGrunnlag(
    val vurderinger: List<ArbeidsopptrappingVurdering>
) {
    fun gjeldendeVurderinger(): Tidslinje<ArbeidsopptrappingVurdering> {
        return vurderinger.gjeldendeVurderinger()
    }
}

fun ArbeidsopptrappingGrunnlag?.gjeldendeVurderinger() = this?.gjeldendeVurderinger().orEmpty()

fun ArbeidsopptrappingGrunnlag?.perioderMedArbeidsopptrapping(): List<Periode> {
    val gjeldendeVurderinger = this.gjeldendeVurderinger()

    return gjeldendeVurderinger.mapMedDatoTilDatoVarighet(
        harBegrensetVarighet = { it.rettPaaAAPIOpptrapping && it.reellMulighetTilOpptrapping },
        varighet = {
            /* Vilkåret har en begrensning på maks 12 måneder.
             */
            it.plusYears(1).minusDays(1)
        },
        body = { varighet, vurdering ->
            when {
                !vurdering.rettPaaAAPIOpptrapping || !vurdering.reellMulighetTilOpptrapping -> null
                varighet == Varighetsvurdering.VARIGHET_OK -> true
                varighet == Varighetsvurdering.VARIGHET_OVERSKREDET -> null
                else -> null
            }
        }
    ).filterNotNull().komprimer().perioder().toList()
}