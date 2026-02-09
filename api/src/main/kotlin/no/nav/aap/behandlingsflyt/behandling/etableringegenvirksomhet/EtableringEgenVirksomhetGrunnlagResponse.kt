package no.nav.aap.behandlingsflyt.behandling.etableringegenvirksomhet

import no.nav.aap.behandlingsflyt.PeriodiserteVurderingerDto
import no.nav.aap.behandlingsflyt.VurderingDto
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvResponse
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvService
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.EtableringEgenVirksomhetVurdering
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import java.time.LocalDate

data class EtableringEgenVirksomhetGrunnlagResponse(
    override val harTilgangTilÅSaksbehandle: Boolean,
    override val sisteVedtatteVurderinger: List<EtableringEgenVirksomhetVurderingResponse>,
    override val nyeVurderinger: List<EtableringEgenVirksomhetVurderingResponse>,
    override val kanVurderes: List<Periode>,
    override val behøverVurderinger: List<Periode>,
    val kvalitetssikretAv: VurdertAvResponse?,
    val ikkeVurderbarePerioder: List<Periode>
) : PeriodiserteVurderingerDto<EtableringEgenVirksomhetVurderingResponse>

data class EtableringEgenVirksomhetVurderingResponse(
    override val fom: LocalDate,
    override val tom: LocalDate?,
    override val vurdertAv: VurdertAvResponse,
    override val kvalitetssikretAv: VurdertAvResponse?,
    override val besluttetAv: VurdertAvResponse?,

    val begrunnelse: String,
    val vurderingenGjelderFra: LocalDate,
    val foreliggerFagligVurdering: Boolean,
    val virksomhetErNy: Boolean,
    val brukerEierVirksomheten: Boolean,
    val kanFøreTilSelvforsørget: Boolean,
    val utviklingsPeriode: List<Periode>,
    val oppstartsPeriode: List<Periode>
) : VurderingDto {
    companion object {
        fun fraDomene(
            tidslinje: Tidslinje<EtableringEgenVirksomhetVurdering>,
            vurdertAvService: VurdertAvService,
        ): List<EtableringEgenVirksomhetVurderingResponse> {
            val segmenter = tidslinje.segmenter().toList()
            return segmenter
                .mapIndexed { index, segment ->
                    fraDomene(
                        etableringEgenVirksomhetVurdering = segment.verdi,
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
            etableringEgenVirksomhetVurdering: EtableringEgenVirksomhetVurdering,
            vurdertAvService: VurdertAvService,
            fom: LocalDate = etableringEgenVirksomhetVurdering.vurderingenGjelderFra,
            tom: LocalDate? = etableringEgenVirksomhetVurdering.vurderingenGjelderTil
        ) = EtableringEgenVirksomhetVurderingResponse(
            begrunnelse = etableringEgenVirksomhetVurdering.begrunnelse,
            vurderingenGjelderFra = etableringEgenVirksomhetVurdering.vurderingenGjelderFra,
            foreliggerFagligVurdering = etableringEgenVirksomhetVurdering.foreliggerFagligVurdering,
            virksomhetErNy = etableringEgenVirksomhetVurdering.virksomhetErNy,
            brukerEierVirksomheten = etableringEgenVirksomhetVurdering.brukerEierVirksomheten,
            kanFøreTilSelvforsørget = etableringEgenVirksomhetVurdering.kanFøreTilSelvforsørget,
            utviklingsPeriode = etableringEgenVirksomhetVurdering.utviklingsPerioder,
            oppstartsPeriode = etableringEgenVirksomhetVurdering.oppstartsPerioder,
            vurdertAv = vurdertAvService.medNavnOgEnhet(
                etableringEgenVirksomhetVurdering.vurdertAv.ident,
                etableringEgenVirksomhetVurdering.opprettetTid
            ),
            fom = fom,
            tom = tom,
            kvalitetssikretAv = vurdertAvService.kvalitetssikretAv(
                definisjon = Definisjon.ETABLERING_EGEN_VIRKSOMHET,
                behandlingId = etableringEgenVirksomhetVurdering.vurdertIBehandling
            ),
            besluttetAv = vurdertAvService.besluttetAv(
                definisjon = Definisjon.ETABLERING_EGEN_VIRKSOMHET,
                behandlingId = etableringEgenVirksomhetVurdering.vurdertIBehandling
            )
        )
    }
}