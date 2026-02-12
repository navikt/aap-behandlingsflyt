package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangufore.flate

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.LøsningForPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangufore.OvergangUføreVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangufore.UføreSøknadVedtakResultat
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.verdityper.Bruker
import java.time.Instant
import java.time.LocalDate

data class OvergangUføreLøsningDto(
    override val begrunnelse: String,
    val brukerHarSøktOmUføretrygd: Boolean,
    val brukerHarFåttVedtakOmUføretrygd: UføreSøknadVedtakResultat?,
    val brukerRettPåAAP: Boolean?,
    override val fom: LocalDate,
    override val tom: LocalDate?,
    val overgangBegrunnelse: String?,
) : LøsningForPeriode {

    fun tilOvergangUføreVurdering(bruker: Bruker, vurdertIBehandling: BehandlingId) =
        OvergangUføreVurdering(
            begrunnelse = begrunnelse,
            brukerHarSøktOmUføretrygd = brukerHarSøktOmUføretrygd,
            brukerHarFåttVedtakOmUføretrygd = brukerHarFåttVedtakOmUføretrygd,
            brukerRettPåAAP = brukerRettPåAAP,
            vurdertIBehandling = vurdertIBehandling,
            fom = fom,
            tom = tom,
            vurdertAv = bruker.ident,
            opprettet = Instant.now()
        )
}