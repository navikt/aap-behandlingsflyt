package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import java.time.Instant
import java.time.LocalDate

data class BistandVurdering(
    val id: Long? = null,
    val begrunnelse: String,
    val erBehovForAktivBehandling: Boolean,
    val erBehovForArbeidsrettetTiltak: Boolean,
    val erBehovForAnnenOppfølging: Boolean?,
    val overgangBegrunnelse: String?,
    val skalVurdereAapIOvergangTilArbeid: Boolean?,
    @Deprecated("""Det er i utgangspunktet Kelvin som avgjør om det mangler en vurdering av overgang til uføre når det kan være relevant.""")
    val skalVurdereAapIOvergangTilUføre: Boolean?,
    val vurdertAv: String,
    val vurderingenGjelderFra: LocalDate,
    val opprettet: Instant? = null,
    val vurdertIBehandling: BehandlingId
) {
    fun erBehovForBistand(): Boolean {
        return (erBehovForAktivBehandling || erBehovForArbeidsrettetTiltak || erBehovForAnnenOppfølging == true)
    }
}

