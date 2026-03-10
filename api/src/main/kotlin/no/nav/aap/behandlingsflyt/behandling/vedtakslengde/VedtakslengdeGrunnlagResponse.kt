package no.nav.aap.behandlingsflyt.behandling.vedtakslengde

import no.nav.aap.behandlingsflyt.PeriodiserteVurderingerDto
import no.nav.aap.behandlingsflyt.VurderingDto
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.ÅrMedHverdager
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvResponse
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvService
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.vedtakslengde.VedtakslengdeVurdering
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.type.Periode
import java.time.LocalDate

data class VedtakslengdeGrunnlagResponse(
    override val harTilgangTilÅSaksbehandle: Boolean,
    override val sisteVedtatteVurderinger: List<VedtakslengdeVurderingResponse>,
    override val nyeVurderinger: List<VedtakslengdeVurderingResponse>,
    override val kanVurderes: List<Periode>,
    override val behøverVurderinger: List<Periode>,
    override val ikkeRelevantePerioder: List<Periode>,
) : PeriodiserteVurderingerDto<VedtakslengdeVurderingResponse>

data class VedtakslengdeVurderingResponse(
    override val fom: LocalDate,
    override val tom: LocalDate?,
    override val vurdertAv: VurdertAvResponse?,
    override val kvalitetssikretAv: VurdertAvResponse?,
    override val besluttetAv: VurdertAvResponse?,
    val sluttdato: LocalDate,
    val utvidetMed: ÅrMedHverdager,
    val begrunnelse: String,
) : VurderingDto

fun VedtakslengdeVurdering.toResponse(
    vurdertAvService: VurdertAvService,
    periode: Periode,
) = VedtakslengdeVurderingResponse(
    fom = periode.fom,
    tom = periode.tom,
    vurdertAv = vurdertAvService.medNavnOgEnhet(vurdertAv.ident, opprettet),
    kvalitetssikretAv = null,
    besluttetAv = vurdertAvService.besluttetAv(
        definisjon = Definisjon.AVKLAR_VEDTAKSLENGDE,
        behandlingId = vurdertIBehandling,
    ),
    sluttdato = sluttdato,
    utvidetMed = utvidetMed,
    begrunnelse = begrunnelse,
)