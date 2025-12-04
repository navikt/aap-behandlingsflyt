package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangufore.flate

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangufore.OvergangUføreVurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.verdityper.Bruker
import java.time.LocalDate

data class OvergangUføreVurderingLøsningDto(
    val begrunnelse: String,
    val brukerHarSøktOmUføretrygd: Boolean,
    val brukerHarFåttVedtakOmUføretrygd: String?,
    val brukerRettPåAAP: Boolean?,
    @Deprecated("Bruk fom")
    val virkningsdato: LocalDate?,
    val fom: LocalDate?, // TODO: Gjør required etter at frontend alltid sender med
    val overgangBegrunnelse: String?,
) {
    fun tilOvergangUføreVurdering(bruker: Bruker, vurdertIBehandling: BehandlingId) = OvergangUføreVurdering(
        begrunnelse = begrunnelse,
        brukerHarSøktOmUføretrygd = brukerHarSøktOmUføretrygd,
        brukerHarFåttVedtakOmUføretrygd = brukerHarFåttVedtakOmUføretrygd,
        brukerRettPåAAP = brukerRettPåAAP,
        vurdertIBehandling = vurdertIBehandling,
        fom = fom ?: virkningsdato,
        vurdertAv = bruker.ident
    )
}