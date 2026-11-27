package no.nav.aap.behandlingsflyt.behandling.vilkår.medlemskap

import no.nav.aap.behandlingsflyt.behandling.lovvalg.ArbeidINorgeGrunnlag
import no.nav.aap.behandlingsflyt.behandling.lovvalg.ArbeidAnsettelsesdetaljGrunnlag
import no.nav.aap.behandlingsflyt.behandling.lovvalg.Arbeidsforholdtype
import no.nav.aap.behandlingsflyt.behandling.lovvalg.Fartsomraade
import no.nav.aap.behandlingsflyt.behandling.lovvalg.ForutgåendeMedlemskapGrunnlag
import no.nav.aap.behandlingsflyt.behandling.lovvalg.ForutgåendeMedlemskapFaktaGrunnlag
import no.nav.aap.behandlingsflyt.behandling.lovvalg.InntektINorgeGrunnlag
import no.nav.aap.behandlingsflyt.behandling.lovvalg.Skipsregister
import no.nav.aap.behandlingsflyt.behandling.lovvalg.Skipstype
import no.nav.aap.behandlingsflyt.behandling.lovvalg.Yrke
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.utenlandsopphold.UtenlandsOppholdData
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.utenlandsopphold.UtenlandsPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.MedlemskapUnntakGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.Unntak
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.FolkeregisterStatus
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonStatus
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonopplysningMedHistorikk
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonopplysningMedHistorikkGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Statsborgerskap
import no.nav.aap.behandlingsflyt.test.Fakes
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth

@Fakes
class ForutgåendeMedlemskapVurderingServiceTest {

    @Test
    fun `automatisk om inntekt er oppfylt`() {
        val grunnlag = lagGrunnlag(false, true, false)
        val resultat = vurderOgSammenlign(grunnlag, Periode(LocalDate.now(), LocalDate.now().plusYears(1)))
        assertThat(resultat.kanBehandlesAutomatisk).isTrue
    }

    @Test
    fun `stopp om inntekt har hull siste 5 år`() {
        val grunnlag = lagGrunnlag(false, false, true)
        val resultat = vurderOgSammenlign(grunnlag, Periode(LocalDate.now(), LocalDate.now().plusYears(1)))
        assertThat(resultat.kanBehandlesAutomatisk).isFalse
    }

    @Test
    fun `stopp om akkumulert gap overstiger ti måneder med gap-toleranse`() {
        val rettighetsFom = LocalDate.now().withDayOfMonth(1)
        val rettighetsperiode = Periode(rettighetsFom, rettighetsFom.plusYears(1))
        val grunnlag = lagGrunnlagMedAkkumulerteEnMånedsgap(rettighetsFom, antallGapMåneder = 11)

        val resultat = vurderOgSammenlign(grunnlag, rettighetsperiode)

        assertThat(resultat.kanBehandlesAutomatisk).isFalse
    }

    @Test
    fun `automatisk om medl er oppfylt`() {
        val grunnlag = lagGrunnlag(true, false, true)
        val resultat = vurderOgSammenlign(grunnlag, Periode(LocalDate.now(), LocalDate.now().plusYears(1)))
        assertThat(resultat.kanBehandlesAutomatisk).isTrue
    }

    @Test
    fun `Bruker har statsborgerskap utenfor EØS og norsk statsborgerskap`() {
        val grunnlag = lagGrunnlagSomFerskUtenforEØSStatsborger(true, true, true)
        val resultat = vurderOgSammenlign(grunnlag, Periode(LocalDate.now(), LocalDate.now().plusYears(1)))
        val vurdering = resultat.tilhørighetVurdering
            .single { it.opplysning == "Har statsborgerskap utenfor EØS i perioden" }
        assertThat(vurdering.resultat).isFalse
    }

    @Test
    fun `Bruker har statsborgerskap utenfor EØS uten norsk statsborgerskap`() {
        val grunnlag = lagGrunnlagSomFerskUtenforEØSStatsborger(
            godkjentPgaUnntakIMedl = true,
            godkjentPgaInntekt = true,
            inntektHarHull = true,
            harNorskStatsborgerskap = false
        )
        val resultat = vurderOgSammenlign(grunnlag, Periode(LocalDate.now(), LocalDate.now().plusYears(1)))
        val vurdering = resultat.tilhørighetVurdering
            .single { it.opplysning == "Har statsborgerskap utenfor EØS i perioden" }
        assertThat(vurdering.resultat).isTrue
    }

    @Test
    fun `Bruker har statsborgerskap utenfor EØS og norsk statsborgerskap utgått før perioden`() {
        val grunnlag = lagGrunnlagSomFerskUtenforEØSStatsborger(
            godkjentPgaUnntakIMedl = true,
            godkjentPgaInntekt = true,
            inntektHarHull = true,
            harNorskStatsborgerskap = true,
            norskStatsborgerskapUtgåttFørPerioden = true
        )
        val resultat = vurderOgSammenlign(grunnlag, Periode(LocalDate.now(), LocalDate.now().plusYears(1)))
        val vurdering = resultat.tilhørighetVurdering
            .single { it.opplysning == "Har statsborgerskap utenfor EØS i perioden" }
        assertThat(vurdering.resultat).isTrue
    }

    @Test
    fun `Bruker har gammelt statsborgerskap utenfor EØS`() {
        val grunnlag = lagGrunnlagSomMedSteingammeltUtenforEØSStatsborgerskap(true, true, true)
        val resultat = vurderOgSammenlign(grunnlag, Periode(LocalDate.now(), LocalDate.now().plusYears(1)))
        val vurdering = resultat.tilhørighetVurdering
            .single { it.opplysning == "Har statsborgerskap utenfor EØS i perioden" }
        assertThat(vurdering.resultat).isFalse
    }

    @Test
    fun `lager tomme perioder i visuell tidslinje for måneder uten inntekt`() {
        val rettighetsperiode = Periode(LocalDate.now(), LocalDate.now().plusYears(1))
        val grunnlag = lagGrunnlag(godkjentPgaUnntakIMedl = false, godkjentPgaInntekt = false, inntektHarHull = true)

        val vurdering = vurderOgSammenlign(grunnlag, rettighetsperiode)
            .tilhørighetVurdering
            .single { it.opplysning == "Arbeids- og inntektshistorikk i Norge siste 5 år" }
        val tidslinje = vurdering.visuellTidslinje

        val gapMåned = YearMonth.from(LocalDate.now().minusYears(2))
        val gapEntry = tidslinje.single { YearMonth.from(it.periode.fom) == gapMåned }
        assertThat(gapEntry.periodeMangler).isTrue()
        assertThat(gapEntry.inntekter).isEmpty()

        val inntektMåned = YearMonth.from(LocalDate.now().minusYears(4))
        val inntektEntry = tidslinje.single { YearMonth.from(it.periode.fom) == inntektMåned }
        assertThat(inntektEntry.inntekter).hasSize(1)
        assertThat(inntektEntry.inntekter.first().virksomhetId).isEqualTo("1")
        assertThat(inntektEntry.inntekter.first().beloep).isEqualTo(1.0)
    }

    @Test
    fun `grupperer flere inntekter i samme måned som én tidslinje-entry med sub-liste`() {
        val rettighetsperiode = Periode(LocalDate.now(), LocalDate.now().plusYears(1))
        val månedMedFlereInntekter = LocalDate.now().minusYears(3)
        val grunnlag = lagGrunnlagMedFlereInntekterSammeMåned()

        val vurdering = vurderOgSammenlign(grunnlag, rettighetsperiode)
            .tilhørighetVurdering
            .single { it.opplysning == "Arbeids- og inntektshistorikk i Norge siste 5 år" }
        val tidslinje = vurdering.visuellTidslinje

        val entries = tidslinje.filter { YearMonth.from(it.periode.fom) == YearMonth.from(månedMedFlereInntekter) }
        assertThat(entries).hasSize(1)
        assertThat(entries.first().periodeMangler).isFalse()
        assertThat(entries.first().inntekter).hasSize(2)
    }

    @Test
    fun `NIS-turistskip inkluderer arbeidsforhold som startet før relevant periode og fortsatt pågår`() {
        val rettighetsperiode = Periode(LocalDate.now(), LocalDate.now().plusYears(1))

        val vurdering = vurderOgSammenlign(
                lagGrunnlagMedNisTuristskip(startdato = LocalDate.now().minusYears(7), sluttdato = null),
                rettighetsperiode
            )
            .tilhørighetVurdering
            .single { it.opplysning == "Har hatt arbeidsforhold på NIS-registrert turistskip" }

        assertThat(vurdering.resultat).isTrue
        assertThat(vurdering.bestemtArbeidsgruppeINorge).hasSize(1)
        val gruppe = vurdering.bestemtArbeidsgruppeINorge!!.first()
        assertThat(gruppe.virksomhetId).isEqualTo("123412341")
        val detalj = gruppe.ansettelsesDetaljer!!.first()
        assertThat(detalj.skipsregister).isEqualTo(Skipsregister.NIS)
        assertThat(detalj.skipstype).isEqualTo(Skipstype.TURIST)
        assertThat(detalj.fartsomraade).isEqualTo(Fartsomraade.UTENRIKS)
        assertThat(detalj.yrke?.kode).isEqualTo("3141113")
        assertThat(detalj.yrke?.beskrivelse).isEqualTo("MASKINSJEF")
    }

    @Test
    fun `stopp behandling når arbeidstaker har jobbet på NIS-turistskip i perioden`() {
        val rettighetsperiode = Periode(LocalDate.now(), LocalDate.now().plusYears(1))

        val resultat = vurderOgSammenlign(lagGrunnlagMedNisTuristskip(), rettighetsperiode)

        assertThat(resultat.kanBehandlesAutomatisk).isFalse
    }

    @Test
    fun `maritimt arbeidsforhold trigges ikke når skipstype er ANNET selv om register er NIS`() {
        val rettighetsperiode = Periode(LocalDate.now(), LocalDate.now().plusYears(1))

        val vurdering = vurderOgSammenlign(
            lagGrunnlagMedMaritimtArbeid(skipsregister = Skipsregister.NIS, skipstype = Skipstype.ANNET),
            rettighetsperiode
        ).tilhørighetVurdering.single { it.opplysning == "Har hatt arbeidsforhold på NIS-registrert turistskip" }

        assertThat(vurdering.resultat).isFalse
    }

    @Test
    fun `maritimt arbeidsforhold trigges ikke når skipstype er TURIST men register er NOR`() {
        val rettighetsperiode = Periode(LocalDate.now(), LocalDate.now().plusYears(1))

        val vurdering = vurderOgSammenlign(
            lagGrunnlagMedMaritimtArbeid(skipsregister = Skipsregister.NOR, skipstype = Skipstype.TURIST),
            rettighetsperiode
        ).tilhørighetVurdering.single { it.opplysning == "Har hatt arbeidsforhold på NIS-registrert turistskip" }

        assertThat(vurdering.resultat).isFalse
    }

    @Test
    fun `opphold etter start på rettighetsperioden teller ikke med i vurdering per relevant dato`() {
        val rettighetsFom = LocalDate.of(2025, 1, 1)
        val rettighetsperiode = Periode(rettighetsFom, rettighetsFom.plusYears(1))
        val oppholdEtterStart = UtenlandsPeriode(
            land = "SWE",
            tilDato = rettighetsFom.plusMonths(6),
            fraDato = rettighetsFom.plusMonths(3),
            iArbeid = false,
            utenlandsId = null
        )
        val grunnlag = lagGrunnlag(false, true, false).copy(
            nyeSoknadGrunnlag = UtenlandsOppholdData(true, true, false, false, listOf(oppholdEtterStart))
        )

        val hele = ForutgåendeMedlemskapVurderingService.vurderTilhørighet(grunnlag, rettighetsperiode)
        val perRelevantDato =
            ForutgåendeMedlemskapVurderingService.vurderTilhørighetForRelevantDato(grunnlag, rettighetsFom)

        assertThat(opphold(hele)).hasSize(1)
        assertThat(opphold(perRelevantDato)).isEmpty()
    }

    @Test
    fun `NIS-turistskip som omslutter hele vinduet gir treff per relevant dato`() {
        val relevantDato = LocalDate.of(2024, 6, 1)
        val grunnlag = lagGrunnlagMedNisTuristskip(
            startdato = LocalDate.of(2018, 1, 1),
            sluttdato = LocalDate.of(2025, 1, 1)
        )

        val resultat = ForutgåendeMedlemskapVurderingService.vurderTilhørighetForRelevantDato(grunnlag, relevantDato)

        val vurdering = resultat.tilhørighetVurdering
            .single { it.opplysning == "Har hatt arbeidsforhold på NIS-registrert turistskip" }
        assertThat(vurdering.resultat).isTrue
        assertThat(resultat.kanBehandlesAutomatisk).isFalse
    }

    @Test
    fun `NIS-turistskip som slutter før vinduet gir ikke treff`() {
        val relevantDato = LocalDate.of(2024, 6, 1)
        val grunnlag = lagGrunnlagMedNisTuristskip(
            startdato = LocalDate.of(2010, 1, 1),
            sluttdato = LocalDate.of(2019, 5, 31)
        )

        val vurdering = ForutgåendeMedlemskapVurderingService.vurderTilhørighetForRelevantDato(grunnlag, relevantDato)
            .tilhørighetVurdering
            .single { it.opplysning == "Har hatt arbeidsforhold på NIS-registrert turistskip" }

        assertThat(vurdering.resultat).isFalse
    }

    @Test
    fun `NIS-turistskip som starter etter relevant dato gir ikke treff`() {
        val relevantDato = LocalDate.of(2024, 6, 1)
        val grunnlag = lagGrunnlagMedNisTuristskip(startdato = LocalDate.of(2024, 6, 2), sluttdato = null)

        val vurdering = ForutgåendeMedlemskapVurderingService.vurderTilhørighetForRelevantDato(grunnlag, relevantDato)
            .tilhørighetVurdering
            .single { it.opplysning == "Har hatt arbeidsforhold på NIS-registrert turistskip" }

        assertThat(vurdering.resultat).isFalse
    }

    @Test
    fun `utenlandsopphold som omslutter hele vinduet gir treff per relevant dato`() {
        val relevantDato = LocalDate.of(2024, 6, 1)
        val omsluttendeOpphold = UtenlandsPeriode(
            land = "SWE",
            fraDato = LocalDate.of(2018, 1, 1),
            tilDato = LocalDate.of(2025, 1, 1),
            iArbeid = false,
            utenlandsId = null
        )
        val grunnlag = lagGrunnlag(false, true, false).copy(
            nyeSoknadGrunnlag = UtenlandsOppholdData(true, true, false, false, listOf(omsluttendeOpphold))
        )

        val vurdering = ForutgåendeMedlemskapVurderingService.vurderTilhørighetForRelevantDato(grunnlag, relevantDato)

        assertThat(opphold(vurdering)).hasSize(1)
    }

    @Test
    fun `arbeid i utlandet som omslutter hele vinduet gir treff per relevant dato`() {
        val relevantDato = LocalDate.of(2024, 6, 1)
        val omsluttendeArbeid = UtenlandsPeriode(
            land = "SWE",
            fraDato = LocalDate.of(2018, 1, 1),
            tilDato = LocalDate.of(2025, 1, 1),
            iArbeid = true,
            utenlandsId = null
        )
        val grunnlag = lagGrunnlag(false, true, false).copy(
            nyeSoknadGrunnlag = UtenlandsOppholdData(
                harBoddINorgeSiste5År = false,
                harArbeidetINorgeSiste5År = false,
                arbeidetUtenforNorgeFørSykdom = false,
                iTilleggArbeidUtenforNorge = false,
                utenlandsOpphold = listOf(omsluttendeArbeid)
            )
        )

        val vurdering = ForutgåendeMedlemskapVurderingService.vurderTilhørighetForRelevantDato(grunnlag, relevantDato)
            .tilhørighetVurdering
            .single { it.opplysning == "Arbeidet i utlandet siste 5 år" }

        assertThat(vurdering.resultat).isTrue
        assertThat(vurdering.oppgittJobbetIUtlandGrunnlag).hasSize(1)
    }

    @Test
    fun `statsborgerskap utenfor EØS som omslutter hele vinduet gir treff per relevant dato`() {
        val relevantDato = LocalDate.of(2024, 6, 1)
        val grunnlag = lagGrunnlag(false, true, false)
        val personopplysning = grunnlag.personopplysningGrunnlag!!.brukerPersonopplysning
        val medOmsluttendeStatsborgerskap = grunnlag.copy(
            personopplysningGrunnlag = PersonopplysningMedHistorikkGrunnlag(
                brukerPersonopplysning = personopplysning.copy(
                    statsborgerskap = listOf(
                        Statsborgerskap("ETH", LocalDate.of(2018, 1, 1), LocalDate.of(2025, 1, 1))
                    )
                )
            )
        )

        val vurdering = ForutgåendeMedlemskapVurderingService
            .vurderTilhørighetForRelevantDato(medOmsluttendeStatsborgerskap, relevantDato)
            .tilhørighetVurdering
            .single { it.opplysning == "Har statsborgerskap utenfor EØS i perioden" }

        assertThat(vurdering.resultat).isTrue
    }

    @Test
    fun `åpent statsborgerskap utenfor EØS som starter etter relevant dato gir ikke treff`() {
        val relevantDato = LocalDate.of(2024, 6, 1)
        val grunnlag = lagGrunnlag(false, true, false)
        val personopplysning = grunnlag.personopplysningGrunnlag!!.brukerPersonopplysning
        val medFremtidigStatsborgerskap = grunnlag.copy(
            personopplysningGrunnlag = PersonopplysningMedHistorikkGrunnlag(
                brukerPersonopplysning = personopplysning.copy(
                    statsborgerskap = listOf(Statsborgerskap("ETH", LocalDate.of(2025, 1, 1), null))
                )
            )
        )

        val vurdering = ForutgåendeMedlemskapVurderingService
            .vurderTilhørighetForRelevantDato(medFremtidigStatsborgerskap, relevantDato)
            .tilhørighetVurdering
            .single { it.opplysning == "Har statsborgerskap utenfor EØS i perioden" }

        assertThat(vurdering.resultat).isFalse
    }

    @Test
    fun `vedtak om annet lovvalgsland etter relevant dato gir ikke treff, men gjør det før`() {
        val relevantDato = LocalDate.of(2024, 6, 1)
        val grunnlag = lagGrunnlag(false, true, false)

        fun medLovvalgsland(periode: Periode) = grunnlag.copy(
            forutgåendeMedlemskapGrunnlag = grunnlag.forutgåendeMedlemskapGrunnlag!!.copy(
                medlemskapUnntakGrunnlag = MedlemskapUnntakGrunnlag(
                    unntak = listOf(
                        Segment(
                            periode = periode,
                            verdi = Unntak("unntak", null, true, "grunnlag", "lovvalg", false, "SWE", null)
                        )
                    )
                )
            )
        )

        fun annetLovvalgsland(grunnlag: ForutgåendeMedlemskapFaktaGrunnlag) = ForutgåendeMedlemskapVurderingService
            .vurderTilhørighetForRelevantDato(grunnlag, relevantDato)
            .tilhørighetVurdering
            .single { it.opplysning == "Vedtak om annet lovvalgsland finnes" }
            .resultat

        assertThat(annetLovvalgsland(medLovvalgsland(Periode(LocalDate.of(2024, 7, 1), LocalDate.of(2025, 1, 1)))))
            .isFalse
        assertThat(annetLovvalgsland(medLovvalgsland(Periode(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 12, 31)))))
            .isTrue
    }

    private fun opphold(vurdering: KanBehandlesAutomatiskVurdering) = vurdering.tilhørighetVurdering
        .single { it.opplysning == "Opphold i utlandet siste 5 år" }.oppgittUtenlandsOppholdGrunnlag

    /** Kjører både hele vurderingen og vurderingen per relevant dato, og krever lik vurdering fra begge. */
    private fun vurderOgSammenlign(
        grunnlag: ForutgåendeMedlemskapFaktaGrunnlag,
        rettighetsperiode: Periode
    ): KanBehandlesAutomatiskVurdering {
        val hele = ForutgåendeMedlemskapVurderingService.vurderTilhørighet(grunnlag, rettighetsperiode)
        val perRelevantDato =
            ForutgåendeMedlemskapVurderingService.vurderTilhørighetForRelevantDato(grunnlag, rettighetsperiode.fom)

        assertThat(perRelevantDato).isEqualTo(hele)
        return hele
    }

    private fun lagGrunnlag(
        godkjentPgaUnntakIMedl: Boolean,
        godkjentPgaInntekt: Boolean,
        inntektHarHull: Boolean
    ): ForutgåendeMedlemskapFaktaGrunnlag {
        val inntekterINorgeGrunnlag = if (godkjentPgaInntekt) {
            listOf(
                InntektINorgeGrunnlag(
                    "1",
                    1.0,
                    "NOR",
                    "NOR",
                    "type",
                    Periode(LocalDate.now().minusYears(5), LocalDate.now().minusYears(3)),
                    "orgname"
                ),
                InntektINorgeGrunnlag(
                    "1",
                    1.0,
                    "NOR",
                    "NOR",
                    "type",
                    Periode(LocalDate.now().minusYears(3), LocalDate.now()),
                    "orgname"
                ),
            )
        } else if (inntektHarHull) {
            listOf(
                InntektINorgeGrunnlag(
                    "1",
                    1.0,
                    "NOR",
                    "NOR",
                    "type",
                    Periode(LocalDate.now().minusYears(5), LocalDate.now().minusYears(3)),
                    "orgname"
                ),
                InntektINorgeGrunnlag(
                    "1",
                    1.0,
                    "NOR",
                    "NOR",
                    "type",
                    Periode(LocalDate.now().minusYears(1), LocalDate.now()),
                    "orgname"
                ),
            )
        } else emptyList()

        val medlUnntak = if (godkjentPgaUnntakIMedl) {
            MedlemskapUnntakGrunnlag(
                unntak = listOf(
                    Segment(
                        periode = Periode(LocalDate.now().minusYears(5), LocalDate.now()),
                        verdi = Unntak(
                            "unntak",
                            "statusaarsak",
                            true,
                            "grunnlag",
                            "lovvalg",
                            false,
                            EØSLandEllerLandMedAvtale.NOR.toString(),
                            null
                        )
                    )
                )
            )
        } else null

        return ForutgåendeMedlemskapFaktaGrunnlag(
            forutgåendeMedlemskapGrunnlag = ForutgåendeMedlemskapGrunnlag(
                medlemskapUnntakGrunnlag = medlUnntak,
                inntekterINorgeGrunnlag = inntekterINorgeGrunnlag,
                arbeiderINorgeGrunnlag = emptyList(),
                vurderinger = emptyList()
            ),
            personopplysningGrunnlag = PersonopplysningMedHistorikkGrunnlag(
                brukerPersonopplysning = PersonopplysningMedHistorikk(
                    fødselsdato = Fødselsdato(LocalDate.now().minusYears(18)),
                    id = 1,
                    dødsdato = null,
                    statsborgerskap = listOf(Statsborgerskap("NOR")),
                    folkeregisterStatuser = listOf(
                        FolkeregisterStatus(
                            status = PersonStatus.bosatt,
                            gyldighetstidspunkt = LocalDate.now(),
                            opphoerstidspunkt = LocalDate.now()
                        )
                    )
                ),
            ),
            nyeSoknadGrunnlag = UtenlandsOppholdData(true, true, false, false, null)
        )
    }

    private fun lagGrunnlagSomMedSteingammeltUtenforEØSStatsborgerskap(
        godkjentPgaUnntakIMedl: Boolean,
        godkjentPgaInntekt: Boolean,
        inntektHarHull: Boolean
    ): ForutgåendeMedlemskapFaktaGrunnlag {
        val inntekterINorgeGrunnlag = if (godkjentPgaInntekt) {
            listOf(
                InntektINorgeGrunnlag(
                    "1",
                    1.0,
                    "NOR",
                    "NOR",
                    "type",
                    Periode(LocalDate.now().minusYears(5), LocalDate.now().minusYears(3)),
                    "orgname"
                ),
                InntektINorgeGrunnlag(
                    "1",
                    1.0,
                    "NOR",
                    "NOR",
                    "type",
                    Periode(LocalDate.now().minusYears(3), LocalDate.now()),
                    "orgname"
                ),
            )
        } else if (inntektHarHull) {
            listOf(
                InntektINorgeGrunnlag(
                    "1",
                    1.0,
                    "NOR",
                    "NOR",
                    "type",
                    Periode(LocalDate.now().minusYears(5), LocalDate.now().minusYears(3)),
                    "orgname"
                ),
                InntektINorgeGrunnlag(
                    "1",
                    1.0,
                    "NOR",
                    "NOR",
                    "type",
                    Periode(LocalDate.now().minusYears(1), LocalDate.now()),
                    "orgname"
                ),
            )
        } else emptyList()

        val medlUnntak = if (godkjentPgaUnntakIMedl) {
            MedlemskapUnntakGrunnlag(
                unntak = listOf(
                    Segment(
                        periode = Periode(LocalDate.now().minusYears(5), LocalDate.now()),
                        verdi = Unntak(
                            "unntak",
                            "statusaarsak",
                            true,
                            "grunnlag",
                            "lovvalg",
                            false,
                            EØSLandEllerLandMedAvtale.NOR.toString(),
                            null
                        )
                    )
                )
            )
        } else null

        return ForutgåendeMedlemskapFaktaGrunnlag(
            forutgåendeMedlemskapGrunnlag = ForutgåendeMedlemskapGrunnlag(
                medlemskapUnntakGrunnlag = medlUnntak,
                inntekterINorgeGrunnlag = inntekterINorgeGrunnlag,
                arbeiderINorgeGrunnlag = emptyList(),
                vurderinger = emptyList()
            ),
            personopplysningGrunnlag = PersonopplysningMedHistorikkGrunnlag(
                brukerPersonopplysning = PersonopplysningMedHistorikk(
                    fødselsdato = Fødselsdato(LocalDate.now().minusYears(18)),
                    id = 1,
                    dødsdato = null,
                    statsborgerskap = listOf(
                        Statsborgerskap(
                            "ETH",
                            gyldigFraOgMed = LocalDate.now().minusYears(20),
                            LocalDate.now().minusYears(9)
                        ), Statsborgerskap("NOR", gyldigFraOgMed = LocalDate.now().minusYears(9), LocalDate.now())
                    ),
                    folkeregisterStatuser = listOf(
                        FolkeregisterStatus(
                            status = PersonStatus.bosatt,
                            gyldighetstidspunkt = LocalDate.now(),
                            opphoerstidspunkt = LocalDate.now()
                        )
                    )
                ),
            ),
            nyeSoknadGrunnlag = UtenlandsOppholdData(false, false, false, false, null)
        )
    }

    private fun lagGrunnlagSomFerskUtenforEØSStatsborger(
        godkjentPgaUnntakIMedl: Boolean,
        godkjentPgaInntekt: Boolean,
        inntektHarHull: Boolean,
        harNorskStatsborgerskap: Boolean = true,
        norskStatsborgerskapUtgåttFørPerioden: Boolean = false
    ): ForutgåendeMedlemskapFaktaGrunnlag {
        val inntekterINorgeGrunnlag = if (godkjentPgaInntekt) {
            listOf(
                InntektINorgeGrunnlag(
                    "1",
                    1.0,
                    "NOR",
                    "NOR",
                    "type",
                    Periode(LocalDate.now().minusYears(5), LocalDate.now().minusYears(3)),
                    "orgname"
                ),
                InntektINorgeGrunnlag(
                    "1",
                    1.0,
                    "NOR",
                    "NOR",
                    "type",
                    Periode(LocalDate.now().minusYears(3), LocalDate.now()),
                    "orgname"
                ),
            )
        } else if (inntektHarHull) {
            listOf(
                InntektINorgeGrunnlag(
                    "1",
                    1.0,
                    "NOR",
                    "NOR",
                    "type",
                    Periode(LocalDate.now().minusYears(5), LocalDate.now().minusYears(3)),
                    "orgname"
                ),
                InntektINorgeGrunnlag(
                    "1",
                    1.0,
                    "NOR",
                    "NOR",
                    "type",
                    Periode(LocalDate.now().minusYears(1), LocalDate.now()),
                    "orgname"
                ),
            )
        } else emptyList()

        val medlUnntak = if (godkjentPgaUnntakIMedl) {
            MedlemskapUnntakGrunnlag(
                unntak = listOf(
                    Segment(
                        periode = Periode(LocalDate.now().minusYears(5), LocalDate.now()),
                        verdi = Unntak(
                            "unntak",
                            "statusaarsak",
                            true,
                            "grunnlag",
                            "lovvalg",
                            false,
                            EØSLandEllerLandMedAvtale.NOR.toString(),
                            null
                        )
                    )
                )
            )
        } else null

        return ForutgåendeMedlemskapFaktaGrunnlag(
            forutgåendeMedlemskapGrunnlag = ForutgåendeMedlemskapGrunnlag(
                medlemskapUnntakGrunnlag = medlUnntak,
                inntekterINorgeGrunnlag = inntekterINorgeGrunnlag,
                arbeiderINorgeGrunnlag = emptyList(),
                vurderinger = emptyList()
            ),
            personopplysningGrunnlag = PersonopplysningMedHistorikkGrunnlag(
                brukerPersonopplysning = PersonopplysningMedHistorikk(
                    fødselsdato = Fødselsdato(LocalDate.now().minusYears(18)),
                    id = 1,
                    dødsdato = null,
                    statsborgerskap =
                        listOf(
                            Statsborgerskap(
                                "ETH",
                                gyldigFraOgMed = LocalDate.now().minusYears(2),
                                LocalDate.now().minusYears(1)
                            ),
                        ) + if (harNorskStatsborgerskap) {
                            listOf(
                                Statsborgerskap(
                                    "NOR",
                                    gyldigFraOgMed = if (norskStatsborgerskapUtgåttFørPerioden) LocalDate.now()
                                        .minusYears(20) else LocalDate.now().minusYears(1),
                                    gyldigTilOgMed = if (norskStatsborgerskapUtgåttFørPerioden) LocalDate.now()
                                        .minusYears(9) else LocalDate.now()
                                )
                            )
                        } else {
                            emptyList()
                        },
                    folkeregisterStatuser = listOf(
                        FolkeregisterStatus(
                            status = PersonStatus.bosatt,
                            gyldighetstidspunkt = LocalDate.now(),
                            opphoerstidspunkt = LocalDate.now()
                        )
                    )
                ),
            ),
            nyeSoknadGrunnlag = UtenlandsOppholdData(false, false, false, false, null)
        )
    }

    private fun lagGrunnlagMedFlereInntekterSammeMåned(): ForutgåendeMedlemskapFaktaGrunnlag {
        val fullCoverage = Periode(LocalDate.now().minusYears(5), LocalDate.now())
        val inntekterINorgeGrunnlag = listOf(
            InntektINorgeGrunnlag("virksomhet-A", 1000.0, "NOR", "NOR", "type", fullCoverage, "Virksomhet A"),
            InntektINorgeGrunnlag("virksomhet-B", 2000.0, "NOR", "NOR", "type", fullCoverage, "Virksomhet B"),
        )

        return ForutgåendeMedlemskapFaktaGrunnlag(
            forutgåendeMedlemskapGrunnlag = ForutgåendeMedlemskapGrunnlag(
                medlemskapUnntakGrunnlag = null,
                inntekterINorgeGrunnlag = inntekterINorgeGrunnlag,
                arbeiderINorgeGrunnlag = emptyList(),
                vurderinger = emptyList()
            ),
            personopplysningGrunnlag = PersonopplysningMedHistorikkGrunnlag(
                brukerPersonopplysning = PersonopplysningMedHistorikk(
                    fødselsdato = Fødselsdato(LocalDate.now().minusYears(18)),
                    id = 1,
                    dødsdato = null,
                    statsborgerskap = listOf(Statsborgerskap("NOR")),
                    folkeregisterStatuser = listOf(
                        FolkeregisterStatus(
                            status = PersonStatus.bosatt,
                            gyldighetstidspunkt = LocalDate.now(),
                            opphoerstidspunkt = LocalDate.now()
                        )
                    )
                ),
            ),
            nyeSoknadGrunnlag = UtenlandsOppholdData(true, true, false, false, null)
        )
    }

    private fun lagGrunnlagMedAkkumulerteEnMånedsgap(
        rettighetsFom: LocalDate,
        antallGapMåneder: Int
    ): ForutgåendeMedlemskapFaktaGrunnlag {
        val startMåned = YearMonth.from(rettighetsFom.minusYears(5))
        val sluttMåned = YearMonth.from(rettighetsFom)
        val inntekterINorgeGrunnlag = mutableListOf<InntektINorgeGrunnlag>()

        var nåMåned = startMåned
        var månedIndeks = 0
        var opprettedeGap = 0

        while (!nåMåned.isAfter(sluttMåned)) {
            val skalVæreGap = opprettedeGap < antallGapMåneder && månedIndeks % 5 == 0
            if (skalVæreGap) {
                opprettedeGap++
            } else {
                inntekterINorgeGrunnlag.add(
                    InntektINorgeGrunnlag(
                        "1",
                        1.0,
                        "NOR",
                        "NOR",
                        "type",
                        Periode(nåMåned.atDay(1), nåMåned.atEndOfMonth()),
                        "orgname"
                    )
                )
            }
            nåMåned = nåMåned.plusMonths(1)
            månedIndeks++
        }

        return ForutgåendeMedlemskapFaktaGrunnlag(
            forutgåendeMedlemskapGrunnlag = ForutgåendeMedlemskapGrunnlag(
                medlemskapUnntakGrunnlag = null,
                inntekterINorgeGrunnlag = inntekterINorgeGrunnlag,
                arbeiderINorgeGrunnlag = emptyList(),
                vurderinger = emptyList()
            ),
            personopplysningGrunnlag = PersonopplysningMedHistorikkGrunnlag(
                brukerPersonopplysning = PersonopplysningMedHistorikk(
                    fødselsdato = Fødselsdato(LocalDate.now().minusYears(18)),
                    id = 1,
                    dødsdato = null,
                    statsborgerskap = listOf(Statsborgerskap("NOR")),
                    folkeregisterStatuser = listOf(
                        FolkeregisterStatus(
                            status = PersonStatus.bosatt,
                            gyldighetstidspunkt = LocalDate.now(),
                            opphoerstidspunkt = LocalDate.now()
                        )
                    )
                ),
            ),
            nyeSoknadGrunnlag = UtenlandsOppholdData(true, true, false, false, null)
        )
    }

    private fun lagGrunnlagMedNisTuristskip(
        startdato: LocalDate = LocalDate.now().minusYears(3),
        sluttdato: LocalDate? = LocalDate.now().minusYears(1)
    ) = lagGrunnlagMedMaritimtArbeid(
        startdato = startdato,
        sluttdato = sluttdato,
        skipsregister = Skipsregister.NIS,
        skipstype = Skipstype.TURIST,
        fartsomraade = Fartsomraade.UTENRIKS,
        yrke = Yrke(kode = "3141113", beskrivelse = "MASKINSJEF"),
    )

    private fun lagGrunnlagMedMaritimtArbeid(
        startdato: LocalDate = LocalDate.now().minusYears(3),
        sluttdato: LocalDate? = LocalDate.now().minusYears(1),
        skipsregister: Skipsregister = Skipsregister.NOR,
        skipstype: Skipstype = Skipstype.ANNET,
        fartsomraade: Fartsomraade = Fartsomraade.INNENRIKS,
        yrke: Yrke = Yrke(kode = "6411104", beskrivelse = "FISKER"),
    ): ForutgåendeMedlemskapFaktaGrunnlag =
        ForutgåendeMedlemskapFaktaGrunnlag(
            forutgåendeMedlemskapGrunnlag = ForutgåendeMedlemskapGrunnlag(
                medlemskapUnntakGrunnlag = null,
                inntekterINorgeGrunnlag = listOf(
                    InntektINorgeGrunnlag(
                        "1", 1.0, "NOR", "NOR", "type",
                        Periode(LocalDate.now().minusYears(5), LocalDate.now()),
                        "orgname"
                    )
                ),
                arbeiderINorgeGrunnlag = listOf(
                    ArbeidINorgeGrunnlag(
                        identifikator = "123412341",
                        arbeidsforholdKode = Arbeidsforholdtype.MARITIMT_ARBEIDSFORHOLD,
                        startdato = startdato,
                        sluttdato = sluttdato,
                        ansettelsesdetaljer = listOf(
                            ArbeidAnsettelsesdetaljGrunnlag(
                                skipsregister = skipsregister,
                                skipstype = skipstype,
                                fartsomraade = fartsomraade,
                                yrke = yrke,
                            )
                        )
                    )
                ),
                vurderinger = emptyList()
            ),
            personopplysningGrunnlag = PersonopplysningMedHistorikkGrunnlag(
                brukerPersonopplysning = PersonopplysningMedHistorikk(
                    fødselsdato = Fødselsdato(LocalDate.now().minusYears(40)),
                    id = 1,
                    dødsdato = null,
                    statsborgerskap = listOf(Statsborgerskap("NOR")),
                    folkeregisterStatuser = listOf(
                        FolkeregisterStatus(
                            status = PersonStatus.bosatt,
                            gyldighetstidspunkt = LocalDate.now(),
                            opphoerstidspunkt = LocalDate.now()
                        )
                    )
                ),
            ),
            nyeSoknadGrunnlag = UtenlandsOppholdData(true, true, false, false, null)
        )
}
