package no.nav.aap.behandlingsflyt.steg.overgangarbeid

import java.time.Instant
import java.time.LocalDate
import no.nav.aap.behandlingsflyt.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.avklaringsbehov.løsning.LøsningForPeriode
import no.nav.aap.overgangarbeid.OvergangArbeidVurdering

data class OvergangArbeidVurderingLøsningDto(
    override val begrunnelse: String,
    override val fom: LocalDate,
    override val tom: LocalDate?,
    val brukerRettPåAAP: Boolean,
): LøsningForPeriode {
    fun tilOvergangArbeidVurdering(avklaringsbehovKontekst: AvklaringsbehovKontekst): OvergangArbeidVurdering {
        return OvergangArbeidVurdering(
            begrunnelse = begrunnelse,
            brukerRettPåAAP = brukerRettPåAAP,
            fom = fom,
            vurdertAv = avklaringsbehovKontekst.bruker,
            opprettet = Instant.now(),
            vurdertIBehandling = avklaringsbehovKontekst.behandlingId(),
            tom = tom,
        )
    }
}