package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangufore.flate

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangufore.OvergangUføreVurdering
import no.nav.aap.komponenter.verdityper.Bruker
import java.time.LocalDate

data class OvergangUføreVurderingLøsningDto(
    val begrunnelse: String,
    val brukerHarSøktOmUføretrygd: Boolean,
    val brukerHarFåttVedtakOmUføretrygd: String?,
    val brukerRettPåAAP: Boolean?,
    val virkningsdato: LocalDate?,
    val overgangBegrunnelse: String?,
) {
    fun tilOvergangUføreVurdering(bruker: Bruker) = OvergangUføreVurdering(
        begrunnelse = begrunnelse,
        brukerHarSøktOmUføretrygd = brukerHarSøktOmUføretrygd,
        brukerHarFåttVedtakOmUføretrygd = brukerHarFåttVedtakOmUføretrygd,
        brukerRettPåAAP = brukerRettPåAAP,
        virkningsdato = virkningsdato,
        vurdertAv = bruker.ident
    )
}