package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand

import java.time.Instant
import java.time.LocalDate

data class BistandVurdering(
    val begrunnelse: String,
    val erBehovForAktivBehandling: Boolean,
    val erBehovForArbeidsrettetTiltak: Boolean,
    val erBehovForAnnenOppfølging: Boolean?,
    val overgangBegrunnelse: String?,
    val skalVurdereAapIOvergangTilArbeid: Boolean?,
    @Deprecated("""Det er i utgangspunktet Kelvin som avgjør om det mangler en vurdering av overgang til uføre når det kan være relevant.""")
    val skalVurdereAapIOvergangTilUføre: Boolean?,
    val vurdertAv: String,
    val vurderingenGjelderFra: LocalDate?,
    val opprettet: Instant? = null
) {
    fun erBehovForBistand(): Boolean {
        return (erBehovForAktivBehandling || erBehovForArbeidsrettetTiltak || erBehovForAnnenOppfølging == true)
    }
}

