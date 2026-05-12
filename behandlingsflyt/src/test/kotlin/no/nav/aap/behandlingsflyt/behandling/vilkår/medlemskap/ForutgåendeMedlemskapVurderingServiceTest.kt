package no.nav.aap.behandlingsflyt.behandling.vilkår.medlemskap

import no.nav.aap.behandlingsflyt.behandling.lovvalg.ArbeidINorgeGrunnlag
import no.nav.aap.behandlingsflyt.behandling.lovvalg.Arbeidsforholdtype
import no.nav.aap.behandlingsflyt.behandling.lovvalg.ForutgåendeMedlemskapArbeidInntektGrunnlag
import no.nav.aap.behandlingsflyt.behandling.lovvalg.ForutgåendeMedlemskapGrunnlag
import no.nav.aap.behandlingsflyt.behandling.lovvalg.InntektINorgeGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.utenlandsopphold.UtenlandsOppholdData
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.MedlemskapUnntakGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.Unntak
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.FolkeregisterStatus
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonStatus
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonopplysningMedHistorikk
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonopplysningMedHistorikkGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Statsborgerskap
import no.nav.aap.behandlingsflyt.test.Fakes
import no.nav.aap.behandlingsflyt.test.FakeUnleashBase
import no.nav.aap.behandlingsflyt.test.FakeUnleashBaseWithDefaultDisabled
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth

@Fakes
class ForutgåendeMedlemskapVurderingServiceTest {
    private val service = ForutgåendeMedlemskapVurderingService()

    @Test
    fun `automatisk om inntekt er oppfylt`() {
        val grunnlag = lagGrunnlag(false, true, false)
        val resultat = service.vurderTilhørighet(grunnlag, Periode(LocalDate.now(), LocalDate.now().plusYears(1)))
        assertThat(resultat.kanBehandlesAutomatisk).isTrue
    }

    @Test
    fun `stopp om inntekt har hull siste 5 år`() {
        val grunnlag = lagGrunnlag(false, false, true)
        val resultat = service.vurderTilhørighet(grunnlag, Periode(LocalDate.now(), LocalDate.now().plusYears(1)))
        assertThat(resultat.kanBehandlesAutomatisk).isFalse
    }

    @Test
    fun `automatisk om medl er oppfylt`() {
        val grunnlag = lagGrunnlag(true, false, true)
        val resultat = service.vurderTilhørighet(grunnlag, Periode(LocalDate.now(), LocalDate.now().plusYears(1)))
        assertThat(resultat.kanBehandlesAutomatisk).isTrue
    }

    @Test
    fun `Bruker har ferskt statsborgerskap utenfor EØS`() {
        val grunnlag = lagGrunnlagSomFerskUtenforEØSStatsborger(true, true, true)
        val resultat = service.vurderTilhørighet(grunnlag, Periode(LocalDate.now(), LocalDate.now().plusYears(1)))
        val vurdering = resultat.tilhørighetVurdering
            .single { it.opplysning == "Har statsborgerskap utenfor EØS i perioden" }
        assertThat(vurdering.resultat).isTrue
    }

    @Test
    fun `Bruker har gammelt statsborgerskap utenfor EØS`() {
        val grunnlag = lagGrunnlagSomMedSteingammeltUtenforEØSStatsborgerskap(true, true, true)
        val resultat = service.vurderTilhørighet(grunnlag, Periode(LocalDate.now(), LocalDate.now().plusYears(1)))
        val vurdering = resultat.tilhørighetVurdering
            .single { it.opplysning == "Har statsborgerskap utenfor EØS i perioden" }
        assertThat(vurdering.resultat).isFalse
    }

    @Test
    fun `lager tomme perioder i visuell tidslinje for måneder uten inntekt`() {
        val rettighetsperiode = Periode(LocalDate.now(), LocalDate.now().plusYears(1))
        val grunnlag = lagGrunnlag(godkjentPgaUnntakIMedl = false, godkjentPgaInntekt = false, inntektHarHull = true)

        val vurdering = service.vurderTilhørighet(grunnlag, rettighetsperiode)
            .tilhørighetVurdering
            .single { it.opplysning == "Sammenhengende arbeid og inntekt i Norge siste 5 år" }
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

        val vurdering = service.vurderTilhørighet(grunnlag, rettighetsperiode)
            .tilhørighetVurdering
            .single { it.opplysning == "Sammenhengende arbeid og inntekt i Norge siste 5 år" }
        val tidslinje = vurdering.visuellTidslinje

        val entries = tidslinje.filter { YearMonth.from(it.periode.fom) == YearMonth.from(månedMedFlereInntekter) }
        assertThat(entries).hasSize(1)
        assertThat(entries.first().periodeMangler).isFalse()
        assertThat(entries.first().inntekter).hasSize(2)
    }

    @Test
    fun `maritimtArbeid inkluderer arbeidsforhold som startet før relevant periode og fortsatt pågår`() {
        val rettighetsperiode = Periode(LocalDate.now(), LocalDate.now().plusYears(1))
        val serviceWithMaritimtArbeid = ForutgåendeMedlemskapVurderingService(
            FakeUnleashBaseWithDefaultDisabled(listOf(BehandlingsflytFeature.MaritimtArbeid))
        )

        val vurdering = serviceWithMaritimtArbeid
            .vurderTilhørighet(
                lagGrunnlagMedMaritimtArbeid(startdato = LocalDate.now().minusYears(7), sluttdato = null),
                rettighetsperiode
            )
            .tilhørighetVurdering
            .single { it.opplysning == "Har hatt arbeid på skip i perioden" }

        assertThat(vurdering.resultat).isTrue
        assertThat(vurdering.maritimtArbeidINorge).hasSize(1)
        assertThat(vurdering.maritimtArbeidINorge!!.first().virksomhetId).isEqualTo("123412341")
    }

    @Test
    fun `stopp behandling når arbeidstaker har jobbet på skip i perioden`() {
        val rettighetsperiode = Periode(LocalDate.now(), LocalDate.now().plusYears(1))
        val service = ForutgåendeMedlemskapVurderingService(
            FakeUnleashBaseWithDefaultDisabled(listOf(BehandlingsflytFeature.MaritimtArbeid))
        )

        val resultat = service.vurderTilhørighet(lagGrunnlagMedMaritimtArbeid(), rettighetsperiode)

        assertThat(resultat.kanBehandlesAutomatisk).isFalse
    }

    private fun lagGrunnlag(
        godkjentPgaUnntakIMedl: Boolean,
        godkjentPgaInntekt: Boolean,
        inntektHarHull: Boolean
    ): ForutgåendeMedlemskapGrunnlag {
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

        return ForutgåendeMedlemskapGrunnlag(
            medlemskapArbeidInntektGrunnlag = ForutgåendeMedlemskapArbeidInntektGrunnlag(
                medlemskapGrunnlag = medlUnntak,
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
    ): ForutgåendeMedlemskapGrunnlag {
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

        return ForutgåendeMedlemskapGrunnlag(
            medlemskapArbeidInntektGrunnlag = ForutgåendeMedlemskapArbeidInntektGrunnlag(
                medlemskapGrunnlag = medlUnntak,
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
        inntektHarHull: Boolean
    ): ForutgåendeMedlemskapGrunnlag {
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

        return ForutgåendeMedlemskapGrunnlag(
            medlemskapArbeidInntektGrunnlag = ForutgåendeMedlemskapArbeidInntektGrunnlag(
                medlemskapGrunnlag = medlUnntak,
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
                            gyldigFraOgMed = LocalDate.now().minusYears(2),
                            LocalDate.now().minusYears(1)
                        ),
                        Statsborgerskap(
                            "NOR",
                            gyldigFraOgMed = LocalDate.now().minusYears(1),
                            LocalDate.now()
                        )
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

    private fun lagGrunnlagMedFlereInntekterSammeMåned(): ForutgåendeMedlemskapGrunnlag {
        val fullCoverage = Periode(LocalDate.now().minusYears(5), LocalDate.now())
        val inntekterINorgeGrunnlag = listOf(
            InntektINorgeGrunnlag("virksomhet-A", 1000.0, "NOR", "NOR", "type", fullCoverage, "Virksomhet A"),
            InntektINorgeGrunnlag("virksomhet-B", 2000.0, "NOR", "NOR", "type", fullCoverage, "Virksomhet B"),
        )

        return ForutgåendeMedlemskapGrunnlag(
            medlemskapArbeidInntektGrunnlag = ForutgåendeMedlemskapArbeidInntektGrunnlag(
                medlemskapGrunnlag = null,
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

    private fun lagGrunnlagMedMaritimtArbeid(
        startdato: LocalDate = LocalDate.now().minusYears(3),
        sluttdato: LocalDate? = LocalDate.now().minusYears(1)
    ): ForutgåendeMedlemskapGrunnlag =
        ForutgåendeMedlemskapGrunnlag(
            medlemskapArbeidInntektGrunnlag = ForutgåendeMedlemskapArbeidInntektGrunnlag(
                medlemskapGrunnlag = null,
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
                        sluttdato = sluttdato
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
