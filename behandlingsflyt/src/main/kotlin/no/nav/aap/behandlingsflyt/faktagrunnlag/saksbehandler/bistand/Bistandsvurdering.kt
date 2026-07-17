package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.PeriodisertVurdering
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
    override val fom: LocalDate,
    override val tom: LocalDate?,
    override val opprettet: Instant,
    override val vurdertIBehandling: BehandlingId
): PeriodisertVurdering {
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
                første.fom == andre.fom &&
                første.tom == andre.tom
    }
}

