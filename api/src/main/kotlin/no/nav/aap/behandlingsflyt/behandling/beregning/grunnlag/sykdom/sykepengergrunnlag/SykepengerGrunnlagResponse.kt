package no.nav.aap.behandlingsflyt.behandling.beregning.grunnlag.sykdom.sykepengergrunnlag

import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvResponse
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykepengerGrunn
import no.nav.aap.verdityper.dokument.JournalpostId

data class SykepengerGrunnlagResponse(
    val harTilgangTilÅSaksbehandle: Boolean,
    val vurdering: SykepengerVurderingResponse?
)

data class SykepengerVurderingResponse(
    val begrunnelse: String,
    val dokumenterBruktIVurdering: List<JournalpostId>,
    val harRettPå: Boolean,
    val grunn: SykepengerGrunn? = null,
    val vurdertAv: VurdertAvResponse,
)
