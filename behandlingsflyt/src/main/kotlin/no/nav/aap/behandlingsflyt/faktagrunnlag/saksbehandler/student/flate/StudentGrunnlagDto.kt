package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.flate

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.OppgittStudent
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentVurdering

open class StudentGrunnlagDto(
    val harTilgangTilÅSaksbehandle: Boolean,
    val studentvurdering: StudentVurdering?,
    val oppgittStudent: OppgittStudent?
)
