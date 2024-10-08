package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon

import no.nav.aap.komponenter.type.Periode
import no.nav.aap.verdityper.dokument.JournalpostId
import java.time.LocalDate

data class Soningsvurdering(
    val dokumenterBruktIVurdering: List<JournalpostId>,
    val soningUtenforFengsel: Boolean,
    val begrunnelse: String? = null,
    val arbeidUtenforAnstalt: Boolean? = null,
    val førsteArbeidsdag: LocalDate? = null,
    val periode: Periode
)
