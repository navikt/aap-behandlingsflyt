package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisÅrsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.tidslinje.JoinStyle
import no.nav.aap.tidslinje.Segment
import no.nav.aap.tidslinje.StandardSammenslåere
import no.nav.aap.tidslinje.Tidslinje
import no.nav.aap.verdityper.dokument.JournalpostId
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
        val meldeperiodeTidslinje = utledMeldetidslinje(input, input.innsendingsTidspunkt)
        var nyttresultat = Tidslinje(resultat.segmenter())

        meldeperiodeTidslinje.segmenter().forEach { meldeperiode ->
            val dokumentTidslinje = Tidslinje(listOfNotNull(input.innsendingsTidspunkt.filter {
                meldeperiode.inneholder(
                    it.key
                )
            }.minOfOrNull { Segment(Periode(it.key, meldeperiode.tom()), it.value) }))

            val tidslinje = Tidslinje(listOf(meldeperiode)).kombiner(
                dokumentTidslinje,
                JoinStyle.OUTER_JOIN { periode, venstreSegment, høyreSegment ->
                    val verdi = requireNotNull(venstreSegment).verdi
                    if (høyreSegment != null) {
                        Segment(periode, MeldepliktVurdering(høyreSegment.verdi, verdi.meldeperiode, Utfall.OPPFYLT))
                    } else {
                        Segment(periode, verdi)
                    }
                })

            nyttresultat =
                nyttresultat.kombiner(tidslinje, JoinStyle.OUTER_JOIN { periode, venstreSegment, høyreSegment ->
                    var verdi = venstreSegment?.verdi ?: Vurdering()
                    if (høyreSegment != null) {
                        verdi = verdi.leggTilMeldepliktVurdering(høyreSegment.verdi)
                    }
                    Segment(periode, verdi)
                })
        }

        return nyttresultat
    }

    private fun utledMeldetidslinje(
        input: UnderveisInput,
        innsendingsTidspunkt: Map<LocalDate, JournalpostId>
    ): Tidslinje<MeldepliktVurdering> {
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
                        journalpostId = null,
                        meldeperiode = Periode(rettighetsperiode.fom, rettighetsperiode.fom.plusDays(13)),
                        utfall = Utfall.OPPFYLT
                    )
                )
            )
        )
        var forrigeSegmentStanset = false
        if (rettighetsperiode.fom.plusDays(13).isBefore(rettighetsperiode.tom)) {
            tidslinje = tidslinje.kombiner(
                dummy.splittOppOgMapOmEtter(
                    rettighetsperiode.fom.plusDays(13),
                    rettighetsperiode.tom,
                    Period.ofDays(14)
                ) { meldesegment ->
                    val segment = utledSegmentForPeriode(meldesegment, innsendingsTidspunkt, forrigeSegmentStanset)

                    forrigeSegmentStanset = segment.first().verdi.utfall == Utfall.IKKE_OPPFYLT

                    segment
                }, StandardSammenslåere.prioriterHøyreSideCrossJoin()
            )
        }

        return tidslinje
    }

    private fun utledSegmentForPeriode(
        it: NavigableSet<Segment<Boolean>>,
        innsendingsTidspunkt: Map<LocalDate, JournalpostId>,
        forrigeSegmentStanset: Boolean
    ): TreeSet<Segment<MeldepliktVurdering>> {

        val meldeperiode = it.first().periode

        val dokumentTidslinje = Tidslinje(listOfNotNull(innsendingsTidspunkt.filter {
            meldeperiode.inneholder(
                it.key
            )
        }.minOfOrNull { Segment(Periode(it.key, meldeperiode.tom), it.value) }))

        return TreeSet(
            listOf(
                Segment(
                    meldeperiode,
                    MeldepliktVurdering(
                        journalpostId = null,
                        meldeperiode = Periode(
                            meldeperiode.fom.plusDays(1),
                            meldeperiode.fom.plusDays(14)
                        ),
                        utfall = utledUtfall(meldeperiode, forrigeSegmentStanset, dokumentTidslinje),
                        årsak = utledÅrsak(meldeperiode.tom)
                    )
                )
            )
        )
    }

    private fun utledUtfall(
        periode: Periode,
        forrigeSegmentStanset: Boolean,
        dokumentTidslinje: Tidslinje<JournalpostId>
    ): Utfall {
        val skalFortsattVæreStanset = forrigeSegmentStanset && dokumentTidslinje.segmenter().any { dokument -> true }
        if (periode.fom.isBefore(LocalDate.now()) || skalFortsattVæreStanset) {
            return Utfall.IKKE_OPPFYLT
        }
        return Utfall.OPPFYLT
    }

    private fun utledÅrsak(tom: LocalDate): UnderveisÅrsak {
        if (tom.isBefore(LocalDate.now())) {
            return UnderveisÅrsak.IKKE_OVERHOLDT_MELDEPLIKT_SANKSJON
        }
        return UnderveisÅrsak.MELDEPLIKT_FRIST_IKKE_PASSERT
    }
}