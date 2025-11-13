package no.nav.aap.behandlingsflyt.behandling.beregning.grunnlag.refusjon

import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvResponse
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.AndreUtbetalingerYtelser
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.AndreYtelserSøknad
import java.time.LocalDate


data class RefusjonkravGrunnlagResponse(
    val harTilgangTilÅSaksbehandle: Boolean,
    val gjeldendeVurdering: RefusjonkravVurderingResponse?,
    val gjeldendeVurderinger: List<RefusjonkravVurderingResponse>?,
    val økonomiskSosialHjelp: Boolean?,
    val nåværendeVirkningsTidspunkt: LocalDate?,
)

data class RefusjonkravVurderingResponse(
    val harKrav: Boolean,
    val fom: LocalDate?,
    val tom: LocalDate?,
    val navKontor: String?,
    val vurdertAv: VurdertAvResponse
)