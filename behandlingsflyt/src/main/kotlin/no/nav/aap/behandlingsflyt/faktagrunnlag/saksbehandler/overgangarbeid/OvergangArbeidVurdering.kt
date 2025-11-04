package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangarbeid

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import java.time.Instant
import java.time.LocalDate

data class OvergangArbeidVurdering(
    val begrunnelse: String,
    val brukerRettPåAAP: Boolean,
    val vurdertAv: String,
    val vurderingenGjelderFra: LocalDate,
    val vurderingenGjelderTil: LocalDate?,
    val opprettet: Instant,
    val vurdertIBehandling: BehandlingId,
)