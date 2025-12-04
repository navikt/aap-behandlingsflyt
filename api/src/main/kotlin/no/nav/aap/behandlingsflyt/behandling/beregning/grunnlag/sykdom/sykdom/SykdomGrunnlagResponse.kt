package no.nav.aap.behandlingsflyt.behandling.beregning.grunnlag.sykdom.sykdom

import no.nav.aap.behandlingsflyt.PeriodiserteVurderingerDto
import no.nav.aap.behandlingsflyt.VurderingDto
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvResponse
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvService
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Sykdomsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.flate.InnhentetSykdomsOpplysninger
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid
import no.nav.aap.verdityper.dokument.JournalpostId
import java.time.LocalDate

data class SykdomGrunnlagResponse(
    override val harTilgangTilÅSaksbehandle: Boolean,
    val skalVurdereYrkesskade: Boolean,
    val erÅrsakssammenhengYrkesskade: Boolean,
    val opplysninger: InnhentetSykdomsOpplysninger,
    @Deprecated("Bruk nyeVurderinger")
    val sykdomsvurderinger: List<SykdomsvurderingResponse>,
    override val nyeVurderinger: List<SykdomsvurderingResponse>,
    val historikkSykdomsvurderinger: List<SykdomsvurderingResponse>,
    @Deprecated("Bruk sisteVedtatteVurderinger")
    val gjeldendeVedtatteSykdomsvurderinger: List<SykdomsvurderingResponse>,
    override val sisteVedtatteVurderinger: List<SykdomsvurderingResponse>,
    @Deprecated("Ligger på vurderingsnivå")
    val kvalitetssikretAv: VurdertAvResponse?,
    override val kanVurderes: List<Periode>,
    override val behøverVurderinger: List<Periode>,
    val perioderSomIkkeErTilstrekkeligVurdert: List<Periode>
    ): PeriodiserteVurderingerDto<SykdomsvurderingResponse>

data class SykdomsvurderingResponse(
    override val vurdertAv: VurdertAvResponse,
    override val fom: LocalDate,
    override val tom: LocalDate?,
    override val kvalitetssikretAv: VurdertAvResponse?,
    override val besluttetAv: VurdertAvResponse?,

    /** Hvis null, så gjelder den fra starten. */
    val begrunnelse: String,
    val vurderingenGjelderFra: LocalDate?, @Deprecated("Bruk fom")
    val dokumenterBruktIVurdering: List<JournalpostId>,
    val erArbeidsevnenNedsatt: Boolean?,
    val harSkadeSykdomEllerLyte: Boolean,
    val erSkadeSykdomEllerLyteVesentligdel: Boolean?,
    val erNedsettelseIArbeidsevneAvEnVissVarighet: Boolean?,
    val erNedsettelseIArbeidsevneMerEnnHalvparten: Boolean?,
    val erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense: Boolean?,
    val yrkesskadeBegrunnelse: String?,
    val kodeverk: String? = null,
    val hoveddiagnose: String? = null,
    val bidiagnoser: List<String>? = emptyList(),
): VurderingDto {
    companion object {
        fun fraDomene(
            tidslinje: Tidslinje<Sykdomsvurdering>,
            vurdertAvService: VurdertAvService,
        ): List<SykdomsvurderingResponse> {
            val segmenter = tidslinje.segmenter().toList()
            return segmenter
                .mapIndexed { index, segment ->
                    fraDomene(
                        sykdomsvurdering = segment.verdi,
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
            sykdomsvurdering: Sykdomsvurdering,
            vurdertAvService: VurdertAvService,
            fom: LocalDate = sykdomsvurdering.vurderingenGjelderFra,
            tom: LocalDate? = sykdomsvurdering.vurderingenGjelderTil
        ) = SykdomsvurderingResponse(
            begrunnelse = sykdomsvurdering.begrunnelse,
            vurderingenGjelderFra = sykdomsvurdering.vurderingenGjelderFra,
            dokumenterBruktIVurdering = sykdomsvurdering.dokumenterBruktIVurdering,
            erArbeidsevnenNedsatt = sykdomsvurdering.erArbeidsevnenNedsatt,
            harSkadeSykdomEllerLyte = sykdomsvurdering.harSkadeSykdomEllerLyte,
            erSkadeSykdomEllerLyteVesentligdel = sykdomsvurdering.erSkadeSykdomEllerLyteVesentligdel,
            erNedsettelseIArbeidsevneAvEnVissVarighet = sykdomsvurdering.erNedsettelseIArbeidsevneAvEnVissVarighet,
            erNedsettelseIArbeidsevneMerEnnHalvparten = sykdomsvurdering.erNedsettelseIArbeidsevneMerEnnHalvparten,
            erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = sykdomsvurdering.erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense,
            yrkesskadeBegrunnelse = sykdomsvurdering.yrkesskadeBegrunnelse,
            kodeverk = sykdomsvurdering.kodeverk,
            hoveddiagnose = sykdomsvurdering.hoveddiagnose,
            bidiagnoser = sykdomsvurdering.bidiagnoser,
            fom = fom,
            tom = tom,
            vurdertAv = vurdertAvService.medNavnOgEnhet(sykdomsvurdering.vurdertAv.ident, sykdomsvurdering.opprettet),
            kvalitetssikretAv = vurdertAvService.kvalitetssikretAv(
                definisjon = Definisjon.AVKLAR_SYKDOM,
                behandlingId = sykdomsvurdering.vurdertIBehandling,
            ),
            besluttetAv = vurdertAvService.besluttetAv(
                definisjon = Definisjon.AVKLAR_SYKDOM,
                behandlingId = sykdomsvurdering.vurdertIBehandling,
            ),
        )
    }
}
