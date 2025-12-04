package no.nav.aap.behandlingsflyt.behandling.beregning.grunnlag.sykdom.sykepengergrunnlag

import no.nav.aap.behandlingsflyt.PeriodiserteVurderingerDto
import no.nav.aap.behandlingsflyt.VurderingDto
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvResponse
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykepengerGrunn
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.verdityper.dokument.JournalpostId
import java.time.LocalDate
import java.time.LocalDateTime

data class SykepengerGrunnlagResponse(
    override val harTilgangTilÅSaksbehandle: Boolean,
    @Deprecated("Bruk nyeVurderinger") val vurderinger: List<SykepengerVurderingResponse>,
    @Deprecated("Bruk sisteVedtatteVurderinger") val vedtatteVurderinger: List<SykepengerVurderingResponse>,
    override val sisteVedtatteVurderinger: List<SykepengerVurderingResponse>,
    override val nyeVurderinger: List<SykepengerVurderingResponse>,
    override val kanVurderes: List<Periode>,
    override val behøverVurderinger: List<Periode>
): PeriodiserteVurderingerDto<SykepengerVurderingResponse>


data class SykepengerVurderingResponse(
    val begrunnelse: String,
    val dokumenterBruktIVurdering: List<JournalpostId>,
    val vurdertIBehandling: BehandlingId,
    val opprettet: LocalDateTime,
    val harRettPå: Boolean,
    @Deprecated("Bruk fom") val gjelderFra: LocalDate,
    @Deprecated("Bruk tom") val gjelderTom: LocalDate?,
    val grunn: SykepengerGrunn? = null,
    override val vurdertAv: VurdertAvResponse,
    override val fom: LocalDate,
    override val tom: LocalDate?,
    override val kvalitetssikretAv: VurdertAvResponse?,
    override val besluttetAv: VurdertAvResponse?,
): VurderingDto
