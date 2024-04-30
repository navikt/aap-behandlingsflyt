package no.nav.aap.behandlingsflyt.underveis.regler

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisAvslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.tidslinje.JoinStyle
import no.nav.aap.tidslinje.Segment
import no.nav.aap.tidslinje.StandardSammenslåere
import no.nav.aap.tidslinje.Tidslinje
import no.nav.aap.verdityper.Periode
import java.time.LocalDate
import java.time.Period
import java.util.*

/**
 * Aktivitetskravene
 *
 * - MP
 * - Fravær
 *   - Aktivitet
 *   - etc
 */
class AktivitetRegel : UnderveisRegel {
    override fun vurder(input: UnderveisInput, resultat: Tidslinje<Vurdering>): Tidslinje<Vurdering> {

        val nyttresultat = håndterMeldeplikt(resultat, input)

        return nyttresultat
    }

    private fun håndterMeldeplikt(
        resultat: Tidslinje<Vurdering>,
        input: UnderveisInput
    ): Tidslinje<Vurdering> {
        val meldeperiodeTidslinje = utledMeldetidslinje(input)
        var nyttresultat = Tidslinje(resultat.segmenter())

        meldeperiodeTidslinje.segmenter().forEach { meldeperiode ->
            val dokumentTidslinje = Tidslinje(listOfNotNull(input.innsendingsTidspunkt.filter {
                meldeperiode.inneholder(
                    it.key
                )
            }.minOfOrNull { Segment(Periode(it.key, meldeperiode.tom()), it.value) }))
            val tidslinje = Tidslinje(listOf(meldeperiode)).kombiner(
                dokumentTidslinje,
                JoinStyle.CROSS_JOIN { periode, venstreSegment, høyreSegment ->
                    val verdi = requireNotNull(venstreSegment).verdi
                    if (høyreSegment != null) {
                        Segment(periode, MeldepliktVurdering(verdi.meldeperiode, Utfall.OPPFYLT))
                    } else {
                        Segment(periode, verdi)
                    }
                })

            nyttresultat =
                nyttresultat.kombiner(tidslinje, JoinStyle.CROSS_JOIN { periode, venstreSegment, høyreSegment ->
                    var verdi = venstreSegment?.verdi ?: Vurdering()
                    if (høyreSegment != null) {
                        verdi = verdi.leggTilMeldepliktVurdering(høyreSegment.verdi)
                    }
                    Segment(periode, verdi)
                })
        }

        return nyttresultat
    }

    private fun utledMeldetidslinje(input: UnderveisInput): Tidslinje<MeldepliktVurdering> {
        val rettighetsperiode = input.rettighetsperiode
        val dummy = Tidslinje(rettighetsperiode, true)
        var tidslinje = Tidslinje(
            listOf(
                Segment(
                    Periode(
                        rettighetsperiode.fom,
                        rettighetsperiode.fom.plusDays(12)
                    ),
                    MeldepliktVurdering(
                        meldeperiode = Periode(rettighetsperiode.fom, rettighetsperiode.fom.plusDays(13)),
                        utfall = Utfall.OPPFYLT
                    )
                )
            )
        )
        if (rettighetsperiode.fom.plusDays(13).isBefore(rettighetsperiode.tom)) {
            tidslinje = tidslinje.kombiner(
                dummy.splittOppOgMapOmEtter(
                    rettighetsperiode.fom.plusDays(13),
                    rettighetsperiode.tom,
                    Period.ofDays(14)
                ) {
                    TreeSet(
                        listOf(
                            Segment(
                                it.first().periode,
                                MeldepliktVurdering(
                                    meldeperiode = Periode(
                                        it.first().periode.fom.plusDays(1),
                                        it.first().periode.fom.plusDays(14)
                                    ),
                                    utfall = Utfall.IKKE_OPPFYLT,
                                    avslagsårsak = utledÅrsak(it.first().periode.tom)
                                )
                            )
                        )
                    )
                }, StandardSammenslåere.prioriterHøyreSideCrossJoin()
            )
        }

        return tidslinje
    }

    private fun utledÅrsak(tom: LocalDate): UnderveisAvslagsårsak {
        if (tom.isBefore(LocalDate.now())) {
            return UnderveisAvslagsårsak.IKKE_OVERHOLDT_MELDEPLIKT_SANKSJON
        }
        return UnderveisAvslagsårsak.MELDEPLIKT_FRIST_IKKE_PASSERT
    }
}