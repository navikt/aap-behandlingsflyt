package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import java.time.Instant
import java.time.LocalDate

data class Bistandsvurdering(
    val id: Long? = null,
    val begrunnelse: String,
    val erBehovForAktivBehandling: Boolean,
    val erBehovForArbeidsrettetTiltak: Boolean,
    val erBehovForAnnenOppfølging: Boolean?,
    val overgangBegrunnelse: String?,
    val skalVurdereAapIOvergangTilArbeid: Boolean?,
    val vurdertAv: String,
    val vurderingenGjelderFra: LocalDate,
    val opprettet: Instant? = null,
    val vurdertIBehandling: BehandlingId
) {
    fun erBehovForBistand(): Boolean {
        return (erBehovForAktivBehandling || erBehovForArbeidsrettetTiltak || erBehovForAnnenOppfølging == true)
    }
}

