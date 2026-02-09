package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangufore

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import java.time.Instant
import java.time.LocalDate

data class OvergangUføreVurdering(
    val begrunnelse: String,
    val brukerHarSøktOmUføretrygd: Boolean,
    val brukerHarFåttVedtakOmUføretrygd: UføreSøknadVedtakResultat?,
    val brukerRettPåAAP: Boolean?,
    val fom: LocalDate,
    val tom: LocalDate?,
    val vurdertAv: String,
    val vurdertIBehandling: BehandlingId,
    val opprettet: Instant? = null
) {
    fun harRettPåAAPMedOvergangUføre(): Boolean {
        return brukerHarSøktOmUføretrygd
                && brukerHarFåttVedtakOmUføretrygd == UføreSøknadVedtakResultat.NEI
                && brukerRettPåAAP == true
    }
}

enum class UføreSøknadVedtakResultat(val verdi: String) {
    JA_AVSLAG("JA_AVSLAG"), JA_INNVILGET_GRADERT("JA_INNVILGET_GRADERT"), JA_INNVILGET_FULL("JA_INNVILGET_FULL"), NEI("NEI")
}

