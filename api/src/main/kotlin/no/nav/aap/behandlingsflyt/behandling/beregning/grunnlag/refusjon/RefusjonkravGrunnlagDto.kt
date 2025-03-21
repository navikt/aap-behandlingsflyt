package no.nav.aap.behandlingsflyt.behandling.beregning.grunnlag.refusjon

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.refusjonkrav.RefusjonkravVurdering


data class RefusjonkravGrunnlagDto (
    val gjeldendeVurdering: RefusjonkravVurdering?,
    val historiskeVurderinger: List<RefusjonkravVurdering>?
)