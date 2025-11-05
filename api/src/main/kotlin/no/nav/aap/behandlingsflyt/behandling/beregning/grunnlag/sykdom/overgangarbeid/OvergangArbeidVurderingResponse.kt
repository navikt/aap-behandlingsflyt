package no.nav.aap.behandlingsflyt.behandling.beregning.grunnlag.sykdom.overgangarbeid

import no.nav.aap.behandlingsflyt.VurderingDto
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvResponse
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvService
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangarbeid.OvergangArbeidVurdering
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.komponenter.tidslinje.Tidslinje
import java.time.LocalDate
import java.util.UUID

data class OvergangArbeidVurderingResponse(
    override val id: UUID = UUID.randomUUID(),
    override val fom: LocalDate,
    override val tom: LocalDate?,
    override val vurdertAv: VurdertAvResponse,
    override val kvalitetssikretAv: VurdertAvResponse?,
    override val besluttetAv: VurdertAvResponse?,
    val begrunnelse: String,
    val brukerRettPåAAP: Boolean,
) : VurderingDto {

    companion object {
        fun fraDomene(
            tidslinje: Tidslinje<OvergangArbeidVurdering>,
            vurdertAvService: VurdertAvService,
        ): List<OvergangArbeidVurderingResponse> {
            val segmenter = tidslinje.segmenter().toList()
            return segmenter
                .mapIndexed { index, segment ->
                    fraDomene(
                        overgangArbeidVurdering = segment.verdi,
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
            overgangArbeidVurdering: OvergangArbeidVurdering,
            vurdertAvService: VurdertAvService,
            fom: LocalDate = overgangArbeidVurdering.vurderingenGjelderFra,
            tom: LocalDate? = overgangArbeidVurdering.vurderingenGjelderTil,
        ) = OvergangArbeidVurderingResponse(
            begrunnelse = overgangArbeidVurdering.begrunnelse,
            brukerRettPåAAP = overgangArbeidVurdering.brukerRettPåAAP,
            fom = fom,
            tom = tom,
            vurdertAv = vurdertAvService.medNavnOgEnhet(overgangArbeidVurdering.vurdertAv, overgangArbeidVurdering.opprettet),
            kvalitetssikretAv = vurdertAvService.kvalitetssikretAv(
                definisjon = Definisjon.AVKLAR_OVERGANG_ARBEID,
                behandlingId = overgangArbeidVurdering.vurdertIBehandling,
            ),
            besluttetAv = vurdertAvService.besluttetAv(
                definisjon = Definisjon.AVKLAR_OVERGANG_ARBEID,
                behandlingId = overgangArbeidVurdering.vurdertIBehandling,
            ),
        )
    }
}

