package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.stønadsperiode

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.VurderingForKrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.VurderingForKravGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.KravGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.Kravreferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.somTidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.komponenter.verdityper.Tid
import java.time.Instant
import java.time.LocalDate

data class StønadsperiodeGrunnlag(
    override val vurderinger: Set<StønadsperiodeVurdering>
) : VurderingForKravGrunnlag<StønadsperiodeVurdering> {
    override fun tilTidslinje(kravGrunnlag: KravGrunnlag): Tidslinje<StønadsperiodeVurdering> {
        val gjeldendeKravreferanser = kravGrunnlag.gjeldendeRelevanteKrav().map { it.referanse }

        return gjeldendeVurderinger()
            .filter { stønadsperiodeVurdering -> stønadsperiodeVurdering.referanse in gjeldendeKravreferanser }
            .sortedBy { it.startDato }
            .somTidslinje { Periode(it.startDato, Tid.MAKS) }
    }
}

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

        /// harGjenværendeKvote | harHattOrdinærSiste52Uker
        /// true                                | true                                              | GJENINNTREKDEN/GJENOPPTAK
        /// true                                | false                                             | NY_STØNADSPERIODE
        /// false                                | true                                             | NY_STØNADSPERIODE
        /// false                                | false                                             | NY_STØNADSPERIODE


        when (relevantKravType) {
            RelevantKravType.NY_STØNADSPERIODE -> require(
                !(harGjenværendeKvote && harHattOrdinærSiste52Uker)
            )

            is RelevantKravType.GJENOPPTAK_ETTER_STANS,
            RelevantKravType.GJENINNTREDEN_ETTER_OPPHØR -> require(
                harGjenværendeKvote && harHattOrdinærSiste52Uker
            )

            RelevantKravType.AVSLAG -> {}
        }
    }
}

sealed interface RelevantKravType {
    data class GJENOPPTAK_ETTER_STANS(
        val gjennopptakEtter: List<Avslagsårsak>,
    ): RelevantKravType

    data object GJENINNTREDEN_ETTER_OPPHØR: RelevantKravType
    data object NY_STØNADSPERIODE: RelevantKravType
    data object AVSLAG: RelevantKravType
}

