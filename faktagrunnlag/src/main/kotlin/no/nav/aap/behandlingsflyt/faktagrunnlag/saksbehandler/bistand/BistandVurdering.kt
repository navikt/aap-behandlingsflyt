package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand

data class BistandVurdering(
    val begrunnelse: String,
    val erBehovForBistand: Boolean,
    val erBehovForAktivBehandling: Boolean? = null,
    val erBehovForArbeidsrettetTiltak: Boolean? = null,
    val erBehovForAnnenOppfølging: Boolean? = null
)

