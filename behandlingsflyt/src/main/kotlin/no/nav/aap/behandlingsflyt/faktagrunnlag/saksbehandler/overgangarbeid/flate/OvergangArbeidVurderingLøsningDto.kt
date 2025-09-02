package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangarbeid.flate


import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangarbeid.OvergangArbeidVurdering
import no.nav.aap.komponenter.verdityper.Bruker
import java.time.LocalDate

data class OvergangArbeidVurderingLøsningDto(
    val begrunnelse: String,
    val brukerRettPaaAAP: Boolean?,
    val virkningsDato: LocalDate?,
    val overgangBegrunnelse: String?,
) {
    fun tilOvergangArbeidVurdering(bruker: Bruker, vurderingenGjelderFra: LocalDate?) = OvergangArbeidVurdering(
        begrunnelse = begrunnelse,
        brukerRettPaaAAP = brukerRettPaaAAP,
        vurderingenGjelderFra = vurderingenGjelderFra,
        virkningsDato = virkningsDato,
        vurdertAv = bruker.ident
    )
}