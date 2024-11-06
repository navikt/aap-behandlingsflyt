package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.behandlingsflyt.behandling.underveis.regler.UtledMeldeperiodeRegel.Companion.groupByMeldeperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisÅrsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisÅrsak.IKKE_OVERHOLDT_MELDEPLIKT_SANKSJON
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisÅrsak.MELDEPLIKT_FRIST_IKKE_PASSERT
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
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

        val fritaksTidslinje = input.meldepliktGrunnlag.vurderinger.tidslinje()
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

        val data =
            fritaksTidslinje.kombiner(dokumentTidslinje, JoinStyle.OUTER_JOIN { periode, fritaksvurdering, dokument ->
                Segment(periode, MeldepliktData(fritaksvurdering?.verdi, dokument?.verdi))

            })

        var forrigeSegmentOppfylt = true

        val meldepliktVurderinger = groupByMeldeperiode(resultat, data)
            .flatMap { meldeperiodeSegment ->
                val meldeperioden = meldeperiodeSegment.periode
                val førsteDokument = meldeperiodeSegment.verdi.segmenter().firstOrNull { it.verdi.innsending != null }

                val fritak = meldeperiodeSegment.verdi.segmenter().firstOrNull {
                    it.verdi.fritaksvurdering?.harFritak == true
                }
                val meldefrist = meldeperioden.fom.plusDays(7)

                when {
                    fritak?.verdi?.fritaksvurdering?.harFritak == true -> Tidslinje(
                        meldeperioden,
                        MeldepliktVurdering(
                            journalpostId = førsteDokument?.verdi?.innsending,
                            fritak = true,
                            utfall = OPPFYLT,
                            årsak = null
                        )
                    ).also { forrigeSegmentOppfylt = true }

                    førsteDokument == null -> {
                        val forrigeMeldeperiodeOppfylt = forrigeSegmentOppfylt
                        Tidslinje(meldeperioden, true).splittOppOgMapOmEtter(
                            Period.ofDays(1)
                        ) { segmenter ->
                            val segment = segmenter.single()
                            val erMeldefristIFremtiden = meldefrist > LocalDate.now()
                            val erVurdertDagIkkePassert = segment.periode.fom >= LocalDate.now()
                            val vurdertDagErInnenforFrist = meldefrist >= segment.periode.fom

                            val fortsattMuligÅEndreUtfall =
                                if (vurdertDagErInnenforFrist && erMeldefristIFremtiden) true
                                else if (erVurdertDagIkkePassert) true
                                else false

                            val utfall = if (fortsattMuligÅEndreUtfall && forrigeMeldeperiodeOppfylt) OPPFYLT else IKKE_OPPFYLT

                            TreeSet(
                                listOf(
                                    Segment(
                                        segment.periode,
                                        MeldepliktVurdering(
                                            journalpostId = null,
                                            fritak = false,
                                            utfall = utfall,
                                            årsak = if (fortsattMuligÅEndreUtfall) MELDEPLIKT_FRIST_IKKE_PASSERT else IKKE_OVERHOLDT_MELDEPLIKT_SANKSJON
                                        )
                                    )
                                )
                            ).also { forrigeSegmentOppfylt = utfall == OPPFYLT }
                        }
                    }

                    førsteDokument.periode.fom > meldefrist -> listOf(
                        Segment(
                            Periode(meldeperioden.fom, førsteDokument.periode.fom.minusDays(1)),
                            MeldepliktVurdering(
                                journalpostId = førsteDokument.verdi.innsending,
                                fritak = false,
                                utfall = IKKE_OPPFYLT,
                                årsak = IKKE_OVERHOLDT_MELDEPLIKT_SANKSJON
                            )
                        ),
                        Segment(
                            Periode(førsteDokument.periode.fom, meldeperioden.tom),
                            MeldepliktVurdering(
                                journalpostId = førsteDokument.verdi.innsending,
                                fritak = false,
                                utfall = OPPFYLT,
                                årsak = null
                            )
                        )
                    ).let { Tidslinje(it) }.also { forrigeSegmentOppfylt = true }

                    else -> Tidslinje(
                        meldeperioden,
                        MeldepliktVurdering(
                            journalpostId = førsteDokument.verdi.innsending,
                            fritak = false,
                            utfall = OPPFYLT,
                            årsak = null
                        )
                    ).also { forrigeSegmentOppfylt = true }
                }
            }

        return resultat.leggTilVurderinger(meldepliktVurderinger, Vurdering::leggTilMeldepliktVurdering)
    }


}
