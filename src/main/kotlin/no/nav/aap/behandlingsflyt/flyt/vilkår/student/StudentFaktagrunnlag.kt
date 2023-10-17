
package no.nav.aap.behandlingsflyt.flyt.vilkår.student

import no.nav.aap.behandlingsflyt.avklaringsbehov.student.Studentvurdering
import no.nav.aap.behandlingsflyt.flyt.vilkår.Faktagrunnlag
import java.time.LocalDate

class StudentFaktagrunnlag(
    val vurderingsdato: LocalDate,
    val studentvurdering: Studentvurdering,
    val sisteDagMedMuligYtelse: LocalDate
) :
    Faktagrunnlag