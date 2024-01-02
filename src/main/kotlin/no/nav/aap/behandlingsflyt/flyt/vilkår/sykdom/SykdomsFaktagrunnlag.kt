package no.nav.aap.behandlingsflyt.flyt.vilkår.sykdom

import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.student.StudentVurdering
import no.nav.aap.behandlingsflyt.avklaringsbehov.sykdom.Sykdomsvurdering
import no.nav.aap.behandlingsflyt.avklaringsbehov.sykdom.Yrkesskadevurdering
import no.nav.aap.behandlingsflyt.flyt.vilkår.Faktagrunnlag
import java.time.LocalDate

class SykdomsFaktagrunnlag(
    val vurderingsdato: LocalDate,
    val sisteDagMedMuligYtelse: LocalDate,
    val yrkesskadevurdering: Yrkesskadevurdering?,
    val sykdomsvurdering: Sykdomsvurdering?,
    val studentvurdering: StudentVurdering?
) : Faktagrunnlag
