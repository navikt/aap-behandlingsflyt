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
import java.time.Clock
import java.time.LocalDate

/**
 * Aktivitetskravene
 *
 * - MP
 * - Fravær
 *   - Aktivitet
 *   - etc
 */

class MeldepliktRegel(
    private val clock: Clock = Clock.systemDefaultZone(),
) : UnderveisRegel {
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

        val groupByMeldeperiode = groupByMeldeperiode(resultat, meldepliktTidslinje).segmenter()

        if (groupByMeldeperiode.isEmpty()) return resultat

        val førsteMeldeperiode = groupByMeldeperiode.first()

        val førsteVurdering = Tidslinje(
            førsteMeldeperiode.periode,
            MeldepliktVurdering(
                journalpostId = null,
                fritak = false,
                utfall = OPPFYLT,
            )
        )

        val meldepliktVurderinger = groupByMeldeperiode.drop(1)
            .fold(førsteVurdering) { meldeperioderVurdert, nåværendeMeldeperiodeSegment ->
                val neste = vurderForMeldeperiode(
                    meldeperiode = nåværendeMeldeperiodeSegment.periode,
                    dataForMeldeperiode = nåværendeMeldeperiodeSegment.verdi,
                    forrigeSegmentOppfylt = meldeperioderVurdert.lastOrNull()?.verdi?.utfall == OPPFYLT
                )
                meldeperioderVurdert.kombiner(neste, StandardSammenslåere.xor())
            }
            .kryss(input.rettighetsperiode)

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
                var forrigeDagOppfylt = forrigeSegmentOppfylt
                meldeperiode.dager().map { gjeldendeDag ->
                    val fortsattMuligÅEndreUtfall = erDetFortsattMuligÅEndreUtfall(meldefrist, gjeldendeDag)
                    val utfall = if (fortsattMuligÅEndreUtfall && forrigeDagOppfylt) OPPFYLT else IKKE_OPPFYLT

                    val vurdering = MeldepliktVurdering(
                        journalpostId = null,
                        fritak = false,
                        utfall = utfall,
                        årsak = when {
                            utfall == OPPFYLT -> null
                            fortsattMuligÅEndreUtfall -> MELDEPLIKT_FRIST_IKKE_PASSERT
                            else -> IKKE_OVERHOLDT_MELDEPLIKT_SANKSJON
                        }
                    )
                    forrigeDagOppfylt = utfall == OPPFYLT
                    Segment(Periode(gjeldendeDag, gjeldendeDag), vurdering)
                }.let { Tidslinje(it).komprimer() }
            }

            meldefrist < førsteDokument.periode.fom -> listOf(
                Segment(
                    Periode(meldeperiode.fom, førsteDokument.periode.fom.minusDays(1)),
                    MeldepliktVurdering(
                        journalpostId = null,
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
        val nå = LocalDate.now(clock)
        val erMeldefristIFremtiden = nå < meldefrist
        val erVurdertDagIkkePassert = nå <= gjeldendeDag
        val vurdertDagErInnenforFrist = gjeldendeDag <= meldefrist

        return (vurdertDagErInnenforFrist && erMeldefristIFremtiden) || erVurdertDagIkkePassert
    }
}
