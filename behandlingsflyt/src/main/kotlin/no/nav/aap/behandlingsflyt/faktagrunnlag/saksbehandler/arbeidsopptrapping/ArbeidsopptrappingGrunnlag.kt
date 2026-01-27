package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsopptrapping

import no.nav.aap.behandlingsflyt.behandling.vilkår.Varighetsvurdering
import no.nav.aap.behandlingsflyt.behandling.vilkår.mapMedDatoTilDatoVarighet
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.filterNotNull
import no.nav.aap.komponenter.tidslinje.orEmpty
import no.nav.aap.komponenter.tidslinje.somTidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid
import java.time.LocalDate

data class ArbeidsopptrappingGrunnlag(
    val vurderinger: List<ArbeidsopptrappingVurdering>
) {
    fun gjeldendeVurderinger(maksDato: LocalDate = Tid.MAKS): Tidslinje<ArbeidsopptrappingVurdering> {
        return vurderinger
            .groupBy { it.vurdertIBehandling }
            .values
            .sortedBy { it[0].opprettetTid }
            .flatMap { it.sortedBy { it.vurderingenGjelderFra } }
            .somTidslinje { Periode(it.vurderingenGjelderFra, it.vurderingenGjelderTil ?: maksDato) }
            .komprimer()
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