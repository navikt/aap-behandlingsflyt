package no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.Soningsvurdering
import no.nav.aap.komponenter.tidslinje.StandardSammenslåere
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid
import java.time.LocalDateTime

data class Soningsvurderinger(
    val id: Long? = null,
    val vurderinger: List<Soningsvurdering>,
    val vurdertAv: String,
    val vurdertTidspunkt: LocalDateTime
) {
    fun tilTidslinje(): Tidslinje<Soningsvurdering> =
        vurderinger
            .sortedBy { it.fraDato }
            .map { Tidslinje(Periode(it.fraDato, Tid.MAKS), it) }
            .fold(Tidslinje<Soningsvurdering>()) { acc, tidslinje ->
                acc.kombiner(tidslinje, StandardSammenslåere.prioriterHøyreSideCrossJoin())
            }
}