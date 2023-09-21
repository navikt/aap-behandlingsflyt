package no.nav.aap.avklaringsbehov.sykdom

import no.nav.aap.domene.behandling.dokumenter.JournalpostId
import java.time.LocalDate

data class Sykdomsvurdering(
    val begrunnelse: String,
    val dokumenterBruktIVurdering: List<JournalpostId>,
    val erSkadeSykdomEllerLyteVesentligdel: Boolean,
    val erNedsettelseIArbeidsevneHøyereEnnNedreGrense: Boolean?,
    val nedreGrense: NedreGrense?,
    val skadetidspunkt: LocalDate?
)

enum class NedreGrense {
    TRETTI, FEMTI
}

data class Yrkesskadevurdering(
    val begrunnelse: String,
    val dokumenterBruktIVurdering: List<JournalpostId>,
    val erÅrsakssammenheng: Boolean,
    val skadetidspunkt: LocalDate?
)