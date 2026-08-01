package no.nav.aap.behandlingsflyt.steg.overgangufore

import java.time.Instant
import java.time.LocalDate
import no.nav.aap.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.avklaringsbehov.løsning.LøsningForPeriode
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.overganguføre.OvergangUføreVurdering
import no.nav.aap.overganguføre.UføreSøknadVedtakResultat

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
            vurdertAv = bruker,
            opprettet = Instant.now()
        )
}