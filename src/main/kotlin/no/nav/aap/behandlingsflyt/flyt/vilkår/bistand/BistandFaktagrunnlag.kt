package no.nav.aap.behandlingsflyt.flyt.vilkår.bistand

import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.bistand.BistandVurdering
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.student.StudentVurdering
import no.nav.aap.behandlingsflyt.flyt.vilkår.Faktagrunnlag
import java.time.LocalDate

class BistandFaktagrunnlag(
    val vurderingsdato: LocalDate,
    val sisteDagMedMuligYtelse: LocalDate,
    val vurdering: BistandVurdering?,
    val studentvurdering: StudentVurdering?,
) : Faktagrunnlag
