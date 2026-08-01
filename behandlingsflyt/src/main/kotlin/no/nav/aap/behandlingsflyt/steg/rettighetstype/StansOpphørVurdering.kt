package no.nav.aap.behandlingsflyt.steg.rettighetstype

import no.nav.aap.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandling.BehandlingId
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
