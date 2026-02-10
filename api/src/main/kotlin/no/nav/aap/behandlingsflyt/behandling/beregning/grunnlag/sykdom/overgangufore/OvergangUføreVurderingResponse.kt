package no.nav.aap.behandlingsflyt.behandling.beregning.grunnlag.sykdom.overgangufore

import no.nav.aap.behandlingsflyt.VurderingDto
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvResponse
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvService
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangufore.OvergangUføreVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangufore.UføreSøknadVedtakResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.komponenter.tidslinje.Tidslinje
import java.time.LocalDate

data class OvergangUføreVurderingResponse(
    val begrunnelse: String,
    val brukerHarSøktUføretrygd: Boolean,
    val brukerHarFåttVedtakOmUføretrygd: UføreSøknadVedtakResultat?,
    val brukerRettPåAAP: Boolean?,
    @Deprecated("Bruk fom")
    val virkningsdato: LocalDate,
    override val fom: LocalDate,
    override val tom: LocalDate?,
    override val vurdertAv: VurdertAvResponse,
    override val kvalitetssikretAv: VurdertAvResponse?,
    override val besluttetAv: VurdertAvResponse?
) : VurderingDto {
    companion object {
        fun fraDomene(
            tidslinje: Tidslinje<OvergangUføreVurdering>,
            vurdertAvService: VurdertAvService
        ): List<OvergangUføreVurderingResponse> {
            val segmenter = tidslinje.segmenter().toList()
            return segmenter
                .mapIndexed { index, segment ->
                    fraDomene(
                        overgangUføreVurdering = segment.verdi,
                        vurdertAvService = vurdertAvService,
                        fom = segment.fom(),
                        tom = if (index == segmenter.size - 1)
                            segment.verdi.tom
                        else
                            segment.tom(),
                    )
                }
        }

        fun fraDomene(
            overgangUføreVurdering: OvergangUføreVurdering,
            vurdertAvService: VurdertAvService,
            fom: LocalDate = overgangUføreVurdering.fom,
            tom: LocalDate? = null
        ) = OvergangUføreVurderingResponse(
            begrunnelse = overgangUføreVurdering.begrunnelse,
            brukerHarSøktUføretrygd = overgangUføreVurdering.brukerHarSøktOmUføretrygd,
            brukerHarFåttVedtakOmUføretrygd = overgangUføreVurdering.brukerHarFåttVedtakOmUføretrygd,
            brukerRettPåAAP = overgangUføreVurdering.brukerRettPåAAP,
            virkningsdato = fom,
            fom = fom,
            tom = tom,
            vurdertAv = vurdertAvService.medNavnOgEnhet(
                overgangUføreVurdering.vurdertAv,
                overgangUføreVurdering.opprettet!!  // TODO: Sett denne i kode i stedet for database
            ),
            kvalitetssikretAv = vurdertAvService.kvalitetssikretAv(
                definisjon = Definisjon.AVKLAR_OVERGANG_UFORE,
                behandlingId = overgangUføreVurdering.vurdertIBehandling
            ),
            besluttetAv = vurdertAvService.besluttetAv(
                definisjon = Definisjon.AVKLAR_OVERGANG_UFORE,
                behandlingId = overgangUføreVurdering.vurdertIBehandling
            )
        )
    }
}