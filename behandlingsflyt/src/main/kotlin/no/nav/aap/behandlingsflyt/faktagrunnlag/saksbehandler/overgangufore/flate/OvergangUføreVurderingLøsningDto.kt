package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangufore.flate

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangufore.OvergangUføreVurdering
import no.nav.aap.komponenter.verdityper.Bruker
import java.time.LocalDate

data class OvergangUføreVurderingLøsningDto(
    val begrunnelse: String,
    val brukerSoktUforetrygd: Boolean,
    val brukerVedtakUforetrygd: String,
    val brukerRettPaaAAP: Boolean?,
    val virkningsDato: LocalDate?,
    val overgangBegrunnelse: String?,
) {
    fun tilOvergangUføreVurdering(bruker: Bruker, vurderingenGjelderFra: LocalDate?) = OvergangUføreVurdering(
        begrunnelse = begrunnelse,
        brukerSoktUforetrygd = brukerSoktUforetrygd,
        brukerVedtakUforetrygd = brukerVedtakUforetrygd,
        brukerRettPaaAAP = brukerRettPaaAAP,
        vurderingenGjelderFra = vurderingenGjelderFra,
        virkningsDato = virkningsDato,
        vurdertAv = bruker.ident
    )

    fun valider() {
        // TODO: Legge til sjekk på om 11-6 er oppfylt, da skal ikke denne oppfylles
    }
}
