package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.stønadsperiode

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
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

            RelevantKravType.MIGRERT_STØNADSPERIODE, RelevantKravType.AVSLAG -> {}
        }
    }
}

enum class RelevantKravTypeNavn {
    GJENOPPTAK_ETTER_STANS,
    GJENINNTREDEN_ETTER_OPPHØR,
    NY_STØNADSPERIODE,
    AVSLAG,
    MIGRERT_STØNADSPERIODE,
}

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type",
    visible = true,
)
sealed interface RelevantKravType {
    val type: RelevantKravTypeNavn

    @JsonTypeName("GJENOPPTAK_ETTER_STANS")
    data class GJENOPPTAK_ETTER_STANS(
        val gjennopptakEtter: List<Avslagsårsak>,
    ) : RelevantKravType {
        override val type = RelevantKravTypeNavn.GJENOPPTAK_ETTER_STANS
    }

    @JsonTypeName("GJENINNTREDEN_ETTER_OPPHØR")
    data object GJENINNTREDEN_ETTER_OPPHØR : RelevantKravType {
        override val type = RelevantKravTypeNavn.GJENINNTREDEN_ETTER_OPPHØR
    }

    @JsonTypeName("NY_STØNADSPERIODE")
    data object NY_STØNADSPERIODE : RelevantKravType {
        override val type = RelevantKravTypeNavn.NY_STØNADSPERIODE
    }

    @JsonTypeName("MIGRERT_STØNADSPERIODE")
    data object MIGRERT_STØNADSPERIODE : RelevantKravType {
        override val type = RelevantKravTypeNavn.MIGRERT_STØNADSPERIODE
    }

    @JsonTypeName("AVSLAG")
    data object AVSLAG : RelevantKravType {
        override val type = RelevantKravTypeNavn.AVSLAG
    }
}

