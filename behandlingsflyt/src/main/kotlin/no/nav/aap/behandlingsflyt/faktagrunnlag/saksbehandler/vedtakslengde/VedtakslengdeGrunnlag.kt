package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.vedtakslengde

import no.nav.aap.behandlingsflyt.SYSTEMBRUKER
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.ÅrMedHverdager
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Bruker
import java.time.Instant
import java.time.LocalDate

data class VedtakslengdeGrunnlag(
    val vurderinger: List<VedtakslengdeVurdering>
) {
    fun gjeldendeVurdering(): VedtakslengdeVurdering? {
        // Foretrekker manuell fremfor automatisk - i siste behandling
        val sisteBehandlingId = vurderinger.maxByOrNull { it.opprettet }?.vurdertIBehandling ?: return null
        val vurderingerISisteBehandling = vurderinger.filter { it.vurdertIBehandling == sisteBehandlingId }
        return vurderingerISisteBehandling.filter { it.vurdertManuelt }.maxByOrNull { it.opprettet }
            ?: vurderingerISisteBehandling.maxByOrNull { it.opprettet }
    }

    fun gjeldendeVurderinger(fraDato: LocalDate): Tidslinje<VedtakslengdeVurdering> {
        if (vurderinger.isEmpty()) return Tidslinje.empty()

        val sortert = vurderinger
            .groupBy { it.vurdertIBehandling }
            .mapValues { (_, v) ->
                // Hvis manuell finnes, prioriteres denne
                v.filter { it.vurdertManuelt }.maxByOrNull { it.opprettet }
                    ?: v.maxBy { it.opprettet }
            }
            .values
            .sortedBy { it.opprettet }

        // Utleder fom fra sluttdato til forrige vurdering, eller fraDato for første vurdering
        val segmenter = sortert.mapIndexed { index, vurdering ->
            val fom = if (index == 0) fraDato else sortert[index - 1].sluttdato.plusDays(1)
            Segment(Periode(fom, vurdering.sluttdato), vurdering)
        }

        return Tidslinje(segmenter)
    }
}

data class VedtakslengdeVurdering(
    val sluttdato: LocalDate,
    val utvidetMed: ÅrMedHverdager,
    val årsaker: List<VedtakslengdeÅrsak>,
    val vurdertAv: Bruker,
    val vurdertIBehandling: BehandlingId,
    val opprettet: Instant,
    val begrunnelse: String,
) {
    val vurdertManuelt: Boolean get() = !vurdertAutomatisk
    val vurdertAutomatisk: Boolean get() = vurdertAv == SYSTEMBRUKER
}

enum class VedtakslengdeÅrsak {
    MAKS_ETT_ÅR,
    BRUKER_OVER_67,
    IKKE_MEDLEM,
    ORDINÆRKVOTE_BRUKT_OPP,
    BRUDD_PÅ_OPPHOLDSKRAV_STANS,
    IKKE_RETT_UNDER_STRAFFEGJENNOMFØRING,
    ANNEN_FULL_YTELSE;

    companion object {
        fun fraAvslagsårsak(avslagsårsak: Avslagsårsak): VedtakslengdeÅrsak? {
            return when (avslagsårsak) {
                Avslagsårsak.BRUKER_OVER_67 -> BRUKER_OVER_67
                Avslagsårsak.IKKE_MEDLEM -> IKKE_MEDLEM
                Avslagsårsak.ORDINÆRKVOTE_BRUKT_OPP -> ORDINÆRKVOTE_BRUKT_OPP
                Avslagsårsak.BRUDD_PÅ_OPPHOLDSKRAV_STANS -> BRUDD_PÅ_OPPHOLDSKRAV_STANS
                Avslagsårsak.IKKE_RETT_UNDER_STRAFFEGJENNOMFØRING -> IKKE_RETT_UNDER_STRAFFEGJENNOMFØRING
                Avslagsårsak.ANNEN_FULL_YTELSE -> ANNEN_FULL_YTELSE
                else -> null
            }
        }
    }
}