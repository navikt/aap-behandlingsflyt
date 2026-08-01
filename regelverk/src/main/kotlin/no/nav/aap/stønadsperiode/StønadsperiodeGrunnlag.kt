package no.nav.aap.stønadsperiode

import java.time.Instant
import java.time.LocalDate
import no.nav.aap.behandling.BehandlingId
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.krav.Kravreferanse
import no.nav.aap.misc.VurderingForKrav
import no.nav.aap.misc.VurderingForKravGrunnlag

data class StønadsperiodeGrunnlag(
    override val vurderinger: Set<StønadsperiodeVurdering>
) : VurderingForKravGrunnlag<StønadsperiodeVurdering>

data class StønadsperiodeVurdering(
    override val referanse: Kravreferanse,
    override val opprettet: Instant,
    override val vurdertIBehandling: BehandlingId,
    override val vurdertAv: Bruker,

    val begrunnelse: String,
    val harHattOrdinærSiste52Uker: Boolean,
    val harGjenværendeKvote: Boolean,
    val relevantKravType: RelevantKravType,
    val startDato: LocalDate,
) : VurderingForKrav {
    init {
        when (relevantKravType) {
            RelevantKravType.NY_STØNADSPERIODE -> require(
                !harGjenværendeKvote && !harHattOrdinærSiste52Uker
            )

            RelevantKravType.GJENOPPTAK_ETTER_STANS, RelevantKravType.GJENINNTREDEN_ETTER_OPPHØR -> require(
                harGjenværendeKvote || harHattOrdinærSiste52Uker
            )

            RelevantKravType.AVSLAG -> {}
        }
    }
}

enum class RelevantKravType {
    GJENOPPTAK_ETTER_STANS,
    GJENINNTREDEN_ETTER_OPPHØR,
    NY_STØNADSPERIODE,
    AVSLAG
}