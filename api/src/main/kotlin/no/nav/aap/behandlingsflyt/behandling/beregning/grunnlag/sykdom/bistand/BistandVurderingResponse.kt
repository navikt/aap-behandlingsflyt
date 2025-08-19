package no.nav.aap.behandlingsflyt.behandling.beregning.grunnlag.sykdom.bistand

import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvResponse
import java.time.LocalDate

data class BistandVurderingResponse(
    val begrunnelse: String,
    val erBehovForAktivBehandling: Boolean,
    val erBehovForArbeidsrettetTiltak: Boolean,
    val erBehovForAnnenOppfølging: Boolean?,
    val overgangBegrunnelse: String?,
    val skalVurdereAapIOvergangTilUføre: Boolean?,
    val skalVurdereAapIOvergangTilArbeid: Boolean?,
    val vurderingenGjelderFra: LocalDate?,
    val vurdertAv: VurdertAvResponse,
    val erGjeldende: Boolean?
)