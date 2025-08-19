package no.nav.aap.behandlingsflyt.behandling.underveis

import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvResponse
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.MeldepliktOverstyringStatus
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.komponenter.type.Periode
import java.time.LocalDate
import java.time.LocalDateTime

data class MeldepliktOverstyringGrunnlagResponse(
    val harTilgangTilÅSaksbehandle: Boolean,
    val perioderIkkeMeldt: List<Periode>,
    val overstyringsvurderinger: List<MeldepliktOverstyringVurderingResponse>,
    val gjeldendeVedtatteOversyringsvurderinger: List<MeldepliktOverstyringVurderingResponse>
)

data class MeldepliktOverstyringVurderingResponse(
    val begrunnelse: String,
    val vurderingsTidspunkt: LocalDateTime,
    val meldepliktOverstyringStatus: MeldepliktOverstyringStatus,
    val vurdertIBehandling: BehandlingReferanse,
    val fraDato: LocalDate,
    val tilDato: LocalDate,
    val vurdertAv: VurdertAvResponse?
)
