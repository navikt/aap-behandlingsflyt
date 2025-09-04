package no.nav.aap.behandlingsflyt.behandling.beregning.grunnlag.sykdom.overgangufore

import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvResponse
import java.time.LocalDate

data class OvergangUføreVurderingResponse(
    val begrunnelse: String,
    val brukerHarSøktUføretrygd: Boolean,
    val brukerHarFåttVedtakOmUføretrygd: String?,
    val brukerRettPåAAP: Boolean?,
    val virkningsdato: LocalDate?,
    val vurdertAv: VurdertAvResponse,
    val vurderingenGjelderFra: LocalDate?,
    val erGjeldende: Boolean?
)