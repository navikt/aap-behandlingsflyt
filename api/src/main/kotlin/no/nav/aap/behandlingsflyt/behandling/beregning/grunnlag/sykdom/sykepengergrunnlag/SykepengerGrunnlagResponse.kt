package no.nav.aap.behandlingsflyt.behandling.beregning.grunnlag.sykdom.sykepengergrunnlag

import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvResponse
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykepengerGrunn
import no.nav.aap.verdityper.dokument.JournalpostId
import java.time.LocalDate

data class SykepengerGrunnlagResponse(
    val harTilgangTilÅSaksbehandle: Boolean,
    @Deprecated("Bruk vurderinger")
    val vurdering: SykepengerVurderingResponse?,
    val vurderinger: List<SykepengerVurderingResponse>,
    val vedtatteVurderinger: List<SykepengerVurderingResponse>
)

data class SykepengerVurderingResponse(
    val begrunnelse: String,
    val dokumenterBruktIVurdering: List<JournalpostId>,
    val harRettPå: Boolean,
    val gjelderFra: LocalDate?,
    val grunn: SykepengerGrunn? = null,
    val vurdertAv: VurdertAvResponse,
)
