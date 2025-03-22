package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.Underveisperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall.OPPFYLT
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.Fritaksvurdering
import no.nav.aap.komponenter.tidslinje.JoinStyle
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.StandardSammenslåere
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.verdityper.dokument.JournalpostId
import java.time.Clock
import java.time.LocalDate

/* Vurder meldeplikt, § 11-10. Basert på meldekort, fritak fra meldeplikt og at at det ikke er
 * meldeplikt før vedtak er fattet.
 */
class MeldepliktRegel(
    private val clock: Clock = Clock.systemDefaultZone(),
) : UnderveisRegel {
    data class MeldepliktData(
        val fritaksvurdering: Fritaksvurdering.FritaksvurderingData? = null,
        val meldekort: JournalpostId? = null,
        val førVedtak: Boolean = false,
    ) {
        companion object {
            fun merge(left: MeldepliktData?, right: MeldepliktData?): MeldepliktData {
                return MeldepliktData(
                    fritaksvurdering = left?.fritaksvurdering ?: right?.fritaksvurdering,
                    meldekort = left?.meldekort ?: right?.meldekort,
                    førVedtak = (left?.førVedtak ?: false) || (right?.førVedtak ?: false),
                )
            }
        }
    }

    override fun vurder(input: UnderveisInput, resultat: Tidslinje<Vurdering>): Tidslinje<Vurdering> {
        require(input.rettighetsperiode.inneholder(resultat.helePerioden())) {
            "kan ikke vurdere utenfor rettighetsperioden fordi meldeperioden ikke er definert"
        }

        val meldepliktDataTidslinje =
            Tidslinje(input.rettighetsperiode, MeldepliktData())
                .outerJoin(meldekortTidslinje(input), MeldepliktData.Companion::merge)
                .outerJoin(fritaksvurderingTidslinje(input), MeldepliktData.Companion::merge)
                .outerJoin(førVedtakTidslinje(input), MeldepliktData.Companion::merge)
                .splittOppIPerioder(resultat.map { vurdering -> vurdering.verdi.meldeperiode() })


        val meldepliktVurderinger = meldepliktDataTidslinje
            .fold(Tidslinje<MeldepliktVurdering>()) { meldeperioderVurdert, nåværendeMeldeperiodeSegment ->
                val neste = vurderMeldeperiode(
                    meldeperiode = nåværendeMeldeperiodeSegment.periode,
                    dataForMeldeperiode = nåværendeMeldeperiodeSegment.verdi,
                    forrigeSegmentOppfylt = meldeperioderVurdert.lastOrNull()?.verdi?.utfall == OPPFYLT
                )
                meldeperioderVurdert.kombiner(neste, StandardSammenslåere.xor())
            }.kryss(input.rettighetsperiode)

        return resultat.leggTilVurderinger(meldepliktVurderinger, Vurdering::leggTilMeldepliktVurdering)
    }


    fun fastsatteDagerMedMeldeplikt(
        vedtaksdatoFørstegangsbehandling: LocalDate?,
        fritak: Tidslinje<Fritaksvurdering.FritaksvurderingData>,
        meldeperioder: List<Periode>,
        underveis: Tidslinje<Underveisperiode>,
    ): List<Periode> {
        val meldepliktFraOgMed = vedtaksdatoFørstegangsbehandling?.plusDays(1)
            ?: return listOf()

        val harRett = underveis.mapValue {
            /* TODO: hent inn 100% gradering (?) */
            it.rettighetsType != null
        }

        return meldeperioder
            .asSequence()
            .map { Periode(it.fom, it.fom.plusDays(7)) }
            .filter { fastsatteDager -> meldepliktFraOgMed <= fastsatteDager.fom }
            .filter { fastsatteDager -> fritak.kryss(fastsatteDager).none { it.verdi.harFritak } }
            .filter { fastsatteDager -> harRett.kryss(fastsatteDager).any { harRett -> harRett.verdi } }
            .toList()
    }

    fun meldepliktFraOgMed(input: UnderveisInput): LocalDate? {
        /* TODO: virkningstidspunkt kan dra tidspunktet for når meldefristen inntrer lenger frem i tid (?) */
        return input.vedtaksdatoFørstegangsbehandling?.plusDays(1)
    }


    private fun fritaksvurderingTidslinje(input: UnderveisInput): Tidslinje<MeldepliktData> {
        return input.meldepliktGrunnlag.tilTidslinje().mapValue { MeldepliktData(fritaksvurdering = it) }
    }

    private fun førVedtakTidslinje(input: UnderveisInput): Tidslinje<MeldepliktData> {
        val meldepliktFraOgMed = meldepliktFraOgMed(input)
        return if (meldepliktFraOgMed == null) {
            Tidslinje(input.rettighetsperiode, MeldepliktData(førVedtak = true))
        } else {
            Tidslinje(
                listOf(
                    Segment(
                        periode = Periode(input.rettighetsperiode.fom, meldepliktFraOgMed.minusDays(1)),
                        verdi = MeldepliktData(førVedtak = true)
                    ),
                    Segment(
                        periode = Periode(meldepliktFraOgMed, input.rettighetsperiode.tom),
                        verdi = MeldepliktData(førVedtak = false)
                    ),
                )
            )
        }
    }

    private fun meldekortTidslinje(input: UnderveisInput): Tidslinje<MeldepliktData> {
        return input.innsendingsTidspunkt.entries.map { (dato, journalpostId) ->
            Segment(
                Periode(dato, dato),
                MeldepliktData(meldekort = journalpostId)
            )
        }.let { Tidslinje(it) }
    }


    private fun vurderMeldeperiode(
        meldeperiode: Periode,
        dataForMeldeperiode: Tidslinje<MeldepliktData>,
        forrigeSegmentOppfylt: Boolean,
    ): Tidslinje<MeldepliktVurdering> {
        val meldefrist = meldeperiode.fom.plusDays(7)
        val dagensDato = LocalDate.now(clock)

        val førsteDokument = dataForMeldeperiode.segmenter().firstNotNullOfOrNull {
            val innsending = it.verdi.meldekort
            when {
                it.verdi.førVedtak -> Segment(it.periode, MeldepliktVurdering.FørVedtak)
                it.verdi.fritaksvurdering?.harFritak == true -> Segment(it.periode, MeldepliktVurdering.Fritak)
                innsending != null -> Segment(it.periode, MeldepliktVurdering.MeldtSeg(innsending))
                else -> null
            }
        }

        val vanligVurdering = vurderMeldeperiodeIsolert(meldeperiode, førsteDokument)

        if (meldeperiode.tom < dagensDato) {
            /* Meldeperioden er i helhet historisk, så vurderingen av meldeperioden
             * i isolasjon står seg. */
            return vanligVurdering
        }

        /* Meldeperioden er ikke historisk. Det kan være ting i fremtiden som endrer
         * utfallet av meldeperioden. For at fremtidige vurderinger skal bli realistiske,
         * antar vi at medlemmet fortsetter å melde seg hvis de meldte seg, og at de
         * fortsetter å ikke melder seg hvis de ikke har meldt seg. */

        if (dagensDato < meldeperiode.fom) {
            return vanligVurdering.mapValue { vurdering ->
                if (vurdering.utfall == OPPFYLT) {
                    return@mapValue vurdering
                }
                if (forrigeSegmentOppfylt)
                    MeldepliktVurdering.FremtidigOppfylt
                else
                    MeldepliktVurdering.FremtidigIkkeOppfylt
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
                    if (forrigeSegmentOppfylt)
                        MeldepliktVurdering.FremtidigOppfylt
                    else
                        MeldepliktVurdering.FremtidigIkkeOppfylt,
                )
            }.let { Tidslinje(it) }
        }

        if (meldefrist < dagensDato) {
            return vanligVurdering.kombiner(
                Tidslinje(
                    Periode(meldeperiode.fom, dagensDato.minusDays(1)), null
                ), JoinStyle.LEFT_JOIN { periode, vurdering, høyre ->
                    if (vurdering.verdi.utfall == OPPFYLT) return@LEFT_JOIN vurdering
                    if (høyre == null) {/* fom dd */
                        return@LEFT_JOIN Segment(
                            periode, MeldepliktVurdering.FremtidigIkkeOppfylt
                        )
                    } else {
                        return@LEFT_JOIN vurdering
                    }
                })
        }

        error("skal ikke skje: uhåndtert case")
    }

    private fun vurderMeldeperiodeIsolert(
        meldeperiode: Periode,
        førsteDokument: Segment<out MeldepliktVurdering>?,
    ): Tidslinje<MeldepliktVurdering> {
        val meldefrist = meldeperiode.fom.plusDays(7)
        return when {
            førsteDokument == null ->
                /* ikke meldt seg */
                Tidslinje(
                    meldeperiode,
                    MeldepliktVurdering.IkkeMeldtSeg
                )

            førsteDokument.fom() <= meldefrist ->
                /* meldt seg innen fristen */
                Tidslinje(
                    meldeperiode,
                    førsteDokument.verdi,
                )

            else -> {
                /* meldt seg etter fristen */
                require(førsteDokument.fom() > meldefrist)
                return Tidslinje(
                    listOf(
                        Segment(
                            Periode(meldeperiode.fom, førsteDokument.fom().minusDays(1)),
                            MeldepliktVurdering.IkkeMeldtSeg,
                        ),
                        Segment(
                            Periode(førsteDokument.fom(), meldeperiode.tom),
                            førsteDokument.verdi,
                        )
                    )
                )
            }
        }
    }

}