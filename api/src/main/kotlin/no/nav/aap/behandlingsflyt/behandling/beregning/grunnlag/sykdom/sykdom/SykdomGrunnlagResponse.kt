package no.nav.aap.behandlingsflyt.behandling.beregning.grunnlag.sykdom.sykdom

import no.nav.aap.behandlingsflyt.PeriodiserteVurderingerDto
import no.nav.aap.behandlingsflyt.VurderingDto
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurderingerMetaResponse
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvService
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.ArbeidsevneNedsattValg
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.ErNedsettelseMerEnnYrkesskadegrenseValg
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.ErNedsettelseMinstHalvpartenValg
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Sykdomsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.flate.InnhentetSykdomsOpplysninger
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.verdityper.dokument.JournalpostId
import java.time.LocalDate

data class SykdomGrunnlagResponse(
    override val harTilgangTilÅSaksbehandle: Boolean,
    val skalVurdereYrkesskade: Boolean,
    val erÅrsakssammenhengYrkesskade: Boolean,
    val opplysninger: InnhentetSykdomsOpplysninger,
    override val nyeVurderinger: List<SykdomsvurderingResponse>,
    override val sisteVedtatteVurderinger: List<SykdomsvurderingResponse>,
    override val kanVurderes: List<Periode>,
    override val ikkeRelevantePerioder: List<Periode>,
    override val behøverVurderinger: List<Periode>,
    ): PeriodiserteVurderingerDto<SykdomsvurderingResponse>

data class SykdomsvurderingResponse(
    override val vurderingerMeta: VurderingerMetaResponse,
    override val fom: LocalDate,
    override val tom: LocalDate?,

    /** Hvis null, så gjelder den fra starten. */
    val begrunnelse: String,
    val vurderingenGjelderFra: LocalDate?, @Deprecated("Bruk fom")
    val dokumenterBruktIVurdering: List<JournalpostId>,
    @Deprecated("Bruk harArbeidsevneNedsatt")
    val erArbeidsevnenNedsatt: Boolean?,
    val harNedsattArbeidsevne: ArbeidsevneNedsattValg?,
    val harSkadeSykdomEllerLyte: Boolean,
    val erSkadeSykdomEllerLyteVesentligdel: Boolean?,
    val erNedsettelseIArbeidsevneMerEnnHalvparten: Boolean?,
    @Deprecated("Bruk erNedsettelseIArbeidsevneMerEnnHalvparten")
    val erNedsettelseMinstHalvparten: ErNedsettelseMinstHalvpartenValg?,
    @Deprecated("Bruk erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense")
    val erNedsettelseMerEnnYrkesskadegrense: ErNedsettelseMerEnnYrkesskadegrenseValg?,
    val erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense: Boolean?,
    @Deprecated("Bruk harNedsattArbeidsevne")
    val erNedsettelseIArbeidsevneAvEnVissVarighet: Boolean?,
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
                            segment.verdi.vurderingenGjelderTil
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
            harNedsattArbeidsevne = sykdomsvurdering.harNedsattArbeidsevne,
            harSkadeSykdomEllerLyte = sykdomsvurdering.harSkadeSykdomEllerLyte,
            erSkadeSykdomEllerLyteVesentligdel = sykdomsvurdering.erSkadeSykdomEllerLyteVesentligdel,
            erNedsettelseMinstHalvparten = sykdomsvurdering.erNedsettelseMinstHalvparten,
            erNedsettelseMerEnnYrkesskadegrense = sykdomsvurdering.erNedsettelseMerEnnYrkesskadegrense,
            erNedsettelseIArbeidsevneAvEnVissVarighet = sykdomsvurdering.erNedsettelseIArbeidsevneAvEnVissVarighet,
            erNedsettelseIArbeidsevneMerEnnHalvparten = sykdomsvurdering.erNedsettelseIArbeidsevneMerEnnHalvparten,
            erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = sykdomsvurdering.erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense,
            yrkesskadeBegrunnelse = sykdomsvurdering.yrkesskadeBegrunnelse,
            kodeverk = sykdomsvurdering.diagnose?.kodeverk,
            hoveddiagnose = sykdomsvurdering.diagnose?.hoveddiagnose,
            bidiagnoser = sykdomsvurdering.diagnose?.bidiagnoser.orEmpty(),
            fom = fom,
            tom = tom,
            vurderingerMeta = vurdertAvService.byggVurderingerMeta(
                definisjon = Definisjon.AVKLAR_SYKDOM,
                behandlingId = sykdomsvurdering.vurdertIBehandling,
                vurdertAv = vurdertAvService.medNavnOgEnhet(sykdomsvurdering.vurdertAv.ident, sykdomsvurdering.opprettet),
            ),
        )
    }
}
