package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsevne

import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.somTidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid
import java.time.LocalDate

data class ArbeidsevneGrunnlag(
    val vurderinger: List<ArbeidsevneVurdering>,
)

fun List<ArbeidsevneVurdering>.tilTidslinje(maksDato: LocalDate = Tid.MAKS): Tidslinje<ArbeidsevneVurdering> =
    groupBy { it.vurdertIBehandling }
        .values
        .sortedBy { it[0].opprettetTid }
        .flatMap { vurderingerForBehandling -> vurderingerForBehandling.sortedBy { it.fraDato } }
        .somTidslinje { Periode(it.fraDato, it.tilDato ?: maksDato) }
        .komprimer()
