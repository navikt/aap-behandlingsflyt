package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarBarnetilleggLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarHelseinstitusjonLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.ForeslåVedtakLøsning
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.TilkjentYtelseRepository
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.tilTidslinje
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.Relasjon
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Institusjonsopphold
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.InstitusjonsoppholdRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Institusjonstype
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Oppholdstype
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.VurderingAvForeldreAnsvarDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.VurderingerForBarnetillegg
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.VurdertBarnDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.flate.HelseinstitusjonVurderingDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.flate.HelseinstitusjonVurderingerDto
import no.nav.aap.behandlingsflyt.help.assertTidslinje
import no.nav.aap.behandlingsflyt.integrasjon.institusjonsopphold.InstitusjonsoppholdJSON
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.test.AlleAvskruddUnleash
import no.nav.aap.behandlingsflyt.test.PersonNavn
import no.nav.aap.behandlingsflyt.test.desember
import no.nav.aap.behandlingsflyt.test.februar
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.behandlingsflyt.test.juli
import no.nav.aap.behandlingsflyt.test.mai
import no.nav.aap.behandlingsflyt.test.modell.TestPerson
import no.nav.aap.behandlingsflyt.test.modell.genererIdent
import no.nav.aap.behandlingsflyt.test.november
import no.nav.aap.behandlingsflyt.test.oktober
import no.nav.aap.behandlingsflyt.test.september
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.komponenter.verdityper.Tid
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class InstitusjonFlytTest : AbstraktFlytOrkestratorTest(AlleAvskruddUnleash::class) {
    @Test
    fun `Stopper opp på institusjonssteget i førstegangsbehandling når innleggelsesdato er mer enn 2 mnd siden`() {
        val fom = LocalDate.now()
        val periode = Periode(fom, fom.plusYears(3))

        val person = TestPersoner.STANDARD_PERSON()
        person.institusjonsopphold = listOf(
            InstitusjonsoppholdJSON(
                startdato = LocalDate.now().minusMonths(3),
                forventetSluttdato = LocalDate.now().plusMonths(2),
                institusjonstype = "HS",
                institusjonsnavn = "institusjon",
                organisasjonsnummer = "2334",
                kategori = "H",
            )
        )

        val (_, behandling) = sendInnFørsteSøknad(
            person = person,
            mottattTidspunkt = fom.atStartOfDay(),
            periode = periode,
        )

        behandling
            .medKontekst {
                assertThat(behandling.typeBehandling()).isEqualTo(TypeBehandling.Førstegangsbehandling)
                assertThat(åpneAvklaringsbehov).isNotEmpty()
                assertThat(behandling.status()).isEqualTo(Status.UTREDES)
            }
            .løsSykdom(periode.fom)
            .løsBistand(periode.fom)
            .løsRefusjonskrav()
            .løsSykdomsvurderingBrev()
            .bekreftVurderinger()
            .kvalitetssikre()
            .løsBeregningstidspunkt()
            .løsOppholdskrav(fom)
            .medKontekst {
                assertThat(åpneAvklaringsbehov.map { it.definisjon }).contains(Definisjon.AVKLAR_HELSEINSTITUSJON)
            }
    }

    // -------------------------------------------------------------------------
    // Institusjon uten barnetillegg
    // -------------------------------------------------------------------------

    @Test
    fun `institusjonsopphold med reduksjon gir 50 prosent gradering i tilkjent ytelse`() {
        val fom = LocalDate.now().minusMonths(5)
        val oppholdTom = LocalDate.now().plusMonths(2)
        val tidligsteReduksjonsdato = fom.withDayOfMonth(1).plusMonths(4)

        val person = TestPersoner.STANDARD_PERSON()
        person.institusjonsopphold = listOf(hsOpphold(fom, oppholdTom))

        val (_, behandling) = sendInnFørsteSøknad(
            person = person,
            mottattTidspunkt = fom.atStartOfDay(),
        )

        behandling
            .løsSykdom(fom)
            .løsBistand(fom)
            .løsRefusjonskrav()
            .løsSykdomsvurderingBrev()
            .bekreftVurderinger()
            .kvalitetssikre()
            .løsBeregningstidspunkt()
            .løsOppholdskrav(fom)
            .medKontekst {
                assertThat(åpneAvklaringsbehov.map { it.definisjon }).contains(Definisjon.AVKLAR_HELSEINSTITUSJON)
            }
            .løsAvklaringsBehov(løsHelseinstitusjonMedReduksjon(fom, oppholdTom))
            .løsAndreStatligeYtelser()
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .fattVedtak()
            .løsVedtaksbrev()
            .medKontekst {
                val tilkjentYtelse = hentTilkjentYtelse(behandling.id)
                val tidslinje = tilkjentYtelse.map { Segment(it.periode, it.tilkjent) }.let(::Tidslinje)

                assertThat(tidslinje.isNotEmpty()).isTrue()

                val periodeUtenReduksjon = Periode(fom, tidligsteReduksjonsdato.minusDays(1))
                val periodeMedReduksjon = Periode(tidligsteReduksjonsdato, oppholdTom)

                assertTidslinje(
                    tidslinje.begrensetTil(periodeUtenReduksjon),
                    periodeUtenReduksjon to {
                        assertThat(it.graderingGrunnlag.institusjonGradering).isEqualTo(Prosent.`0_PROSENT`)
                    }
                )
                assertTidslinje(
                    tidslinje.begrensetTil(periodeMedReduksjon),
                    periodeMedReduksjon to {
                        assertThat(it.graderingGrunnlag.institusjonGradering).isEqualTo(Prosent.`50_PROSENT`)
                    }
                )
            }
    }

    @Test
    fun `institusjonsopphold uten reduksjon pga forsørger gir 0 prosent gradering i tilkjent ytelse`() {
        val fom = LocalDate.now().minusMonths(5)
        val oppholdTom = LocalDate.now().plusMonths(2)

        val person = TestPersoner.STANDARD_PERSON()
        person.institusjonsopphold = listOf(hsOpphold(fom, oppholdTom))

        val (_, behandling) = sendInnFørsteSøknad(
            person = person,
            mottattTidspunkt = fom.atStartOfDay(),
        )

        behandling
            .løsSykdom(fom)
            .løsBistand(fom)
            .løsRefusjonskrav()
            .løsSykdomsvurderingBrev()
            .bekreftVurderinger()
            .kvalitetssikre()
            .løsBeregningstidspunkt()
            .løsOppholdskrav(fom)
            .løsAvklaringsBehov(løsHelseinstitusjonUtenReduksjon(fom, oppholdTom, "uten reduksjon"))
            .løsAndreStatligeYtelser()
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .fattVedtak()
            .løsVedtaksbrev()
            .medKontekst {
                val tilkjentYtelse = hentTilkjentYtelse(behandling.id)
                val tidslinje = tilkjentYtelse.map { Segment(it.periode, it.tilkjent) }.let(::Tidslinje)

                assertThat(tidslinje.isNotEmpty()).isTrue()

                val helePerioden = tilkjentYtelse.tilTidslinje().helePerioden()
                assertTidslinje(
                    tidslinje.begrensetTil(helePerioden),
                    helePerioden to {
                        assertThat(it.graderingGrunnlag.institusjonGradering).isEqualTo(Prosent.`0_PROSENT`)
                        assertThat(it.barnetillegg).isEqualTo(Beløp(0))
                    }
                )
            }
    }

    @Test
    fun `ingen institusjonsopphold gir 0 prosent institusjongradering i tilkjent ytelse`() {
        val fom = LocalDate.now().minusMonths(3)

        val (_, behandling) = sendInnFørsteSøknad(
            person = TestPersoner.STANDARD_PERSON(),
            mottattTidspunkt = fom.atStartOfDay(),
        )

        behandling
            .løsSykdom(fom)
            .løsBistand(fom)
            .løsRefusjonskrav()
            .løsSykdomsvurderingBrev()
            .bekreftVurderinger()
            .kvalitetssikre()
            .løsBeregningstidspunkt()
            .løsOppholdskrav(fom)
            .medKontekst {
                assertThat(åpneAvklaringsbehov.map { it.definisjon })
                    .doesNotContain(Definisjon.AVKLAR_HELSEINSTITUSJON)
            }
            .løsAndreStatligeYtelser()
            .løsUtenSamordning()
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .fattVedtak()
            .løsVedtaksbrev()
            .medKontekst {
                val tilkjentYtelse = hentTilkjentYtelse(behandling.id)
                val tidslinje = tilkjentYtelse.map { Segment(it.periode, it.tilkjent) }.let(::Tidslinje)
                val helePerioden = tilkjentYtelse.tilTidslinje().helePerioden()

                assertTidslinje(
                    tidslinje.begrensetTil(helePerioden),
                    helePerioden to {
                        assertThat(it.graderingGrunnlag.institusjonGradering).isEqualTo(Prosent.`0_PROSENT`)
                    }
                )
            }
    }

    // -------------------------------------------------------------------------
    // Institusjon med barnetillegg
    // -------------------------------------------------------------------------

    @Test
    fun `institusjonsopphold med reduksjon og barnetillegg i forskjellige perioder - barnetillegg beholdes og AAP reduseres`() {
        val fom = LocalDate.now().minusMonths(5)
        val oppholdTom = LocalDate.now().plusMonths(6)
        val barnetilleggFom = fom.minusMonths(5)
        val barnetilleggTom = fom.plusMonths(1)
        val tidligsteReduksjonsdato = fom.withDayOfMonth(1).plusMonths(4)
        val barnFødselsdato = fom.minusYears(5)

        val barn = lagBarn(barnFødselsdato)
        val person = TestPersoner.STANDARD_PERSON()
            .medBarn(listOf(barn))
        person.institusjonsopphold = listOf(hsOpphold(fom, oppholdTom))

        val (_, behandling) = sendInnFørsteSøknad(
            person = person,
            mottattTidspunkt = fom.atStartOfDay(),
        )

        behandling
            .løsSykdom(fom)
            .løsBistand(fom)
            .løsRefusjonskrav()
            .løsSykdomsvurderingBrev()
            .bekreftVurderinger()
            .kvalitetssikre()
            .løsBeregningstidspunkt()
            .løsOppholdskrav(fom)
            .løsAvklaringsBehov(løsBarnetilleggFraOgTilDato(barn, barnetilleggFom, barnetilleggTom))
            .løsAvklaringsBehov(løsHelseinstitusjonMedReduksjon(fom, oppholdTom))
            .løsAndreStatligeYtelser()
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .fattVedtak()
            .løsVedtaksbrev()
            .medKontekst {
                val tilkjentYtelse = hentTilkjentYtelse(behandling.id)
                val tidslinje = tilkjentYtelse.map { Segment(it.periode, it.tilkjent) }.let(::Tidslinje)

                assertThat(tidslinje.isNotEmpty()).isTrue()

                // Periode uten reduksjon: full AAP og barnetillegg
                val periodeMedBarnetillegg = Periode(tidslinje.minDato(), barnetilleggTom.minusDays(1))
                assertTidslinje(
                    tidslinje.begrensetTil(periodeMedBarnetillegg),
                    periodeMedBarnetillegg to {
                        assertThat(it.graderingGrunnlag.institusjonGradering).isEqualTo(Prosent.`0_PROSENT`)
                        assertThat(it.antallBarn).isEqualTo(1)
                        assertThat(it.barnetillegg).isEqualTo(Beløp(37))
                    }
                )

                // Periode med reduksjon: institusjongradering=50%, men barnetillegg beholdes
                val periodeMedReduksjon = Periode(tidligsteReduksjonsdato, oppholdTom)
                assertTidslinje(
                    tidslinje.begrensetTil(periodeMedReduksjon),
                    periodeMedReduksjon to {
                        assertThat(it.graderingGrunnlag.institusjonGradering).isEqualTo(Prosent.`50_PROSENT`)
                        assertThat(it.antallBarn).isEqualTo(0)
                    }
                )
            }
    }

    @Test
    fun `institusjonsopphold med barnetillegg i samme periode gir full AAP og barnetillegg`() {
        val fom = LocalDate.now().minusMonths(5)
        val oppholdTom = LocalDate.now().plusMonths(2)
        val barnetilleggTom = oppholdTom.plusMonths(3)
        val barnFødselsdato = fom.minusYears(5)

        val barn = lagBarn(barnFødselsdato)
        val person = TestPersoner.STANDARD_PERSON()
            .medBarn(listOf(barn))
            .medInstitusjonsopphold(listOf(hsOpphold(startdato = fom, sluttdato = oppholdTom)))

        val (_, behandling) = sendInnFørsteSøknad(
            person = person,
            mottattTidspunkt = fom.atStartOfDay(),
        )

        behandling
            .løsSykdom(fom)
            .løsBistand(fom)
            .løsRefusjonskrav()
            .løsSykdomsvurderingBrev()
            .bekreftVurderinger()
            .kvalitetssikre()
            .løsBeregningstidspunkt()
            .løsOppholdskrav(fom)
            .løsAvklaringsBehov(løsBarnetilleggFraOgTilDato(barn, fom, barnetilleggTom))
            .løsAndreStatligeYtelser()
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .fattVedtak()
            .løsVedtaksbrev()
            .medKontekst {
                val tilkjentYtelse = hentTilkjentYtelse(behandling.id)
                val tidslinje = tilkjentYtelse.map { Segment(it.periode, it.tilkjent) }.let(::Tidslinje)

                assertThat(tidslinje.isNotEmpty()).isTrue()

                val barnetilleggPerioden = Periode(fom, barnetilleggTom.minusDays(1))
                assertTidslinje(
                    tidslinje.begrensetTil(barnetilleggPerioden),
                    barnetilleggPerioden to {
                        assertThat(it.graderingGrunnlag.institusjonGradering).isEqualTo(Prosent.`0_PROSENT`)
                        assertThat(it.antallBarn).isEqualTo(1)
                    }
                )

                val segment2025 = tidslinje.segmenter().find { it.periode.fom.year == 2025 }
                assertThat(segment2025).isNotNull
                assertThat(segment2025!!.verdi.barnetillegg).isEqualTo(Beløp(37))

                val segment2026 = tidslinje.segmenter().find { it.periode.fom.year == 2026 }
                assertThat(segment2026).isNotNull
                assertThat(segment2026!!.verdi.barnetillegg).isEqualTo(Beløp(38))
            }
    }

    @Test
    fun `barnetillegg skal beholdes under og etter institusjonsopphold`() {
        val fom = 1 januar 2025
        val oppholdTom = 30 juli 2025
        val barnetilleggTom1 = 28 mai 2025
        val barnetilleggFom2 = 1 september 2025
        val barnetilleggTom2 = 31 desember 2025
        val tidligsteReduksjonsdato = fom.withDayOfMonth(1).plusMonths(4)
        val barnFødselsdato = fom.minusYears(8)

        val barn = lagBarn(barnFødselsdato)
        val person = TestPersoner.STANDARD_PERSON()
            .medBarn(listOf(barn))
            .medInstitusjonsopphold(listOf(hsOpphold(startdato = fom, sluttdato = oppholdTom)))

        val (_, behandling) = sendInnFørsteSøknad(
            person = person,
            mottattTidspunkt = fom.atStartOfDay(),
        )

        behandling
            .løsSykdom(fom)
            .løsBistand(fom)
            .løsRefusjonskrav()
            .løsSykdomsvurderingBrev()
            .kvalitetssikre()
            .løsBeregningstidspunkt()
            .løsOppholdskrav(fom)
            .løsAvklaringsBehov(
                løsBarnetilleggMedToPerioder(
                    barn,
                    fom,
                    barnetilleggTom1,
                    barnetilleggFom2,
                    barnetilleggTom2
                )
            )
            .medKontekst {
                assertThat(åpneAvklaringsbehov.map { it.definisjon }).contains(Definisjon.AVKLAR_HELSEINSTITUSJON)
            }
            .løsAvklaringsBehov(løsHelseinstitusjonMedReduksjon(fom, oppholdTom))
            .løsAndreStatligeYtelser()
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .fattVedtak()
            .løsVedtaksbrev()
            .medKontekst {
                val tilkjentYtelse = hentTilkjentYtelse(behandling.id)
                val tidslinje = tilkjentYtelse.map { Segment(it.periode, it.tilkjent) }.let(::Tidslinje)

                assertThat(tidslinje.isNotEmpty()).isTrue()
                val periodeMedBarnetillegUnderInstitusjonsopphold =
                    Periode(tidligsteReduksjonsdato, barnetilleggTom1.minusDays(1))

                // Verifiser at barnetillegg ikke er 0 i perioden med institusjon-reduksjon
                val tilkjentMedBarnetilleggOgReduksjon =
                    tidslinje.begrensetTil(periodeMedBarnetillegUnderInstitusjonsopphold)
                tilkjentMedBarnetilleggOgReduksjon.segmenter().forEach { segment ->
                    assertThat(segment.verdi.barnetillegg)
                        .`as`("Barnetillegg skal ikke bortfalle ved institusjonsreduksjon")
                        .isEqualTo(Beløp(37))
                }

                val periodeMedInstitusjonsoppholdReduksjon = Periode(barnetilleggTom1, oppholdTom)
                val tilkjentMedInstitusjonsoppholdReduksjon =
                    tidslinje.begrensetTil(periodeMedInstitusjonsoppholdReduksjon)
                tilkjentMedInstitusjonsoppholdReduksjon.segmenter().forEach { segment ->
                    assertThat(segment.verdi.graderingGrunnlag.institusjonGradering)
                        .`as`("Institusjonsgradering skal være 50% i perioden med institusjonsopphold")
                        .isEqualTo(Prosent.`50_PROSENT`)
                }

                val periodeMedBarnetilleggEtterInstitusjonsopphold =
                    Periode(barnetilleggFom2, barnetilleggTom2.minusDays(1))
                val tilkjentMedBarnetilleggEtterInstitusjonsopphold =
                    tidslinje.begrensetTil(periodeMedBarnetilleggEtterInstitusjonsopphold)
                tilkjentMedBarnetilleggEtterInstitusjonsopphold.segmenter().forEach { segment ->
                    assertThat(segment.verdi.barnetillegg)
                        .`as`("Barnetillegg etter institusjonsopphold")
                        .isEqualTo(Beløp(37))
                    assertThat(segment.verdi.graderingGrunnlag.institusjonGradering).isEqualTo(Prosent.`0_PROSENT`)
                }
            }
    }

    @Test
    fun `to barnetillegg perioder med to institusjonsopphold som overlapper hverandre`() {
        val oppholdFom1 = 1 januar 2025
        val oppholdTom1 = 1 oktober 2025
        val oppholdFom2 = 1 november 2025
        val oppholdTom2 = 1 januar 2026
        val barnetilleggFom1 = 20 mai 2025
        val barnetilleggTom1 = 1 juli 2025
        val barnetilleggFom2 = 16 november 2025
        val barnetilleggTom2 = 1 desember 2025
        val tidligsteReduksjonsdato1 = oppholdFom1.withDayOfMonth(1).plusMonths(4)
        val barnFødselsdato = oppholdFom1.minusYears(5)

        val barn = lagBarn(barnFødselsdato)
        val person = TestPersoner.STANDARD_PERSON()
            .medBarn(listOf(barn))
            .medInstitusjonsopphold(
                listOf(
                    hsOpphold(startdato = oppholdFom1, sluttdato = oppholdTom1),
                    hsOpphold(startdato = oppholdFom2, sluttdato = oppholdTom2)
                )
            )

        val (_, behandling) = sendInnFørsteSøknad(
            person = person,
            mottattTidspunkt = oppholdFom1.atStartOfDay(),
        )

        behandling
            .løsSykdom(oppholdFom1)
            .løsBistand(oppholdFom1)
            .løsRefusjonskrav()
            .løsSykdomsvurderingBrev()
            .kvalitetssikre()
            .løsBeregningstidspunkt()
            .løsOppholdskrav(oppholdFom1)
            .løsAvklaringsBehov(
                løsBarnetilleggMedToPerioder(
                    barn,
                    barnetilleggFom1,
                    barnetilleggTom1,
                    barnetilleggFom2,
                    barnetilleggTom2
                )
            )
            .medKontekst {
                assertThat(åpneAvklaringsbehov.map { it.definisjon }).contains(Definisjon.AVKLAR_HELSEINSTITUSJON)
            }
            .løsAvklaringsBehov(løsToHelseinstitusjonMedReduksjon(oppholdFom1, oppholdTom1, oppholdFom2, oppholdTom2))
            .løsAndreStatligeYtelser()
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .fattVedtak()
            .løsVedtaksbrev()
            .medKontekst {
                val tilkjentYtelse = hentTilkjentYtelse(behandling.id)
                val tidslinje = tilkjentYtelse.map { Segment(it.periode, it.tilkjent) }.let(::Tidslinje)

                // Verifisere perioder med barnetillegg
                assertThat(tidslinje.isNotEmpty()).isTrue()
                val periodeMedBarnetillegg1 =
                    Periode(barnetilleggFom1, barnetilleggTom1.minusDays(1))

                val tilkjentPeriodeMedBarnetillegg1 =
                    tidslinje.begrensetTil(periodeMedBarnetillegg1)
                tilkjentPeriodeMedBarnetillegg1.segmenter().forEach { segment ->
                    assertThat(segment.verdi.barnetillegg)
                        .`as`("Barnetillegg periode 1")
                        .isEqualTo(Beløp(37))
                }

                val periodePåOpphold1MedReduksjon1 = Periode(tidligsteReduksjonsdato1, barnetilleggFom1.minusDays(1))
                val tilkjentPeriodePåOpphold1MedReduksjon1 =
                    tidslinje.begrensetTil(periodePåOpphold1MedReduksjon1)
                tilkjentPeriodePåOpphold1MedReduksjon1.segmenter().forEach { segment ->
                    assertThat(segment.verdi.graderingGrunnlag.institusjonGradering)
                        .`as`("Institusjonsgradering for opphold1 skal være 50% i periode 1 med institusjonsopphold")
                        .isEqualTo(Prosent.`50_PROSENT`)
                }

                val periodePåOpphold1MedReduksjon2 = Periode(barnetilleggTom1, oppholdTom1)
                val tilkjentPeriodePåOpphold1MedReduksjon2 =
                    tidslinje.begrensetTil(periodePåOpphold1MedReduksjon2)
                tilkjentPeriodePåOpphold1MedReduksjon2.segmenter().forEach { segment ->
                    assertThat(segment.verdi.graderingGrunnlag.institusjonGradering)
                        .`as`("Institusjonsgradering for opphold1 skal være 50% i periode 2 med institusjonsopphold")
                        .isEqualTo(Prosent.`50_PROSENT`)
                }

                val periodeMedBarnetillegg2 =
                    Periode(barnetilleggFom2, barnetilleggTom2.minusDays(1))

                val tilkjentPeriodeMedBarnetillegg2 =
                    tidslinje.begrensetTil(periodeMedBarnetillegg2)
                tilkjentPeriodeMedBarnetillegg2.segmenter().forEach { segment ->
                    assertThat(segment.verdi.barnetillegg)
                        .`as`("Barnetillegg periode 2")
                        .isEqualTo(Beløp(37))
                }

                val periodePåOpphold2MedReduksjon1 = Periode(barnetilleggTom2, oppholdTom2)
                val tilkjentPeriodePåOpphold2MedReduksjon1 =
                    tidslinje.begrensetTil(periodePåOpphold2MedReduksjon1)
                tilkjentPeriodePåOpphold2MedReduksjon1.segmenter().forEach { segment ->
                    assertThat(segment.verdi.graderingGrunnlag.institusjonGradering)
                        .`as`("Institusjonsgradering for opphold2 skal være 50% i periode 1")
                        .isEqualTo(Prosent.`50_PROSENT`)
                }

                val periodePåOpphold2MedReduksjon2 = Periode(barnetilleggTom2, oppholdTom2)
                val tilkjentPeriodePåOpphold2MedReduksjon2 =
                    tidslinje.begrensetTil(periodePåOpphold2MedReduksjon2)
                tilkjentPeriodePåOpphold2MedReduksjon2.segmenter().forEach { segment ->
                    assertThat(segment.verdi.graderingGrunnlag.institusjonGradering)
                        .`as`("Institusjonsgradering for opphold2 skal være 50% i periode 2")
                        .isEqualTo(Prosent.`50_PROSENT`)
                }
            }
    }

    // -------------------------------------------------------------------------
    // Revurdering
    // -------------------------------------------------------------------------

    @Test
    fun `revurdering av institusjonsopphold omgjør fra reduksjon til ingen reduksjon`() {
        val fom = LocalDate.now().minusMonths(5)
        val oppholdTom = LocalDate.now().plusMonths(7)
        val tidligsteReduksjonsdato = fom.withDayOfMonth(1).plusMonths(4)

        val (sak, behandling) = sendInnFørsteSøknad(
            person = TestPersoner.STANDARD_PERSON().medInstitusjonsopphold(
                listOf(hsOpphold(startdato = fom, sluttdato = oppholdTom))
            ),
            mottattTidspunkt = fom.atStartOfDay(),
        )

        // Fullfør førstegangsbehandling med reduksjon
        behandling
            .løsSykdom(fom)
            .løsBistand(fom)
            .løsRefusjonskrav()
            .løsSykdomsvurderingBrev()
            .kvalitetssikre()
            .løsBeregningstidspunkt()
            .løsOppholdskrav(fom)
            .løsAvklaringsBehov(løsHelseinstitusjonMedReduksjon(fom, oppholdTom))
            .løsAndreStatligeYtelser()
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .fattVedtak()
            .løsVedtaksbrev()
            .medKontekst {
                val tilkjentYtelse = hentTilkjentYtelse(behandling.id)
                val tidslinje = tilkjentYtelse.map { Segment(it.periode, it.tilkjent) }.let(::Tidslinje)

                assertThat(tidslinje.isNotEmpty()).isTrue()

                val periodeMedReduksjon = Periode(tidligsteReduksjonsdato, oppholdTom.minusDays(1))
                val tilkjentPeriodeMedReduksjon = tidslinje.begrensetTil(periodeMedReduksjon)
                tilkjentPeriodeMedReduksjon.segmenter().forEach { segment ->
                    assertThat(segment.verdi.graderingGrunnlag.institusjonGradering)
                        .`as`("Institusjonsgradering skal være 50% i perioden med institusjonsopphold")
                        .isEqualTo(Prosent.`50_PROSENT`)
                }
            }

        // Revurder og omgjør til ingen reduksjon
        val revurdering = sak.opprettManuellRevurdering(listOf(Vurderingsbehov.INSTITUSJONSOPPHOLD))

        revurdering
            .medKontekst {
                assertThat(revurdering.typeBehandling()).isEqualTo(TypeBehandling.Revurdering)
                assertThat(åpneAvklaringsbehov.map { it.definisjon }).contains(Definisjon.AVKLAR_HELSEINSTITUSJON)
            }
            .løsAvklaringsBehov(løsHelseinstitusjonUtenReduksjon(fom, oppholdTom, "Omgjør: forsørger ektefelle"))
            .løsAndreStatligeYtelser()
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .fattVedtak()
            .medKontekst {
                val tilkjentYtelse = hentTilkjentYtelse(revurdering.id)
                val tidslinje = tilkjentYtelse.map { Segment(it.periode, it.tilkjent) }.let(::Tidslinje)
                val helePerioden = tilkjentYtelse.tilTidslinje().helePerioden()

                assertTidslinje(
                    tidslinje.begrensetTil(helePerioden),
                    helePerioden to {
                        assertThat(it.graderingGrunnlag.institusjonGradering).isEqualTo(Prosent.`0_PROSENT`)
                    }
                )
            }
    }

    @Test
    fun `revurdering med barnetillegg - fjerning av barnetillegg gir reduksjon i samme periode`() {
        val fom = 5 februar 2025
        val oppholdTom = 14 september 2025
        val barnetilleggFom = fom.minusDays(8)
        val barnetilleggTom = oppholdTom
        val tidligsteReduksjonsdato = fom.withDayOfMonth(1).plusMonths(4)
        val barnFødselsdato = fom.minusYears(6)

        val barn = lagBarn(barnFødselsdato)
        val person = TestPersoner.STANDARD_PERSON()
            .medBarn(listOf(barn))
            .medInstitusjonsopphold(listOf(hsOpphold(startdato = fom, sluttdato = oppholdTom)))

        val (sak, behandling) = sendInnFørsteSøknad(
            person = person,
            mottattTidspunkt = fom.atStartOfDay(),
        )

        behandling
            .løsSykdom(fom)
            .løsBistand(fom)
            .løsRefusjonskrav()
            .løsSykdomsvurderingBrev()
            .kvalitetssikre()
            .løsBeregningstidspunkt()
            .løsOppholdskrav(fom)
            .løsAvklaringsBehov(løsBarnetilleggFraOgTilDato(barn, barnetilleggFom, barnetilleggTom))
            .løsAvklaringsBehov(løsHelseinstitusjonMedReduksjon(fom, oppholdTom))
            .løsAndreStatligeYtelser()
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .fattVedtak()
            .løsVedtaksbrev()
            .medKontekst {

                val tilkjentYtelse = hentTilkjentYtelse(behandling.id)
                val tidslinje = tilkjentYtelse.map { Segment(it.periode, it.tilkjent) }.let(::Tidslinje)

                assertThat(tidslinje.isNotEmpty()).isTrue()

                val periodeMedBarnetillegg = Periode(fom, barnetilleggTom.minusDays(1))
                val tilkjentPeriodeMedBarnetillegg = tidslinje.begrensetTil(periodeMedBarnetillegg)
                tilkjentPeriodeMedBarnetillegg.segmenter().forEach { segment ->
                    assertThat(segment.verdi.barnetillegg)
                        .`as`("Barnetillegg skal være tilstede i førstegangsbehandling")
                        .isEqualTo(Beløp(37))

                    assertThat(segment.verdi.graderingGrunnlag.institusjonGradering)
                        .`as`("Institusjonsgradering skal være 0% i perioden med barnetillegg")
                        .isEqualTo(Prosent.`0_PROSENT`)
                }
            }

        // Revurder og fjern barnetillegg
        val revurdering =
            sak.opprettManuellRevurdering(listOf(Vurderingsbehov.BARNETILLEGG))

        revurdering
            .løsAvklaringsBehov(løsNeiTilBarnetillegg(barn, barnetilleggFom))
            .løsAvklaringsBehov(løsHelseinstitusjonMedReduksjon(fom, oppholdTom))
            .løsAndreStatligeYtelser()
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .fattVedtak()
            .medKontekst {
                val tilkjentYtelse = hentTilkjentYtelse(revurdering.id)
                val tidslinje = tilkjentYtelse.map { Segment(it.periode, it.tilkjent) }.let(::Tidslinje)

                val periodeMedReduksjon = Periode(tidligsteReduksjonsdato, oppholdTom)
                val tilkjentPeriodeMedReduksjon = tidslinje.begrensetTil(periodeMedReduksjon)
                tilkjentPeriodeMedReduksjon.segmenter().forEach { segment ->
                    assertThat(segment.verdi.barnetillegg)
                        .`as`("Barnetillegg er fjernet fra revurdering")
                        .isEqualTo(Beløp(0))

                    assertThat(segment.verdi.graderingGrunnlag.institusjonGradering)
                        .`as`("Institusjonsgradering skal være 50% i perioden med barnetillegg")
                        .isEqualTo(Prosent.`50_PROSENT`)
                }
            }
    }

    @Test
    fun `fra pågående institusjonsopphold til avsluttet opphold med gitt sluttdato`() {
        val fom = LocalDate.now().minusMonths(5)
        val pågåendeOpphold = Tid.MAKS;
        val tidligsteReduksjonsdato = fom.withDayOfMonth(1).plusMonths(4)

        val (sak, behandling) = sendInnFørsteSøknad(
            person = TestPersoner.STANDARD_PERSON().medInstitusjonsopphold(
                listOf(hsOpphold(startdato = fom, sluttdato = null))
            ),
            mottattTidspunkt = fom.atStartOfDay(),
        )

        // Fullfør førstegangsbehandling med reduksjon
        behandling
            .løsSykdom(fom)
            .løsBistand(fom)
            .løsRefusjonskrav()
            .løsSykdomsvurderingBrev()
            .kvalitetssikre()
            .løsBeregningstidspunkt()
            .løsOppholdskrav(fom)
            .løsAvklaringsBehov(løsHelseinstitusjonMedReduksjon(fom, pågåendeOpphold))
            .løsAndreStatligeYtelser()
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .fattVedtak()
            .løsVedtaksbrev()
            .medKontekst {
                val tilkjentYtelse = hentTilkjentYtelse(behandling.id)
                val tidslinje = tilkjentYtelse.map { Segment(it.periode, it.tilkjent) }.let(::Tidslinje)

                assertThat(tidslinje.isNotEmpty()).isTrue()

                val periodeMedReduksjon = Periode(tidligsteReduksjonsdato, pågåendeOpphold)
                val tilkjentPeriodeMedReduksjon = tidslinje.begrensetTil(periodeMedReduksjon)
                tilkjentPeriodeMedReduksjon.segmenter().forEach { segment ->
                    assertThat(segment.verdi.graderingGrunnlag.institusjonGradering)
                        .`as`("Institusjonsgradering skal være 50% i perioden med institusjonsopphold")
                        .isEqualTo(Prosent.`50_PROSENT`)
                }
            }

        // Revurder og sett sluttdato for oppholdet
        val revurdering = sak.opprettManuellRevurdering(listOf(Vurderingsbehov.INSTITUSJONSOPPHOLD))
        val oppholdTom = LocalDate.now().plusMonths(5)

        revurdering
            .medKontekst {
                assertThat(revurdering.typeBehandling()).isEqualTo(TypeBehandling.Revurdering)
                assertThat(åpneAvklaringsbehov.map { it.definisjon }).contains(Definisjon.AVKLAR_HELSEINSTITUSJON)

                // Setter sluttdato for oppholdet
                repositoryProvider.provide<InstitusjonsoppholdRepository>().lagreOpphold(
                    revurdering.id, listOf(
                        Institusjonsopphold(
                            startdato = fom,
                            sluttdato = oppholdTom,
                            institusjonstype = Institusjonstype.HS,
                            institusjonsnavn = "Testinstitusjon",
                            orgnr = "111222333",
                            kategori = Oppholdstype.H
                        )
                    )
                )
            }
            .løsAvklaringsBehov(løsHelseinstitusjonMedReduksjon(fom, oppholdTom))
            .løsAndreStatligeYtelser()
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .fattVedtak()
            .medKontekst {
                val tilkjentYtelse = hentTilkjentYtelse(revurdering.id)
                val tidslinje = tilkjentYtelse.map { Segment(it.periode, it.tilkjent) }.let(::Tidslinje)

                val periodeMedReduksjon = Periode(tidligsteReduksjonsdato, oppholdTom)
                val tilkjentPeriodeMedReduksjon = tidslinje.begrensetTil(periodeMedReduksjon)
                tilkjentPeriodeMedReduksjon.segmenter().forEach { segment ->
                    assertThat(segment.verdi.graderingGrunnlag.institusjonGradering)
                        .`as`("Institusjonsgradering skal være 50% i perioden med institusjonsopphold")
                        .isEqualTo(Prosent.`50_PROSENT`)
                }
            }
    }

    @Test
    fun `gitt sluttdato på opphold som blir forlenget eller utvidet gir riktig forlenget reduksjon`() {
        val fom = LocalDate.now().minusMonths(5)
        val oppholdTom = LocalDate.now().plusMonths(3)
        val tidligsteReduksjonsdato = fom.withDayOfMonth(1).plusMonths(4)

        val (sak, behandling) = sendInnFørsteSøknad(
            person = TestPersoner.STANDARD_PERSON().medInstitusjonsopphold(
                listOf(hsOpphold(startdato = fom, sluttdato = oppholdTom))
            ),
            mottattTidspunkt = fom.atStartOfDay(),
        )

        // Fullfør førstegangsbehandling med reduksjon
        behandling
            .løsSykdom(fom)
            .løsBistand(fom)
            .løsRefusjonskrav()
            .løsSykdomsvurderingBrev()
            .kvalitetssikre()
            .løsBeregningstidspunkt()
            .løsOppholdskrav(fom)
            .løsAvklaringsBehov(løsHelseinstitusjonMedReduksjon(fom, oppholdTom))
            .løsAndreStatligeYtelser()
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .fattVedtak()
            .løsVedtaksbrev()
            .medKontekst {
                val tilkjentYtelse = hentTilkjentYtelse(behandling.id)
                val tidslinje = tilkjentYtelse.map { Segment(it.periode, it.tilkjent) }.let(::Tidslinje)

                assertThat(tidslinje.isNotEmpty()).isTrue()

                val periodeMedReduksjon = Periode(tidligsteReduksjonsdato, oppholdTom)
                val tilkjentPeriodeMedReduksjon = tidslinje.begrensetTil(periodeMedReduksjon)
                tilkjentPeriodeMedReduksjon.segmenter().forEach { segment ->
                    assertThat(segment.verdi.graderingGrunnlag.institusjonGradering)
                        .`as`("Institusjonsgradering skal være 50% i perioden med institusjonsopphold")
                        .isEqualTo(Prosent.`50_PROSENT`)
                }
            }

        // Revurder og forlenger sluttdato
        val revurdering = sak.opprettManuellRevurdering(listOf(Vurderingsbehov.INSTITUSJONSOPPHOLD))
        val utvidetOppholdTom = LocalDate.now().plusMonths(5)

        revurdering
            .medKontekst {
                assertThat(revurdering.typeBehandling()).isEqualTo(TypeBehandling.Revurdering)
                assertThat(åpneAvklaringsbehov.map { it.definisjon }).contains(Definisjon.AVKLAR_HELSEINSTITUSJON)

                // Forlenger sluttdato
                repositoryProvider.provide<InstitusjonsoppholdRepository>().lagreOpphold(
                    revurdering.id, listOf(
                        Institusjonsopphold(
                            startdato = fom,
                            sluttdato = utvidetOppholdTom,
                            institusjonstype = Institusjonstype.HS,
                            institusjonsnavn = "Testinstitusjon",
                            orgnr = "111222333",
                            kategori = Oppholdstype.H
                        )
                    )
                )
            }
            .løsAvklaringsBehov(løsHelseinstitusjonMedReduksjon(fom, utvidetOppholdTom))
            .løsAndreStatligeYtelser()
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .fattVedtak()
            .medKontekst {
                val tilkjentYtelse = hentTilkjentYtelse(revurdering.id)
                val tidslinje = tilkjentYtelse.map { Segment(it.periode, it.tilkjent) }.let(::Tidslinje)

                val periodeMedReduksjon = Periode(tidligsteReduksjonsdato, utvidetOppholdTom)
                val tilkjentPeriodeMedReduksjon = tidslinje.begrensetTil(periodeMedReduksjon)
                tilkjentPeriodeMedReduksjon.segmenter().forEach { segment ->
                    assertThat(segment.verdi.graderingGrunnlag.institusjonGradering)
                        .`as`("Institusjonsgradering skal være 50% i perioden med institusjonsopphold")
                        .isEqualTo(Prosent.`50_PROSENT`)
                }
            }
    }

    // -------------------------------------------------------------------------
    // Hjelpemetoder
    // -------------------------------------------------------------------------

    private fun hsOpphold(startdato: LocalDate, sluttdato: LocalDate?, kategori: String = "H") =
        InstitusjonsoppholdJSON(
            startdato = startdato,
            forventetSluttdato = sluttdato,
            institusjonstype = "HS",
            institusjonsnavn = "Testinstitusjon",
            organisasjonsnummer = "111222333",
            kategori = kategori,
        )

    private fun BehandlingInfo.hentTilkjentYtelse(behandlingId: BehandlingId) =
        requireNotNull(
            repositoryProvider.provide<TilkjentYtelseRepository>().hentHvisEksisterer(behandlingId)
        ) { "Tilkjent ytelse skal være beregnet her." }

    private fun lagBarn(fødselsdato: LocalDate): TestPerson {
        val ident = genererIdent(fødselsdato)
        return TestPerson(
            identer = setOf(ident),
            navn = PersonNavn("Tuva", "Hansen"),
            fødselsdato = Fødselsdato(fødselsdato),
        )
    }

    private fun løsBarnetilleggMedToPerioder(
        barn: TestPerson,
        fraDato1: LocalDate,
        tilDato1: LocalDate,
        fraDato2: LocalDate,
        tilDato2: LocalDate,
    ): AvklarBarnetilleggLøsning {
        return AvklarBarnetilleggLøsning(
            vurderingerForBarnetillegg = VurderingerForBarnetillegg(
                vurderteBarn = listOf(
                    VurdertBarnDto(
                        ident = barn.identer.first().identifikator,
                        navn = barn.navn.fornavn + " " + barn.navn.etternavn,
                        fødselsdato = barn.fødselsdato.toLocalDate(),
                        oppgittForeldreRelasjon = Relasjon.FORELDER,
                        dødsdato = null,
                        vurderinger = listOf(
                            VurderingAvForeldreAnsvarDto(
                                harForeldreAnsvar = true,
                                begrunnelse = "Barnetillegg periode 1 start",
                                fraDato = fraDato1
                            ),
                            VurderingAvForeldreAnsvarDto(
                                harForeldreAnsvar = false,
                                begrunnelse = "Barnetillegg periode 1 slutt",
                                fraDato = tilDato1
                            ),
                            VurderingAvForeldreAnsvarDto(
                                harForeldreAnsvar = true,
                                begrunnelse = "Barnetillegg periode 2 start",
                                fraDato = fraDato2
                            ),
                            VurderingAvForeldreAnsvarDto(
                                harForeldreAnsvar = false,
                                begrunnelse = "Barnetillegg periode 2 slutt",
                                fraDato = tilDato2
                            ),
                        )
                    )
                ),
                saksbehandlerOppgitteBarn = emptyList()
            )
        )
    }

    private fun løsNeiTilBarnetillegg(
        barn: TestPerson,
        fraDato: LocalDate
    ): AvklarBarnetilleggLøsning {
        return AvklarBarnetilleggLøsning(
            vurderingerForBarnetillegg = VurderingerForBarnetillegg(
                vurderteBarn = listOf(
                    VurdertBarnDto(
                        ident = barn.identer.first().identifikator,
                        navn = barn.navn.fornavn + " " + barn.navn.etternavn,
                        fødselsdato = barn.fødselsdato.toLocalDate(),
                        oppgittForeldreRelasjon = Relasjon.FORELDER,
                        dødsdato = null,
                        vurderinger = listOf(
                            VurderingAvForeldreAnsvarDto(
                                harForeldreAnsvar = false,
                                begrunnelse = "Har foreldreansvar",
                                fraDato = fraDato
                            )
                        ),
                    )
                ),
                saksbehandlerOppgitteBarn = emptyList()
            )
        )
    }

    private fun løsBarnetilleggFraOgTilDato(
        barn: TestPerson,
        fraDato: LocalDate,
        tilDato: LocalDate?
    ): AvklarBarnetilleggLøsning {
        return AvklarBarnetilleggLøsning(
            vurderingerForBarnetillegg = VurderingerForBarnetillegg(
                vurderteBarn = listOf(
                    VurdertBarnDto(
                        ident = barn.identer.first().identifikator,
                        navn = barn.navn.fornavn + " " + barn.navn.etternavn,
                        fødselsdato = barn.fødselsdato.toLocalDate(),
                        oppgittForeldreRelasjon = Relasjon.FORELDER,
                        dødsdato = null,
                        vurderinger = listOf(
                            VurderingAvForeldreAnsvarDto(
                                harForeldreAnsvar = true,
                                begrunnelse = "Har foreldreansvar",
                                fraDato = fraDato
                            ),
                            // Legger til denne vurderingen kun hvis tilDato ikke er null
                        ).let { vurderinger ->
                            if (tilDato != null) {
                                vurderinger + VurderingAvForeldreAnsvarDto(
                                    harForeldreAnsvar = false,
                                    begrunnelse = "Har ikke foreldreansvar",
                                    fraDato = tilDato
                                )
                            } else vurderinger
                        }
                    )
                ),
                saksbehandlerOppgitteBarn = emptyList()
            )
        )
    }

    private fun løsToHelseinstitusjonMedReduksjon(
        oppholdFom1: LocalDate,
        oppholdTom1: LocalDate,
        oppholdFom2: LocalDate,
        oppholdTom2: LocalDate,
    ): AvklarHelseinstitusjonLøsning {
        val tidligsteReduksjonsdato1 = oppholdFom1.withDayOfMonth(1).plusMonths(4)
        val tidligsteReduksjonsdato2 = oppholdFom2.withDayOfMonth(1).plusMonths(1)
        return AvklarHelseinstitusjonLøsning(
            helseinstitusjonVurdering = HelseinstitusjonVurderingerDto(
                vurderinger = listOf(
                    HelseinstitusjonVurderingDto(
                        periode = Periode(tidligsteReduksjonsdato1, oppholdTom1),
                        begrunnelse = "Innlagt med kost og losji",
                        faarFriKostOgLosji = true,
                        forsoergerEktefelle = false,
                        harFasteUtgifter = false,
                    ),
                    HelseinstitusjonVurderingDto(
                        periode = Periode(tidligsteReduksjonsdato2, oppholdTom2),
                        begrunnelse = "Innlagt med kost og losji",
                        faarFriKostOgLosji = true,
                        forsoergerEktefelle = false,
                        harFasteUtgifter = false,
                    )
                )
            )
        )
    }

    private fun løsHelseinstitusjonMedReduksjon(
        oppholdFom: LocalDate,
        oppholdTom: LocalDate,
    ): AvklarHelseinstitusjonLøsning {
        val tidligsteReduksjonsdato = oppholdFom.withDayOfMonth(1).plusMonths(4)
        return AvklarHelseinstitusjonLøsning(
            helseinstitusjonVurdering = HelseinstitusjonVurderingerDto(
                vurderinger = listOf(
                    HelseinstitusjonVurderingDto(
                        periode = Periode(tidligsteReduksjonsdato, oppholdTom),
                        begrunnelse = "Innlagt med kost og losji",
                        faarFriKostOgLosji = true,
                        forsoergerEktefelle = false,
                        harFasteUtgifter = false,
                    )
                )
            )
        )
    }

    private fun løsHelseinstitusjonUtenReduksjon(
        oppholdFom: LocalDate,
        oppholdTom: LocalDate,
        begrunnelse: String,
    ): AvklarHelseinstitusjonLøsning {
        return AvklarHelseinstitusjonLøsning(
            helseinstitusjonVurdering = HelseinstitusjonVurderingerDto(
                vurderinger = listOf(
                    HelseinstitusjonVurderingDto(
                        periode = Periode(oppholdFom, oppholdTom),
                        begrunnelse = begrunnelse,
                        faarFriKostOgLosji = false,
                        forsoergerEktefelle = true,
                        harFasteUtgifter = false,
                    )
                )
            )
        )
    }
}