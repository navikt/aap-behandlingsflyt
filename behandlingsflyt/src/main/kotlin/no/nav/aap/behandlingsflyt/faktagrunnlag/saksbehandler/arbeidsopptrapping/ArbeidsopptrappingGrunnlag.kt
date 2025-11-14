package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsopptrapping

import no.nav.aap.komponenter.tidslinje.Tidslinje
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
    }
}