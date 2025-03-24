package no.nav.aap.behandlingsflyt.behandling.beregning.grunnlag.sykdom.sykdom

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.flate.InnhentetSykdomsOpplysninger
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.flate.YrkesskadevurderingDto

data class YrkesskadeVurderingGrunnlagDto(
    val harTilgangTilÅSaksbehandle: Boolean,
    val opplysninger: InnhentetSykdomsOpplysninger,
    val yrkesskadeVurdering: YrkesskadevurderingDto?
)