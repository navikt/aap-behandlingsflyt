package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.stansopphør

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import java.time.LocalDate
import java.time.Instant

interface StansOpphørVurdering {
    val fom: LocalDate
    val vurdertIBehandling: BehandlingId
    val vurdertTidspunkt: Instant
}

data class StansVurdering(
    override val fom: LocalDate,
    override val vurdertIBehandling: BehandlingId,
    override val vurdertTidspunkt: Instant,
    val årsaker: Set<Avslagsårsak>,
): StansOpphørVurdering

data class OpphørVurdering(
    override val fom: LocalDate,
    override val vurdertIBehandling: BehandlingId,
    override val vurdertTidspunkt: Instant,
    val årsaker: Set<Avslagsårsak>,
): StansOpphørVurdering

data class IkkeStansOpphørVurdering(
    override val fom: LocalDate,
    override val vurdertIBehandling: BehandlingId,
    override val vurdertTidspunkt: Instant,
): StansOpphørVurdering
