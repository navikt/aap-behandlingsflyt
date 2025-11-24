package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.Underveisperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall.OPPFYLT
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.Fritaksvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.MeldepliktOverstyringStatus
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.OverstyringMeldepliktData
import no.nav.aap.komponenter.tidslinje.JoinStyle
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.StandardSammenslåere
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.verdityper.dokument.JournalpostId
import java.time.Clock
import java.time.LocalDate

/** Vurder meldeplikt, § 11-10. Basert på meldekort, fritak fra meldeplikt og at at det ikke er
 * meldeplikt før vedtak er fattet.
 */
class MeldepliktRegel(
    private val clock: Clock = Clock.systemDefaultZone(),
) : UnderveisRegel {
    data class MeldepliktData(
        val fritaksvurdering: Fritaksvurdering.FritaksvurderingData? = null,
        val overstyringMeldeplikt: OverstyringMeldepliktData? = null,
        val meldekort: JournalpostId? = null,
        val førVedtak: Boolean = false,
        val utenRett: Boolean = false,
        val førsteDagMedRettForPerioden: Boolean = false,
    ) {
        companion object {
            fun merge(left: MeldepliktData?, right: MeldepliktData?): MeldepliktData {
                return MeldepliktData(
                    fritaksvurdering = left?.fritaksvurdering ?: right?.fritaksvurdering,
                    overstyringMeldeplikt = left?.overstyringMeldeplikt ?: right?.overstyringMeldeplikt,
                    meldekort = left?.meldekort ?: right?.meldekort,
                    førVedtak = (left?.førVedtak ?: false) || (right?.førVedtak ?: false),
                    utenRett = (left?.utenRett ?: false) || (right?.utenRett ?: false),
                    førsteDagMedRettForPerioden =
                        (left?.førsteDagMedRettForPerioden ?: false) ||
                                (right?.førsteDagMedRettForPerioden ?: false)
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
                .outerJoin(overstyringMeldepliktVurderingTidslinje(input), MeldepliktData.Companion::merge)
                .outerJoin(førVedtakTidslinje(input), MeldepliktData.Companion::merge)
                .outerJoin(førsteDagMedRettTidslinje(resultat), MeldepliktData.Companion::merge)
                .outerJoin(utenRettTidslinje(resultat), MeldepliktData.Companion::merge)
                .splittOppIPerioder(resultat.segmenter().map { vurdering -> vurdering.verdi.meldeperiode() })


        val meldepliktVurderinger = meldepliktDataTidslinje
            .segmenter()
            .fold(Tidslinje<MeldepliktVurdering>()) { meldeperioderVurdert, nåværendeMeldeperiodeSegment ->
                val neste = vurderMeldeperiode(
                    meldeperiode = nåværendeMeldeperiodeSegment.periode,
                    dataForMeldeperiode = nåværendeMeldeperiodeSegment.verdi,
                    forrigeSegmentOppfylt = meldeperioderVurdert.segmenter().lastOrNull()?.verdi?.utfall == OPPFYLT
                )
                meldeperioderVurdert.kombiner(neste, StandardSammenslåere.xor())
            }.begrensetTil(input.rettighetsperiode)

        return resultat.leggTilVurderinger(meldepliktVurderinger, Vurdering::leggTilMeldepliktVurdering)
    }

    private fun utenRettTidslinje(vurderinger: Tidslinje<Vurdering>): Tidslinje<MeldepliktData> {
        return vurderinger.mapValue { MeldepliktData(utenRett = it.fårAapEtter == null) }
    }

    private fun førsteDagMedRettTidslinje(vurderinger: Tidslinje<Vurdering>): Tidslinje<MeldepliktData> {
        val dagenFørVurderinger = vurderinger.segmenter().firstOrNull()?.periode?.fom?.minusDays(1) ?: return Tidslinje()
        val virtuellPrefixUtenRett = listOf(Segment(Periode(dagenFørVurderinger, dagenFørVurderinger), null))

        return (virtuellPrefixUtenRett + vurderinger.mapValue { it.fårAapEtter }.komprimer().segmenter())
            .asSequence()
            .windowed(2, 1)
            .flatMap { (segment1, segment2) ->
                val periode = segment2.periode
                if (segment1.verdi == null && segment2.verdi != null) {
                    val overgangsperiode = Periode(periode.fom, periode.fom)
                    val restperiode = Periode(periode.fom.plusDays(1), periode.tom.plusDays(1)).overlapp(periode)
                    listOfNotNull(
                        Segment(overgangsperiode, MeldepliktData(førsteDagMedRettForPerioden = true)),
                        restperiode?.let { Segment(it, MeldepliktData(førsteDagMedRettForPerioden = false)) },
                    )
                } else {
                    listOf(
                        Segment(periode, MeldepliktData(førsteDagMedRettForPerioden = false))
                    )
                }
            }
            .let { Tidslinje(it.toList()) }
    }


    fun fastsatteDagerMedMeldeplikt(
        vedtaksdatoFørstegangsbehandling: LocalDate?,
        fritak: Tidslinje<Fritaksvurdering.FritaksvurderingData>,
        meldeperioder: List<Periode>,
        underveis: Tidslinje<Underveisperiode>,
    ): List<Periode> {
        /* TODO: uheldig at periodene vi sender til meldekort-backend ikke deler implementasjon
         * med vilkårsprøvingen.
         */
        val meldepliktFraOgMed = vedtaksdatoFørstegangsbehandling?.plusDays(1)
            ?: return emptyList()

        val harRett = underveis.mapValue {
            it.rettighetsType != null
        }

        return meldeperioder
            .asSequence()
            .map { Periode(it.fom, it.fom.plusDays(7)) }
            .filter { fastsatteDager -> meldepliktFraOgMed <= fastsatteDager.fom }
            .filter { fastsatteDager -> fritak.begrensetTil(fastsatteDager).segmenter().none { it.verdi.harFritak } }
            .filter { fastsatteDager -> harRett.begrensetTil(fastsatteDager).segmenter().any { harRett -> harRett.verdi } }
            .toList()
    }

    fun meldepliktFraOgMed(input: UnderveisInput): LocalDate? {
        return input.vedtaksdatoFørstegangsbehandling?.plusDays(1)
    }


    private fun fritaksvurderingTidslinje(input: UnderveisInput): Tidslinje<MeldepliktData> {
        return input.meldepliktGrunnlag.tilTidslinje().mapValue { MeldepliktData(fritaksvurdering = it) }
    }
    
    private fun overstyringMeldepliktVurderingTidslinje(input: UnderveisInput): Tidslinje<MeldepliktData> {
        return input.overstyringMeldepliktGrunnlag.tilTidslinje().mapValue {
            MeldepliktData(overstyringMeldeplikt = it)
        }
    }

    private fun førVedtakTidslinje(input: UnderveisInput): Tidslinje<MeldepliktData> {
        val meldepliktFraOgMed = meldepliktFraOgMed(input)
        return if (meldepliktFraOgMed == null) {
            Tidslinje(input.rettighetsperiode, MeldepliktData(førVedtak = true))
        } else if (meldepliktFraOgMed <= input.rettighetsperiode.fom) {
            Tidslinje(input.rettighetsperiode, MeldepliktData(førVedtak = false))
        }
        else {
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
                it.verdi.utenRett ->
                    Segment(it.periode, MeldepliktVurdering.UtenRett)
                it.verdi.førsteDagMedRettForPerioden -> Segment(
                    it.periode,
                    MeldepliktVurdering.FørsteMeldeperiodeMedRett
                )

                it.verdi.fritaksvurdering?.harFritak == true -> Segment(it.periode, MeldepliktVurdering.Fritak)
                it.verdi.overstyringMeldeplikt?.meldepliktOverstyringStatus == MeldepliktOverstyringStatus.RIMELIG_GRUNN ->
                    Segment(it.periode, MeldepliktVurdering.RimeligGrunnOverstyring)
                it.verdi.overstyringMeldeplikt?.meldepliktOverstyringStatus == MeldepliktOverstyringStatus.HAR_MELDT_SEG ->
                    Segment(it.periode, MeldepliktVurdering.MeldtSegOverstyring)
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
            return vanligVurdering.segmenter().map {
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
                Tidslinje(
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