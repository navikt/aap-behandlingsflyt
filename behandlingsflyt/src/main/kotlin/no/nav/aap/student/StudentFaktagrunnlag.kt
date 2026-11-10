package no.nav.aap.student

import no.nav.aap.behandlingsflyt.faktagrunnlag.Faktagrunnlag
import no.nav.aap.komponenter.type.Periode

class StudentFaktagrunnlag(
    val rettighetsperiode: Periode,
    val studentGrunnlag: StudentGrunnlag?,
) : Faktagrunnlag