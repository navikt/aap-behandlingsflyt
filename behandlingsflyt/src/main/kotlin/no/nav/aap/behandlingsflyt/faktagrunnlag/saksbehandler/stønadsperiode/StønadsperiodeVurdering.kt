package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.stønadsperiode

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.VurderingForKrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.VurderingForKravGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.Kravreferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.verdityper.Bruker
import java.time.Instant

data class StønadsperiodeGrunnlag(
    override val vurderinger: List<StønadsperiodeVurdering>
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