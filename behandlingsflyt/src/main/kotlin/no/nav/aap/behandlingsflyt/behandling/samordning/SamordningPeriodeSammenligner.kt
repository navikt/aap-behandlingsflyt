package no.nav.aap.behandlingsflyt.behandling.samordning

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelse
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelsePeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelseRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Prosent

data class SamordningYtelseMedEndring(
    val ytelseType: Ytelse,
    val kilde: String,
    val saksRef: String? = null,
    val periode: Periode,
    val gradering: Prosent?,
    val kronesum: Number? = null,
    val endringStatus: EndringStatus,
) {
    companion object {
        fun konstruer(
            ytelsePar: Pair<SamordningYtelse, SamordningYtelsePeriode>, endringStatus: EndringStatus
        ): SamordningYtelseMedEndring {
            val (ytelse, periode) = ytelsePar
            return SamordningYtelseMedEndring(
                ytelseType = ytelse.ytelseType,
                kilde = ytelse.kilde,
                saksRef = ytelse.saksRef,
                periode = periode.periode,
                gradering = periode.gradering,
                kronesum = periode.kronesum,
                endringStatus = endringStatus
            )
        }
    }
}


class SamordningPeriodeSammenligner(private val samordningYtelseRepository: SamordningYtelseRepository) {

    fun hentPerioderMarkertMedEndringer(behandlingId: BehandlingId): List<SamordningYtelseMedEndring> {
        val forrige = samordningYtelseRepository.hentHvisEksisterer(behandlingId)

        val eldste = samordningYtelseRepository.hentEldsteGrunnlag(behandlingId)

        if (forrige == null && eldste == null) return emptyList()

        if (forrige != null && forrige.grunnlagId == eldste?.grunnlagId) {
            return forrige.ytelser.flatMap {
                it.ytelsePerioder.map { periode ->
                    SamordningYtelseMedEndring.konstruer(
                        Pair(it, periode),
                        EndringStatus.NY
                    )
                }
            }
        }

        val nyeYtelser = forrige?.ytelser.orEmpty().associateBy { it.ytelseType }
            .mapValues { Pair(it.value, it.value.ytelsePerioder) }

        val eldsteYtelser = eldste?.ytelser.orEmpty().associateBy { it.ytelseType }
            .mapValues { Pair(it.value, it.value.ytelsePerioder) }

        // Som kun er i listen av nye
        val nyeSammenlignetMedEldre = nyeYtelser.mapValues { (ytelseType, nyePerioder) ->
            val eldre = eldsteYtelser[ytelseType]?.second ?: return@mapValues nyePerioder

            Pair(nyePerioder.first, nyePerioder.second.filterNot { it in eldre })
        }
            .tilSamordningYtelseMedEndring(EndringStatus.NY)

        // Som er til stede i både gamle og nye
        val uendrede = nyeYtelser.mapValues { (ytelseType, nyePerioder) ->
            val eldre = eldsteYtelser[ytelseType]?.second
                ?: return@mapValues Pair(nyePerioder.first, emptyList<SamordningYtelsePeriode>())

            Pair(nyePerioder.first, nyePerioder.second.filter { it in eldre })
        }
            .tilSamordningYtelseMedEndring(EndringStatus.UENDRET)

        // Som bare er i gamle, ikke nye
        val slettede = eldsteYtelser.mapValues { (ytelseType, eldstePerioder) ->
            val nye = nyeYtelser[ytelseType]?.second
                ?: return@mapValues Pair(eldstePerioder.first, emptyList<SamordningYtelsePeriode>())

            Pair(eldstePerioder.first, eldstePerioder.second.filterNot { it in nye })
        }
            .tilSamordningYtelseMedEndring(EndringStatus.SLETTET)

        return nyeSammenlignetMedEldre + uendrede + slettede
    }

    private fun Map<Ytelse, Pair<SamordningYtelse, Collection<SamordningYtelsePeriode>>>.tilSamordningYtelseMedEndring(
        endringStatus: EndringStatus
    ): List<SamordningYtelseMedEndring> {
        return this.flatMap { par -> par.value.second.map { Pair(par.value.first, it) } }
            .map { SamordningYtelseMedEndring.konstruer(it, endringStatus) }
    }
}