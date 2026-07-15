package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import java.time.Instant
import java.time.LocalDate

data class Bistandsvurdering(
    val begrunnelse: String,
    val erBehovForAktivBehandling: Boolean,
    val erBehovForArbeidsrettetTiltak: Boolean,
    val erBehovForAnnenOppfølging: Boolean?,
    val overgangBegrunnelse: String?,
    val skalVurdereAapIOvergangTilArbeid: Boolean?,
    val vurdertAv: String,
    val vurderingenGjelderFra: LocalDate,
    val tom: LocalDate?,
    val opprettet: Instant,
    val vurdertIBehandling: BehandlingId
) {
    fun erBehovForBistand(): Boolean {
        return (erBehovForAktivBehandling || erBehovForArbeidsrettetTiltak || erBehovForAnnenOppfølging == true)
    }
}

fun List<Bistandsvurdering>.erFunksjoneltLik(annen: List<Bistandsvurdering>): Boolean {
    if (this.size != annen.size) return false

    // sammenlikner alle felter unntat vurdertAv og tidsstempel
    return this.zip(annen).all { (første, andre) ->
        første.begrunnelse == andre.begrunnelse &&
                første.erBehovForAktivBehandling == andre.erBehovForAktivBehandling &&
                første.erBehovForArbeidsrettetTiltak == andre.erBehovForArbeidsrettetTiltak &&
                første.erBehovForAnnenOppfølging == andre.erBehovForAnnenOppfølging &&
                første.overgangBegrunnelse == andre.overgangBegrunnelse &&
                første.skalVurdereAapIOvergangTilArbeid == andre.skalVurdereAapIOvergangTilArbeid &&
                første.vurderingenGjelderFra == andre.vurderingenGjelderFra &&
                første.tom == andre.tom
    }
}

