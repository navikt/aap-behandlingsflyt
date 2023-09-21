package no.nav.aap.domene.behandling.grunnlag.sykdom

import no.nav.aap.avklaringsbehov.sykdom.Sykdomsvurdering
import no.nav.aap.avklaringsbehov.sykdom.Yrkesskadevurdering

class SykdomsGrunnlag(
    val id: Long,
    val behandlingId: Long,
    val yrkesskadevurdering: Yrkesskadevurdering?,
    val sykdomsvurdering: Sykdomsvurdering
) {

}
