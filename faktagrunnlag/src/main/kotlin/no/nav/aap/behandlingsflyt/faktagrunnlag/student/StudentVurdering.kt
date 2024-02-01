
package no.nav.aap.behandlingsflyt.faktagrunnlag.student

import no.nav.aap.verdityper.dokument.JournalpostId
import java.time.LocalDate

data class StudentVurdering(
    val begrunnelse: String,
    val dokumenterBruktIVurdering: List<JournalpostId>,
    val oppfyller11_14: Boolean?,
    val avbruttStudieDato: LocalDate?
)
