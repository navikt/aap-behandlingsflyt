package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand

data class BistandVurdering(
    val begrunnelse: String,
    val erBehovForBistand: Boolean,
    val erBehovForAktivBehandling: Boolean? = null,
    val erBehovForArbeidsrettetTiltak: Boolean? = null,
    val erBehovForAnnenOppfølging: Boolean? = null
)

object BistandGrunner {
    const val ARBEIDSRETTET_TILTAK = "Behov for arbeidsrettet tiltak"
    const val AKTIV_BEHANDLING = "Behov for aktiv behandling"
    const val ANNEN_OPPFØLGING = "Etter å ha prøvd tiltakene etter bokstav a eller b fortsatt anses for å ha en viss mulighet for å komme i arbeid, og får annen oppfølging fra Arbeids- og velferdsetaten"
}
