package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.avslag11_27.flate

import no.nav.aap.behandlingsflyt.behandling.samordning.Ytelse

data class Avslag11_27VurderingDto(
    val referanse: String,
    val begrunnelse: String,
    val harAnnenFullYtelse: Boolean,
    val brukersYtelse: Ytelse? = null,
    /**
     * Kun for sykepenger
     */
    val harSykepengegrunnlagOver2G: Boolean? = null,
    val skalAvslås1127: Boolean,
)
