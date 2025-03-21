package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.refusjonkrav

import no.nav.aap.komponenter.type.Periode

data class RefusjonkravVurdering(
    val harKrav: Boolean,
    val periode: Periode
)
