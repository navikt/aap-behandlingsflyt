package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.flate

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.OppgittStudent
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentVurderingResponse

open class StudentGrunnlagResponse(
    val harTilgangTilÅSaksbehandle: Boolean,
    val studentvurdering: StudentVurderingResponse?,
    val oppgittStudent: OppgittStudent?,
)