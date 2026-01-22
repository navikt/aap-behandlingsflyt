package no.nav.aap.behandlingsflyt.behandling.vilkår.student

import no.nav.aap.behandlingsflyt.faktagrunnlag.Faktagrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentGrunnlag
import no.nav.aap.komponenter.type.Periode

class StudentFaktagrunnlag(
    val rettighetsperiode: Periode,
    val studentGrunnlag: StudentGrunnlag?,
) : Faktagrunnlag