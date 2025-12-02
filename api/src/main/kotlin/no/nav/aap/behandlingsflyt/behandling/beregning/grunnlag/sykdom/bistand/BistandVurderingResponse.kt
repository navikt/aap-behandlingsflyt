package no.nav.aap.behandlingsflyt.behandling.beregning.grunnlag.sykdom.bistand

import no.nav.aap.behandlingsflyt.VurderingDto
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvResponse
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvService
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.Bistandsvurdering
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.verdityper.Tid
import java.time.LocalDate

data class BistandVurderingResponse(
    val begrunnelse: String,
    val erBehovForAktivBehandling: Boolean,
    val erBehovForArbeidsrettetTiltak: Boolean,
    val erBehovForAnnenOppfølging: Boolean?,
    val overgangBegrunnelse: String?,
    val skalVurdereAapIOvergangTilArbeid: Boolean?,
    @Deprecated("Bruk fom")
    val vurderingenGjelderFra: LocalDate?,
    override val fom: LocalDate,
    override val tom: LocalDate?,
    override val vurdertAv: VurdertAvResponse,
    override val kvalitetssikretAv: VurdertAvResponse?,
    override val besluttetAv: VurdertAvResponse?
) : VurderingDto {
    companion object {
        fun fraDomene(
            tidslinje: Tidslinje<Bistandsvurdering>,
            vurdertAvService: VurdertAvService,
        ): List<BistandVurderingResponse> {
            val segmenter = tidslinje.segmenter().toList()
            return segmenter
                .mapIndexed { index, segment ->
                    fraDomene(
                        bistandsvurdering = segment.verdi,
                        vurdertAvService = vurdertAvService,
                        fom = segment.fom(),
                        tom = if (index == segmenter.size - 1)
                            Tid.MAKS
                        else
                            segment.tom(),
                    )
                }
        }

        fun fraDomene(
            bistandsvurdering: Bistandsvurdering,
            vurdertAvService: VurdertAvService,
            fom: LocalDate = bistandsvurdering.vurderingenGjelderFra,
            tom: LocalDate? = null
        ) = BistandVurderingResponse(
            begrunnelse = bistandsvurdering.begrunnelse,
            erBehovForAktivBehandling = bistandsvurdering.erBehovForAktivBehandling,
            erBehovForArbeidsrettetTiltak = bistandsvurdering.erBehovForArbeidsrettetTiltak,
            erBehovForAnnenOppfølging = bistandsvurdering.erBehovForAnnenOppfølging,
            fom = fom,
            vurderingenGjelderFra = fom,
            tom = tom,
            skalVurdereAapIOvergangTilArbeid = bistandsvurdering.skalVurdereAapIOvergangTilArbeid,
            overgangBegrunnelse = bistandsvurdering.overgangBegrunnelse,
            vurdertAv = vurdertAvService.medNavnOgEnhet(bistandsvurdering.vurdertAv, bistandsvurdering.opprettet),
            kvalitetssikretAv = vurdertAvService.kvalitetssikretAv(
                definisjon = Definisjon.AVKLAR_BISTANDSBEHOV,
                behandlingId = bistandsvurdering.vurdertIBehandling,
            ),
            besluttetAv = vurdertAvService.besluttetAv(
                definisjon = Definisjon.AVKLAR_BISTANDSBEHOV,
                behandlingId = bistandsvurdering.vurdertIBehandling,
            ),
        )
    }
}