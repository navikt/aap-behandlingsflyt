package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangufore

import no.nav.aap.behandlingsflyt.behandling.vilkår.overganguføre.OvergangUføreVilkår.UføreSøknadVedtak
import java.time.Instant
import java.time.LocalDate

data class OvergangUføreVurdering(
    val begrunnelse: String,
    val brukerHarSøktOmUføretrygd: Boolean,
    val brukerHarFåttVedtakOmUføretrygd: String?,
    val brukerRettPåAAP: Boolean?,
    val virkningsdato: LocalDate?,
    val vurdertAv: String,
    val vurderingenGjelderFra: LocalDate?,
    val opprettet: Instant? = null
) {
    fun harRettPåAAPMedOvergangUføre(): Boolean {
        return virkningsdato != null
                && brukerHarSøktOmUføretrygd
                && brukerHarFåttVedtakOmUføretrygd == UføreSøknadVedtak.NEI.verdi
                && brukerRettPåAAP == true
    }
}

