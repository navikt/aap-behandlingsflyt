package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangarbeid.flate

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.LøsningForPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangarbeid.OvergangArbeidVurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.verdityper.Bruker
import java.time.Instant
import java.time.LocalDate

data class OvergangArbeidVurderingLøsningDto(
    override val begrunnelse: String,
    override val fom: LocalDate,
    override val tom: LocalDate?,
    val brukerRettPåAAP: Boolean,
): LøsningForPeriode {
    fun tilOvergangArbeidVurdering(bruker: Bruker, vurdertIBehandling: BehandlingId): OvergangArbeidVurdering {
        return OvergangArbeidVurdering(
            begrunnelse = begrunnelse,
            brukerRettPåAAP = brukerRettPåAAP,
            fom = fom,
            vurdertAv = bruker,
            opprettet = Instant.now(),
            vurdertIBehandling = vurdertIBehandling,
            tom = tom,
        )
    }
}