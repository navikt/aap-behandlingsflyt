package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.flate.SykdomsvurderingDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.flate.YrkesskadevurderingDto
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.verdityper.dokument.JournalpostId
import java.time.LocalDate

class Sykdomsvurdering(
    val begrunnelse: String,
    val dokumenterBruktIVurdering: List<JournalpostId>,
    val harSkadeSykdomEllerLyte: Boolean,
    val erSkadeSykdomEllerLyteVesentligdel: Boolean?,
    val erNedsettelseIArbeidsevneHøyereEnnNedreGrense: Boolean?,
    val nedreGrense: NedreGrense?,
    val nedsattArbeidsevneDato: LocalDate?,
    val erArbeidsevnenNedsatt: Boolean?
) {
    fun toDto(yrkesskadevurdering: Yrkesskadevurdering?) = SykdomsvurderingDto(
            begrunnelse,
            dokumenterBruktIVurdering,
            erArbeidsevnenNedsatt,
            harSkadeSykdomEllerLyte,
            erSkadeSykdomEllerLyteVesentligdel,
            erNedsettelseIArbeidsevneHøyereEnnNedreGrense,
            nedreGrense,
            nedsattArbeidsevneDato,
            mapYrkesskade(yrkesskadevurdering)
        )

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

class Yrkesskadevurdering(
    val begrunnelse: String,
    val erÅrsakssammenheng: Boolean,
    val skadetidspunkt: LocalDate?,
    val andelAvNedsettelse: Prosent?
)
