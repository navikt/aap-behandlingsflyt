package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.flate

import com.papsign.ktor.openapigen.annotations.Response

@Response(statusCode = 204)
class NoneStudentGrunnlagResponse() :
    StudentGrunnlagResponse(false,null, null)
