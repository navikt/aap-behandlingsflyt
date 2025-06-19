package no.nav.aap.behandlingsflyt.behandling.student

import com.papsign.ktor.openapigen.annotations.Response

@Response(statusCode = 204)
class NoneStudentGrunnlagResponse() :
    StudentGrunnlagResponse(false,null, null)
