package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangufore

import no.nav.aap.behandlingsflyt.behandling.vilkår.overganguføre.OvergangUføreVilkår.UføreSøknadVedtak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import java.time.Instant
import java.time.LocalDate

data class OvergangUføreVurdering(
    val begrunnelse: String,
    val brukerHarSøktOmUføretrygd: Boolean,
    val brukerHarFåttVedtakOmUføretrygd: String?,
    val brukerRettPåAAP: Boolean?,
    val fom: LocalDate?,
    val tom: LocalDate?,
    val vurdertAv: String,
    val vurdertIBehandling: BehandlingId? = null, // TODO: Gjør required etter migrering
    val opprettet: Instant? = null
) {
    fun harRettPåAAPMedOvergangUføre(): Boolean {
        return fom != null // TODO: Fjern denne - fom skal også settes for nei-svar
                && brukerHarSøktOmUføretrygd
                && brukerHarFåttVedtakOmUføretrygd == UføreSøknadVedtak.NEI.verdi
                && brukerRettPåAAP == true
    }
}

