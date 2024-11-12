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

        val innsendtTidslinje: Tidslinje<JournalpostId?> = input.innsendingsTidspunkt.entries.map {
            Segment<JournalpostId?>(
                Periode(it.key, it.key), it.value
            )
        }.let { Tidslinje(it) }

        val dokumentTidslinje: Tidslinje<JournalpostId?> = defaultTidslinje.kombiner(
            innsendtTidslinje, StandardSammenslåere.prioriterHøyreSideCrossJoin()
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
            førsteMeldeperiode.periode, MeldepliktVurdering(
                dokument = null,
                utfall = OPPFYLT,
            )
        )

        val meldepliktVurderinger =
            groupByMeldeperiode.drop(1).fold(førsteVurdering) { meldeperioderVurdert, nåværendeMeldeperiodeSegment ->
                val neste = vurderForMeldeperiode(
                    meldeperiode = nåværendeMeldeperiodeSegment.periode,
                    dataForMeldeperiode = nåværendeMeldeperiodeSegment.verdi,
                    forrigeSegmentOppfylt = meldeperioderVurdert.lastOrNull()?.verdi?.utfall == OPPFYLT
                )
                meldeperioderVurdert.kombiner(neste, StandardSammenslåere.xor())
            }.kryss(input.rettighetsperiode)

        return resultat.leggTilVurderinger(meldepliktVurderinger, Vurdering::leggTilMeldepliktVurdering)
    }


    sealed interface Dokument
    data class Meldt(val journalpostId: JournalpostId) : Dokument
    data object Fritak : Dokument


    private fun vanligRegel(
        meldeperiode: Periode,
        førsteDokument: Segment<out Dokument>?,
    ): Tidslinje<MeldepliktVurdering> {

        val meldefrist = meldeperiode.fom.plusDays(7)
        return when {/* meldt seg innen fristen */
            førsteDokument != null && førsteDokument.fom() <= meldefrist -> Tidslinje(
                meldeperiode, MeldepliktVurdering(
                    dokument = førsteDokument.verdi, utfall = OPPFYLT, årsak = null
                )
            )

            /* meldt seg etter fristen */
            førsteDokument != null -> listOf(
                Segment(
                    Periode(meldeperiode.fom, førsteDokument.fom().minusDays(1)), MeldepliktVurdering(
                        dokument = null,
                        utfall = IKKE_OPPFYLT,
                        årsak = IKKE_OVERHOLDT_MELDEPLIKT_SANKSJON,
                    )
                ), Segment(
                    Periode(førsteDokument.fom(), meldeperiode.tom), MeldepliktVurdering(
                        dokument = førsteDokument.verdi,
                        utfall = OPPFYLT,
                        årsak = null,
                    )
                )
            ).let { Tidslinje(it) }

            /* ikke meldt seg */
            førsteDokument == null -> Tidslinje(
                meldeperiode, MeldepliktVurdering(
                    dokument = null,
                    utfall = IKKE_OPPFYLT,
                    årsak = IKKE_OVERHOLDT_MELDEPLIKT_SANKSJON,
                )
            )

            else -> error("uforventet case")
        }
    }

    private fun vurderForMeldeperiode(
        meldeperiode: Periode,
        dataForMeldeperiode: Tidslinje<MeldepliktData>,
        forrigeSegmentOppfylt: Boolean
    ): Tidslinje<MeldepliktVurdering> {
        val meldefrist = meldeperiode.fom.plusDays(7)
        val dagensDato = LocalDate.now(clock)

        val førsteDokument = dataForMeldeperiode.segmenter().firstNotNullOfOrNull {
            val innsending = it.verdi.innsending
            when {
                it.verdi.fritaksvurdering?.harFritak == true -> Segment(it.periode, Fritak)
                innsending != null -> Segment(it.periode, Meldt(innsending))
                else -> null
            }
        }

        val vanligVurdering = vanligRegel(meldeperiode, førsteDokument)

        if (meldeperiode.tom < dagensDato) {
            return vanligVurdering
        }

        if (dagensDato < meldeperiode.fom) {
            return vanligVurdering.mapValue { vurdering ->
                if (vurdering.utfall == OPPFYLT) {
                    return@mapValue vurdering
                }
                val utfall = if (forrigeSegmentOppfylt) OPPFYLT else IKKE_OPPFYLT
                MeldepliktVurdering(
                    dokument = null,
                    utfall = utfall,
                    årsak = if (utfall == OPPFYLT) null else MELDEPLIKT_FRIST_IKKE_PASSERT,
                )
            }
        }

        check(meldeperiode.inneholder(dagensDato))

        if (dagensDato <= meldefrist) {
            return vanligVurdering.map {
                if (it.verdi.utfall == OPPFYLT) {
                    return@map it
                }
                Segment(
                    it.periode,
                    MeldepliktVurdering(
                        dokument = null,
                        utfall = if (forrigeSegmentOppfylt) OPPFYLT else IKKE_OPPFYLT,
                        årsak = if (forrigeSegmentOppfylt) null else MELDEPLIKT_FRIST_IKKE_PASSERT,
                    )
                )
            }
                .let { Tidslinje(it) }
        }

        if (meldefrist < dagensDato) {
            return vanligVurdering.kombiner(
                Tidslinje(
                    Periode(meldeperiode.fom, dagensDato.minusDays(1)),
                    null
                ), JoinStyle.LEFT_JOIN { periode, vurdering, høyre ->
                    if (vurdering.verdi.utfall == OPPFYLT) return@LEFT_JOIN vurdering
                    if (høyre == null) {
                        /* fom dd */
                        return@LEFT_JOIN Segment(
                            periode, MeldepliktVurdering(
                                dokument = null,
                                utfall = IKKE_OPPFYLT,
                                årsak = MELDEPLIKT_FRIST_IKKE_PASSERT
                            )
                        )
                    } else {
                        return@LEFT_JOIN vurdering
                    }
                })
        }

        error("skal ikke skje: uhåndtert case")
    }
}
