package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.flate

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykepengerGrunn
import no.nav.aap.verdityper.dokument.JournalpostId
import java.time.LocalDate

data class SykepengerVurderingDto(
    val begrunnelse: String,
    val dokumenterBruktIVurdering: List<JournalpostId>,
    val harRettPå: Boolean,
    val grunn: SykepengerGrunn? = null,
    val gjelderFra: LocalDate? = null,
)