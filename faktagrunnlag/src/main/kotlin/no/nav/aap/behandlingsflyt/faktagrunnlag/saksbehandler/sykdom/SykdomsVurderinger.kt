package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.flate.SykdomsvurderingDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.flate.YrkesskadevurderingDto
import no.nav.aap.verdityper.Prosent
import no.nav.aap.verdityper.dokument.JournalpostId
import java.time.LocalDate
import java.time.Year

data class Sykdomsvurdering(
    val begrunnelse: String,
    val dokumenterBruktIVurdering: List<JournalpostId>,
    val erSkadeSykdomEllerLyteVesentligdel: Boolean?,
    val erNedsettelseIArbeidsevneHøyereEnnNedreGrense: Boolean?,
    val nedreGrense: NedreGrense?,
    val nedsattArbeidsevneDato: Year?,
    val erArbeidsevnenNedsatt: Boolean
) {
    fun toDto(yrkesskadevurdering: Yrkesskadevurdering?): SykdomsvurderingDto? {
        return SykdomsvurderingDto(
            begrunnelse,
            dokumenterBruktIVurdering,
            erArbeidsevnenNedsatt,
            erSkadeSykdomEllerLyteVesentligdel,
            erNedsettelseIArbeidsevneHøyereEnnNedreGrense,
            nedreGrense,
            nedsattArbeidsevneDato?.atMonth(1)?.atDay(1),
            mapYrkesskade(yrkesskadevurdering)
        )
    }

    private fun mapYrkesskade(yrkesskadevurdering: Yrkesskadevurdering?): YrkesskadevurderingDto? {
        if (yrkesskadevurdering == null) {
            return null
        }
        return YrkesskadevurderingDto(yrkesskadevurdering.erÅrsakssammenheng)
    }
}

enum class NedreGrense {
    TRETTI, FEMTI
}

data class Yrkesskadevurdering(
    val begrunnelse: String,
    val erÅrsakssammenheng: Boolean,
    val skadetidspunkt: LocalDate?,
    val andelAvNedsettelse: Prosent?
)
