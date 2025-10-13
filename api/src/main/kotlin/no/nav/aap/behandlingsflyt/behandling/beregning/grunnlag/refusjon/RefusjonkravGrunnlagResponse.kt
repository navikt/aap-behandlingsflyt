package no.nav.aap.behandlingsflyt.behandling.beregning.grunnlag.refusjon

import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvResponse
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.andrestatligeytelservurdering.AndreStatligeYtelser
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.AndreUtbetalingerYtelser
import java.time.LocalDate


data class RefusjonkravGrunnlagResponse(
    val harTilgangTilÅSaksbehandle: Boolean,
    val gjeldendeVurdering: RefusjonkravVurderingResponse?,
    val gjeldendeVurderinger: List<RefusjonkravVurderingResponse>?,
    val historiskeVurderinger: List<RefusjonkravVurderingResponse>?,
    val andreUtbetalingerYtelser: List<AndreUtbetalingerYtelser>?,
)

data class RefusjonkravVurderingResponse(
    val harKrav: Boolean,
    val fom: LocalDate?,
    val tom: LocalDate?,
    val navKontor: String?,
    val vurdertAv: VurdertAvResponse
)