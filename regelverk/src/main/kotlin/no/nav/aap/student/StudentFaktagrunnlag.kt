package no.nav.aap.student

import no.nav.aap.misc.Faktagrunnlag
import no.nav.aap.komponenter.type.Periode

class StudentFaktagrunnlag(
    val rettighetsperiode: Periode,
    val studentGrunnlag: StudentGrunnlag?,
) : Faktagrunnlag