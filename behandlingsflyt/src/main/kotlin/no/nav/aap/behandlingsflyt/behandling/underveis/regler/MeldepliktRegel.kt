package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.Underveisperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.Fritaksvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.MeldepliktOverstyringStatus
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.OverstyringMeldepliktData
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.StandardSammenslåere
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.orEmpty
import no.nav.aap.komponenter.tidslinje.tidslinjeOf
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid
import no.nav.aap.verdityper.dokument.JournalpostId
import java.time.LocalDate

/** Vurder meldeplikt, § 11-10. Basert på meldekort, fritak fra meldeplikt og at at det ikke er
 * meldeplikt før vedtak er fattet.
 */
class MeldepliktRegel: UnderveisRegel {
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

    companion object {
        fun fastsatteDagerMedMeldeplikt(
            vedtaksdatoFørsteInnvilgelse: LocalDate?,
            fritak: Tidslinje<Fritaksvurdering.FritaksvurderingData>,
            meldeperioder: List<Periode>,
            underveis: Tidslinje<Underveisperiode>,
        ): List<Periode> {
            /* TODO: uheldig at periodene vi sender til meldekort-backend ikke deler implementasjon
            * med vilkårsprøvingen.
            */
            val meldepliktFraOgMed = vedtaksdatoFørsteInnvilgelse?.plusDays(1)
                ?: return emptyList()

            val harRett = underveis.mapValue {
                it.rettighetsType != null
            }

            return meldeperioder
                .asSequence()
                .map { Periode(it.fom, it.fom.plusDays(7)) }
                .filter { fastsatteDager -> meldepliktFraOgMed <= fastsatteDager.fom }
                .filter { fastsatteDager ->
                    fritak.begrensetTil(fastsatteDager).segmenter().none { it.verdi.harFritak }
                }
                .filter { fastsatteDager ->
                    harRett.begrensetTil(fastsatteDager).segmenter().any { harRett -> harRett.verdi }
                }
                .toList()
        }
    }

    override fun vurder(input: UnderveisInput, resultat: Tidslinje<Vurdering>): Tidslinje<Vurdering> {
        require(input.periodeForVurdering.inneholder(resultat.helePerioden())) {
            "kan ikke vurdere utenfor rettighetsperioden fordi meldeperioden ikke er definert"
        }

        // Bygg opp input-tidslinje
        val meldepliktDataTidslinje =
            Tidslinje(input.periodeForVurdering, MeldepliktData())
                .outerJoin(meldekortTidslinje(input), MeldepliktData.Companion::merge)
                .outerJoin(fritaksvurderingTidslinje(input), MeldepliktData.Companion::merge)
                .outerJoin(overstyringMeldepliktVurderingTidslinje(input), MeldepliktData.Companion::merge)
                .outerJoin(førVedtakTidslinje(input), MeldepliktData.Companion::merge)
                .outerJoin(førsteDagMedRettTidslinje(resultat), MeldepliktData.Companion::merge)
                .outerJoin(utenRettTidslinje(resultat), MeldepliktData.Companion::merge)
                .splittOppIPerioder(resultat.segmenter().map { vurdering -> vurdering.verdi.meldeperiode() })

        // Kjør meldeplikt-regel-logikk
        var forrigePeriode: Segment<Tidslinje<MeldepliktData>>? = null
        val meldepliktVurderinger = meldepliktDataTidslinje
            .segmenter()
            .fold(Tidslinje<MeldepliktVurdering>()) { meldeperioderVurdert, nåværendeMeldeperiodeSegment: Segment<Tidslinje<MeldepliktData>> ->
                val neste = vurderMeldeperiode(
                    meldeperiode = nåværendeMeldeperiodeSegment.periode,
                    dataForMeldeperiode = nåværendeMeldeperiodeSegment.verdi,
                    dataForForrigeMeldeperiode = forrigePeriode?.verdi,
                )
                forrigePeriode = nåværendeMeldeperiodeSegment
                meldeperioderVurdert.kombiner(neste, StandardSammenslåere.xor())
            }.begrensetTil(input.periodeForVurdering)

        return resultat.leggTilVurderinger(meldepliktVurderinger, Vurdering::leggTilMeldepliktVurdering)
    }

    private fun utenRettTidslinje(vurderinger: Tidslinje<Vurdering>): Tidslinje<MeldepliktData> {
        return vurderinger.mapValue { MeldepliktData(utenRett = it.fårAapEtter == null) }
    }

    private fun førsteDagMedRettTidslinje(vurderinger: Tidslinje<Vurdering>): Tidslinje<MeldepliktData> {
        val dagenFørVurderinger =
            vurderinger.segmenter().firstOrNull()?.periode?.fom?.minusDays(1) ?: return Tidslinje()
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


    private fun fritaksvurderingTidslinje(input: UnderveisInput): Tidslinje<MeldepliktData> {
        return input.meldepliktGrunnlag.tilTidslinje().mapValue { MeldepliktData(fritaksvurdering = it) }
    }

    private fun overstyringMeldepliktVurderingTidslinje(input: UnderveisInput): Tidslinje<MeldepliktData> {
        return input.overstyringMeldepliktGrunnlag.tilTidslinje().mapValue {
            MeldepliktData(overstyringMeldeplikt = it)
        }
    }

    private fun førVedtakTidslinje(input: UnderveisInput): Tidslinje<MeldepliktData> {
        val meldepliktFraOgMed = input.vedtaksdatoFørstegangsbehandling?.plusDays(1)
        return if (meldepliktFraOgMed == null) {
            tidslinjeOf(input.periodeForVurdering to MeldepliktData(førVedtak = true))
        } else {
            tidslinjeOf(
                Periode(Tid.MIN, meldepliktFraOgMed.minusDays(1)) to MeldepliktData(førVedtak = true),
                Periode(meldepliktFraOgMed, Tid.MAKS) to MeldepliktData(førVedtak = false)
            ).begrensetTil(input.periodeForVurdering)
        }
    }

    private fun meldekortTidslinje(input: UnderveisInput): Tidslinje<MeldepliktData> {
        return input.innsendingsTidspunkt.entries.map { (dato, journalpostId) ->
            Segment(
                Periode(dato, dato),
                MeldepliktData(meldekort = journalpostId)
            )
        }.let(::Tidslinje)
    }

    private fun vurderMeldeperiode(
        meldeperiode: Periode,
        dataForMeldeperiode: Tidslinje<MeldepliktData>,
        dataForForrigeMeldeperiode: Tidslinje<MeldepliktData>?,
    ): Tidslinje<MeldepliktVurdering> {
        val potensieltTidligDokument = helligdagsunntakFastsattMeldedag[meldeperiode.fom]?.let { nyTidligereFrist ->
            val begrensetForrigePeriode =
                dataForForrigeMeldeperiode?.begrensetTil(Periode(nyTidligereFrist, LocalDate.MAX)).orEmpty()

            begrensetForrigePeriode.segmenter().firstNotNullOfOrNull {
                val innsending = it.verdi.meldekort
                when {
                    innsending != null -> Segment(it.periode, MeldepliktVurdering.MeldtSeg(innsending))
                    else -> null
                }
            }
        }

        val førsteDokument = førsteDokumentForMeldepliktdataTidslinje(dataForMeldeperiode)

        val prioritertDokument = potensieltTidligDokument ?: førsteDokument

        return vurderMeldeperiodeIsolert(meldeperiode, prioritertDokument)
    }

    private fun førsteDokumentForMeldepliktdataTidslinje(dataForMeldeperiode: Tidslinje<MeldepliktData>): Segment<out MeldepliktVurdering>? =
        dataForMeldeperiode.segmenter().firstNotNullOfOrNull {
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

    private fun vurderMeldeperiodeIsolert(
        meldeperiode: Periode,
        førsteDokument: Segment<out MeldepliktVurdering>?,
    ): Tidslinje<MeldepliktVurdering> {
        val meldefrist = helligdagsunntakjustertMeldefrist(meldeperiode.fom.plusDays(7))

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
