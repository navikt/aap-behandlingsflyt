package no.nav.aap.behandlingsflyt.avklaringsbehov.løser.sykdom

import no.nav.aap.verdityper.dokument.JournalpostId
import no.nav.aap.verdityper.Beløp
import no.nav.aap.verdityper.Prosent
import java.time.LocalDate

data class Sykdomsvurdering(
    val begrunnelse: String,
    val dokumenterBruktIVurdering: List<JournalpostId>,
    val erSkadeSykdomEllerLyteVesentligdel: Boolean,
    val erNedsettelseIArbeidsevneHøyereEnnNedreGrense: Boolean?,
    val nedreGrense: NedreGrense?,
    val nedsattArbeidsevneDato: LocalDate?,
    val ytterligereNedsattArbeidsevneDato: LocalDate?
)

enum class NedreGrense {
    TRETTI, FEMTI
}

data class Yrkesskadevurdering(
    val begrunnelse: String,
    val dokumenterBruktIVurdering: List<JournalpostId>,
    val erÅrsakssammenheng: Boolean,
    val skadetidspunkt: LocalDate?,
    val andelAvNedsettelse: Prosent?,
    val antattÅrligInntekt: Beløp?
)
