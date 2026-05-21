package no.nav.aap.behandlingsflyt.behandling.vurdering

data class VurderingerMetaResponse(
    val vurdertAv: VurdertAvResponse? = null,
    val besluttetAv: VurdertAvResponse? = null,
    val kvalitetssikretAv: VurdertAvResponse? = null,
)

