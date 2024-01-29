package no.nav.aap.behandlingsflyt.vilkår.sykdom

import no.nav.aap.behandlingsflyt.faktagrunnlag.student.StudentVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.sykdom.Sykdomsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.sykdom.Yrkesskadevurdering
import no.nav.aap.behandlingsflyt.vilkår.Faktagrunnlag
import java.time.LocalDate

class SykdomsFaktagrunnlag(
    val vurderingsdato: LocalDate,
    val sisteDagMedMuligYtelse: LocalDate,
    val yrkesskadevurdering: Yrkesskadevurdering?,
    val sykdomsvurdering: Sykdomsvurdering?,
    val studentvurdering: StudentVurdering?
) : Faktagrunnlag
