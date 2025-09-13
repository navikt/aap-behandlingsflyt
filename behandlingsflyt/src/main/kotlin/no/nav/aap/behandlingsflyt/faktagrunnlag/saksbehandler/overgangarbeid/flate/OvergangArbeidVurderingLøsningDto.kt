package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangarbeid.flate

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.LøsningForPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangarbeid.OvergangArbeidVurdering
import java.time.Instant
import java.time.LocalDate

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
            vurderingenGjelderFra = fom,
            vurdertAv = avklaringsbehovKontekst.bruker.ident,
            opprettet = Instant.now(),
            vurdertIBehandling = avklaringsbehovKontekst.behandlingId(),
            vurderingenGjelderTil = tom,
        )
    }
}