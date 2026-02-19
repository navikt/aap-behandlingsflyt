package no.nav.aap.behandlingsflyt.behandling.beregning.grunnlag.fritakmeldeplikt

import no.nav.aap.behandlingsflyt.PeriodiserteVurderingerDto
import no.nav.aap.behandlingsflyt.VurderingDto
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvResponse
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvService
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.Fritaksvurdering
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid
import java.time.LocalDate

data class FritakMeldepliktGrunnlagResponse(
    override val harTilgangTilÅSaksbehandle: Boolean,
    override val sisteVedtatteVurderinger: List<PeriodisertFritakMeldepliktVurderingResponse>,
    override val nyeVurderinger: List<PeriodisertFritakMeldepliktVurderingResponse>,
    override val kanVurderes: List<Periode>,
    override val behøverVurderinger: List<Periode>,
    override val ikkeRelevantePerioder: List<Periode>,
) : PeriodiserteVurderingerDto<PeriodisertFritakMeldepliktVurderingResponse>

data class PeriodisertFritakMeldepliktVurderingResponse(
    override val fom: LocalDate,
    override val tom: LocalDate?,
    override val vurdertAv: VurdertAvResponse?,
    override val kvalitetssikretAv: VurdertAvResponse? = null,
    override val besluttetAv: VurdertAvResponse? = null,
    val begrunnelse: String,
    val harFritak: Boolean,
) : VurderingDto

fun Tidslinje<Fritaksvurdering>.toResponse(
    vurdertAvService: VurdertAvService,
) = segmenter()
        .map { segment ->
            segment.verdi.toResponse(
                vurdertAvService = vurdertAvService,
                fom = segment.fom(),
                tom = if (segment.tom().isEqual(Tid.MAKS)) null else segment.tom()
            )
        }

fun Fritaksvurdering.toResponse(
    vurdertAvService: VurdertAvService,
    fom: LocalDate = this.fraDato,
    tom: LocalDate? = this.tilDato,
) =
    PeriodisertFritakMeldepliktVurderingResponse(
        fom = fom,
        tom = tom,
        vurdertAv = vurdertAvService.medNavnOgEnhet(vurdertAv, opprettetTid.toLocalDate()),
        besluttetAv = vurdertAvService.besluttetAv(
            definisjon = Definisjon.FRITAK_MELDEPLIKT,
            behandlingId = vurdertIBehandling
        ),
        kvalitetssikretAv = vurdertAvService.besluttetAv(
            definisjon = Definisjon.FRITAK_MELDEPLIKT,
            behandlingId = vurdertIBehandling
        ),
        begrunnelse = begrunnelse,
        harFritak = harFritak,
    )