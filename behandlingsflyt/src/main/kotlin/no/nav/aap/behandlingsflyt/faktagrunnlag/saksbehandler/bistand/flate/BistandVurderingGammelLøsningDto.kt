package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.flate

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.LøsningForPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.Bistandsvurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import java.time.Instant
import java.time.LocalDate

data class BistandLøsningDto(
    override val fom: LocalDate,
    override val tom: LocalDate?, // TODO: Støtt tom? Ignoreres enn så lenge
    override val begrunnelse: String,
    val erBehovForAktivBehandling: Boolean,
    val erBehovForArbeidsrettetTiltak: Boolean,
    val erBehovForAnnenOppfølging: Boolean?,
    val overgangBegrunnelse: String?,
    val skalVurdereAapIOvergangTilArbeid: Boolean?,
) : LøsningForPeriode {
    fun tilBistandVurdering(bruker: Bruker, vurdertIBehandling: BehandlingId) =
        Bistandsvurdering(
            begrunnelse = begrunnelse,
            erBehovForAktivBehandling = erBehovForAktivBehandling,
            erBehovForArbeidsrettetTiltak = erBehovForArbeidsrettetTiltak,
            erBehovForAnnenOppfølging = erBehovForAnnenOppfølging,
            vurderingenGjelderFra = fom,
            overgangBegrunnelse = overgangBegrunnelse,
            skalVurdereAapIOvergangTilArbeid = skalVurdereAapIOvergangTilArbeid,
            vurdertAv = bruker.ident,
            vurdertIBehandling = vurdertIBehandling,
            opprettet = Instant.now(),
        )

    fun valider() {
        val gyldigAnnenOppfølging =
            (erBehovForAktivBehandling || erBehovForArbeidsrettetTiltak) xor (erBehovForAnnenOppfølging != null)
        if (!gyldigAnnenOppfølging) throw UgyldigForespørselException(
            "erBehovForAnnenOppfølging kan bare bli besvart hvis erBehovForAktivBehandling og erBehovForArbeidsrettetTiltak er besvart med nei"
        )
    }
}
