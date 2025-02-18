package no.nav.aap.behandlingsflyt.behandling.samordning

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelseVurderingGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelseVurderingRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.tidslinje.JoinStyle
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.verdityper.Prosent
import kotlin.math.max
import kotlin.math.min

class SamordningService(
    private val samordningYtelseVurderingRepository: SamordningYtelseVurderingRepository,
    private val underveisRepository: UnderveisRepository,
) {
    fun vurder(behandlingId: BehandlingId): Tidslinje<SamordningGradering> {
        val samordningYtelseVurderingGrunnlag = samordningYtelseVurderingRepository.hentHvisEksisterer(behandlingId)

        //TODO: Kan benytte denne til å filtrere perioder hvor det ikke er rett på ytelse uansett
        val underveisPerioder = underveisRepository.hentHvisEksisterer(behandlingId)

        if (samordningYtelseVurderingGrunnlag == null) {
            return Tidslinje(emptyList())
        }
        val vurderRegler = vurderRegler(samordningYtelseVurderingGrunnlag)

        return vurderRegler
    }

    // TODO: håndter tilfellet at det kommer ny info
    fun harGjortVurdering(behandlingId: BehandlingId): Boolean {
        val samordningYtelseVurderingGrunnlag = samordningYtelseVurderingRepository.hentHvisEksisterer(behandlingId)

        return samordningYtelseVurderingGrunnlag?.vurderingerId != null && samordningYtelseVurderingGrunnlag.vurderinger.isNotEmpty()
    }

    private fun vurderRegler(samordning: SamordningYtelseVurderingGrunnlag): Tidslinje<SamordningGradering> {
        val ytelserByType = samordning.ytelser.associateBy { it.ytelseType }
            .mapValues { (_, vurdering) -> vurdering.ytelsePerioder.map { Segment(it.periode, it) } }
            .mapValues { (_, vurdering) -> Tidslinje(vurdering) }
            .mapValues { (ytelse, tidslinje) ->
                tidslinje.mapValue {
                    YtelseGradering(
                        ytelse,
                        it.gradering!!
                    )
                }
            }

        val vurderingerByType = samordning.vurderinger.associateBy { it.ytelseType }
            .mapValues { (_, vurdering) -> vurdering.vurderingPerioder.map { Segment(it.periode, it) } }
            .mapValues { (_, vurdering) -> Tidslinje(vurdering) }
            .mapValues { (ytelse, tidslinje) ->
                tidslinje.mapValue {
                    YtelseGradering(
                        ytelse,
                        it.gradering!!
                    )
                }
            }

        var other = joinGraderingTilTidslinje(vurderingerByType)
        other = joinYtelseGraderingerTilTidslinje(ytelserByType, other)
        return other
    }

    private fun joinYtelseGraderingerTilTidslinje(
        ytelserByType: Map<Ytelse, Tidslinje<YtelseGradering>>,
        other: Tidslinje<SamordningGradering>
    ): Tidslinje<SamordningGradering> {
        var other1 = other
        ytelserByType.forEach { (ytelseType, ytelseGradering) ->
            other1 = other1.kombiner(ytelseGradering, JoinStyle.RIGHT_JOIN { periode, venstre, høyre ->
                if (venstre == null) {
                    Segment(
                        periode, SamordningGradering(
                            gradering = høyre.verdi.gradering,
                            ytelsesGraderinger = listOf(
                                YtelseGradering(
                                    ytelse = høyre.verdi.ytelse,
                                    gradering = høyre.verdi.gradering,
                                )
                            )
                        )
                    )
                } else {
                    // TODO: eller må vi håndtere case-by-case?
                    require(ytelseType == høyre.verdi.ytelse) { "Vi joiner på samme ytelse" }

                    Segment(
                        periode, SamordningGradering(
                            gradering = Prosent(
                                min(
                                    100,
                                    høyre.verdi.gradering.prosentverdi() + venstre.verdi.gradering.prosentverdi()
                                )
                            ),
                            ytelsesGraderinger = venstre.verdi.ytelsesGraderinger // TODO ...
                        )
                    )
                }
            })
        }
        return other1
    }

    private fun joinGraderingTilTidslinje(vurderingerByType: Map<Ytelse, Tidslinje<YtelseGradering>>): Tidslinje<SamordningGradering> {
        var other = Tidslinje(emptyList<Segment<SamordningGradering>>())
        vurderingerByType.forEach { (ytelseType, vurdering) ->
            other = other.kombiner(vurdering, JoinStyle.RIGHT_JOIN { periode, venstre, høyre ->
                if (venstre == null) {
                    Segment(
                        periode, SamordningGradering(
                            gradering = høyre.verdi.gradering,
                            ytelsesGraderinger = listOf(
                                YtelseGradering(
                                    ytelse = høyre.verdi.ytelse,
                                    gradering = høyre.verdi.gradering,
                                )
                            )
                        )
                    )
                } else {
                    // TODO!!!! summer på et vis??
                    val nyListe = venstre.verdi.ytelsesGraderinger + høyre.verdi
                    val nyGradering = venstre.verdi.gradering.prosentverdi() + høyre.verdi.gradering.prosentverdi()
                    Segment(
                        periode,
                        SamordningGradering(
                            gradering = Prosent(nyGradering),
                            ytelsesGraderinger = nyListe
                        )
                    )
                }
            })
        }
        return other
    }
}