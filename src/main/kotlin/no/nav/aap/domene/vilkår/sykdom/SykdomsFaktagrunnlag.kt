package no.nav.aap.domene.vilkår.sykdom

import no.nav.aap.avklaringsbehov.sykdom.Sykdomsvurdering
import no.nav.aap.avklaringsbehov.sykdom.Yrkesskadevurdering
import no.nav.aap.domene.behandling.Faktagrunnlag
import java.time.LocalDate

class SykdomsFaktagrunnlag(
    val vurderingsdato: LocalDate,
    val sisteDagMedMuligYtelse: LocalDate,
    val yrkesskadevurdering: Yrkesskadevurdering?,
    val sykdomsvurdering: Sykdomsvurdering
) :
    Faktagrunnlag {
}