package no.nav.aap.behandlingsflyt.behandling.totrinnsvurdering

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.vedtak.TotrinnsVurdering
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.ÅrsakTilRetur
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode

data class TotrinnsVurderingResponse(
    val definisjon: AvklaringsbehovKode,
    val godkjent: Boolean?,
    val begrunnelse: String?,
    val endretSidenSist: Boolean?,
    val grunner: List<ÅrsakTilRetur>?,
) {
    fun tilTotrinnsVurdering() = TotrinnsVurdering(
        definisjon = definisjon,
        godkjent = godkjent,
        begrunnelse = begrunnelse,
        grunner = grunner,
        markeringer = emptyList()
    )
}