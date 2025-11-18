package no.nav.aap.behandlingsflyt.behandling.arbeidsopptrapping

import no.nav.aap.behandlingsflyt.PeriodiserteVurderingerDto
import no.nav.aap.behandlingsflyt.VurderingDto
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvResponse
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvService
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsopptrapping.ArbeidsopptrappingVurdering
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import java.time.LocalDate

data class ArbeidsopptrappingGrunnlagResponse(
    override val harTilgangTilÅSaksbehandle: Boolean,
    override val sisteVedtatteVurderinger: List<ArbeidsopptrappingVurderingResponse>,
    override val nyeVurderinger: List<ArbeidsopptrappingVurderingResponse>,
    override val kanVurderes: List<Periode>,
    override val behøverVurderinger: List<Periode>,
    val ikkeVurderbarePerioder: List<Periode>
) : PeriodiserteVurderingerDto<ArbeidsopptrappingVurderingResponse>

data class ArbeidsopptrappingVurderingResponse(
    override val fom: LocalDate,
    override val tom: LocalDate?,
    override val vurdertAv: VurdertAvResponse,
    override val kvalitetssikretAv: VurdertAvResponse?,
    override val besluttetAv: VurdertAvResponse?,
    val begrunnelse: String,
    val reellMulighetTilOpptrapping: Boolean,
    val rettPaaAAPIOpptrapping: Boolean,
) : VurderingDto {
    companion object {
        fun fraDomene(
            tidslinje: Tidslinje<ArbeidsopptrappingVurdering>,
            vurdertAvService: VurdertAvService,
        ): List<ArbeidsopptrappingVurderingResponse> {
            val segmenter = tidslinje.segmenter().toList()
            return segmenter
                .mapIndexed { index, segment ->
                    fraDomene(
                        arbeidsopptrappingVurdering = segment.verdi,
                        vurdertAvService = vurdertAvService,
                        fom = segment.fom(),
                        tom = if (index == segmenter.size - 1)
                            segment.verdi.vurderingenGjelderTil
                        else
                            segment.tom(),
                    )
                }
        }

        fun fraDomene(
            arbeidsopptrappingVurdering: ArbeidsopptrappingVurdering,
            vurdertAvService: VurdertAvService,
            fom: LocalDate = arbeidsopptrappingVurdering.vurderingenGjelderFra,
            tom: LocalDate? = arbeidsopptrappingVurdering.vurderingenGjelderTil,
        ) = ArbeidsopptrappingVurderingResponse(
            begrunnelse = arbeidsopptrappingVurdering.begrunnelse,
            reellMulighetTilOpptrapping = arbeidsopptrappingVurdering.reellMulighetTilOpptrapping,
            rettPaaAAPIOpptrapping = arbeidsopptrappingVurdering.rettPaaAAPIOpptrapping,
            fom = fom,
            tom = tom,
            vurdertAv = vurdertAvService.medNavnOgEnhet(
                arbeidsopptrappingVurdering.vurdertAv,
                arbeidsopptrappingVurdering.opprettetTid
            ),
            kvalitetssikretAv = vurdertAvService.kvalitetssikretAv(
                definisjon = Definisjon.ARBEIDSOPPTRAPPING,
                behandlingId = arbeidsopptrappingVurdering.vurdertIBehandling,
            ),
            besluttetAv = vurdertAvService.besluttetAv(
                definisjon = Definisjon.ARBEIDSOPPTRAPPING,
                behandlingId = arbeidsopptrappingVurdering.vurdertIBehandling,
            ),
        )
    }
}