package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangarbeid

import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.somTidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid
import java.time.LocalDate

data class OvergangArbeidGrunnlag(
    val vurderinger: List<OvergangArbeidVurdering>,
) {
    fun gjeldendeVurderinger(maksDato: LocalDate = Tid.MAKS): Tidslinje<OvergangArbeidVurdering> {
        return vurderinger
            .groupBy { it.vurdertIBehandling }
            .values
            .sortedBy { it[0].opprettet }
            .flatMap { it.sortedBy { it.vurderingenGjelderFra } }
            .somTidslinje { Periode(it.vurderingenGjelderFra, it.vurderingenGjelderTil ?: maksDato) }
    }
}