package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.behandlingsflyt.behandling.underveis.regler.UtledMeldeperiodeRegel.Companion.groupByMeldeperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisÅrsak.IKKE_OVERHOLDT_MELDEPLIKT_SANKSJON
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisÅrsak.MELDEPLIKT_FRIST_IKKE_PASSERT
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall.IKKE_OPPFYLT
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall.OPPFYLT
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.Fritaksvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.Fritaksvurdering.Companion.tidslinje
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

//TODO - REFACTOR!!!!
class MeldepliktRegel : UnderveisRegel {
    class MeldepliktData(val fritaksvurdering: Fritaksvurdering.FritaksvurderingData?, val innsending: JournalpostId?)

    override fun vurder(input: UnderveisInput, resultat: Tidslinje<Vurdering>): Tidslinje<Vurdering> {
        val defaultTidslinje = Tidslinje<JournalpostId?>(input.rettighetsperiode, null)

        val innsendtTidslinje: Tidslinje<JournalpostId?> = input.innsendingsTidspunkt.entries
            .map {
                Segment<JournalpostId?>(
                    Periode(it.key, it.key),
                    it.value
                )
            }
            .let { Tidslinje(it) }

        val dokumentTidslinje: Tidslinje<JournalpostId?> = defaultTidslinje.kombiner(
            innsendtTidslinje,
            StandardSammenslåere.prioriterHøyreSideCrossJoin()
        )

        val fritaksTidslinje = input.meldepliktGrunnlag.vurderinger.tidslinje()

        val meldepliktTidslinje =
            fritaksTidslinje.kombiner(dokumentTidslinje, JoinStyle.OUTER_JOIN { periode, fritaksvurdering, dokument ->
                Segment(periode, MeldepliktData(fritaksvurdering?.verdi, dokument?.verdi))
            })

        val meldepliktVurderinger = groupByMeldeperiode(resultat, meldepliktTidslinje)
            .fold(Tidslinje<MeldepliktVurdering>()) { meldeperioderVurdert, nåværendeMeldeperiodeSegment ->
                meldeperioderVurdert.flatMap {
                    vurderForMeldeperiode(
                        meldeperiode = nåværendeMeldeperiodeSegment.periode,
                        dataForMeldeperiode = nåværendeMeldeperiodeSegment.verdi,
                        forrigeSegmentOppfylt = meldeperioderVurdert.lastOrNull()?.verdi?.utfall == OPPFYLT
                    )
                }
            }

        return resultat.leggTilVurderinger(meldepliktVurderinger, Vurdering::leggTilMeldepliktVurdering)
    }

    private fun vurderForMeldeperiode(
        meldeperiode: Periode,
        dataForMeldeperiode: Tidslinje<MeldepliktData>,
        forrigeSegmentOppfylt: Boolean
    ): Tidslinje<MeldepliktVurdering> {
        val førsteDokument = dataForMeldeperiode.segmenter().firstOrNull { it.verdi.innsending != null }

        val fritak = dataForMeldeperiode.segmenter().firstOrNull {
            it.verdi.fritaksvurdering?.harFritak == true
        }
        val meldefrist = meldeperiode.fom.plusDays(7)

        return when {
            fritak?.verdi?.fritaksvurdering?.harFritak == true -> Tidslinje(
                meldeperiode,
                MeldepliktVurdering(
                    journalpostId = null,
                    fritak = true,
                    utfall = OPPFYLT,
                    årsak = null
                )
            )

            førsteDokument == null -> {
                Tidslinje(meldeperiode, Unit).splittOppOgMapOmEtter(
                    Period.ofDays(1)
                ) { segmenter ->
                    val segment = segmenter.single()
                    val fortsattMuligÅEndreUtfall = erDetFortsattMuligÅEndreUtfall(meldefrist, segment.periode.fom)

                    val utfall = if (fortsattMuligÅEndreUtfall && forrigeSegmentOppfylt) OPPFYLT else IKKE_OPPFYLT

                    listOf(
                        Segment(
                            segment.periode,
                            MeldepliktVurdering(
                                journalpostId = null,
                                fritak = false,
                                utfall = utfall,
                                årsak =
                                when {
                                    utfall == OPPFYLT -> null
                                    fortsattMuligÅEndreUtfall -> MELDEPLIKT_FRIST_IKKE_PASSERT
                                    else -> IKKE_OVERHOLDT_MELDEPLIKT_SANKSJON
                                }
                            )
                        )
                    ).let { TreeSet(it) }
                }
            }

            førsteDokument.periode.fom > meldefrist -> listOf(
                Segment(
                    Periode(meldeperiode.fom, førsteDokument.periode.fom.minusDays(1)),
                    MeldepliktVurdering(
                        journalpostId = førsteDokument.verdi.innsending,
                        fritak = false,
                        utfall = IKKE_OPPFYLT,
                        årsak = IKKE_OVERHOLDT_MELDEPLIKT_SANKSJON
                    )
                ),
                Segment(
                    Periode(førsteDokument.periode.fom, meldeperiode.tom),
                    MeldepliktVurdering(
                        journalpostId = førsteDokument.verdi.innsending,
                        fritak = false,
                        utfall = OPPFYLT,
                        årsak = null
                    )
                )
            ).let { Tidslinje(it) }

            else -> Tidslinje(
                meldeperiode,
                MeldepliktVurdering(
                    journalpostId = førsteDokument.verdi.innsending,
                    fritak = false,
                    utfall = OPPFYLT,
                    årsak = null
                )
            )
        }
    }

    private fun erDetFortsattMuligÅEndreUtfall(meldefrist: LocalDate, gjeldendeDag: LocalDate): Boolean {
        val erMeldefristIFremtiden = meldefrist > LocalDate.now()
        val erVurdertDagIkkePassert = gjeldendeDag >= LocalDate.now()
        val vurdertDagErInnenforFrist = meldefrist >= gjeldendeDag

        return (vurdertDagErInnenforFrist && erMeldefristIFremtiden) || erVurdertDagIkkePassert
    }


}
