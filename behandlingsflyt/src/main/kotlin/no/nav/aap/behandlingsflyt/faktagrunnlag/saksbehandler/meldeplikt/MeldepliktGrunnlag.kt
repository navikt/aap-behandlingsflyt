package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.Fritaksvurdering.FritaksvurderingData
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.somTidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid
import java.time.LocalDate

data class MeldepliktGrunnlag(
    val vurderinger: List<Fritaksvurdering>
) {
    fun gjeldendeVurderinger(maksDato: LocalDate = Tid.MAKS): Tidslinje<Fritaksvurdering> =
        vurderinger
            .groupBy { it.vurdertIBehandling }
            .values
            .sortedBy { it[0].opprettetTid }
            .flatMap { vurderingerForBehandling -> vurderingerForBehandling.sortedBy { it.fraDato } }
            .somTidslinje { Periode(it.fraDato, it.tilDato ?: maksDato) }
            .komprimer()

    fun tilTidslinje(): Tidslinje<FritaksvurderingData> =
        gjeldendeVurderinger()
            .map { it.toFritaksvurderingData() }
            .komprimer()
}