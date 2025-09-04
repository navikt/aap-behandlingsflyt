package no.nav.aap.behandlingsflyt.behandling.beregning.grunnlag.sykdom.overgangarbeid

import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvResponse
import java.time.LocalDate

data class OvergangArbeidVurderingResponse(
    val begrunnelse: String,
    val brukerRettPåAAP: Boolean?,
    val virkningsdato: LocalDate?,
    val vurdertAv: VurdertAvResponse,
    val vurderingenGjelderFra: LocalDate?,
    val erGjeldende: Boolean?
)