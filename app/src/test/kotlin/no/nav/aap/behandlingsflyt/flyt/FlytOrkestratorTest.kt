package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.Fakes
import no.nav.aap.behandlingsflyt.avklaringsbehov.AvklaringsbehovHendelseHåndterer
import no.nav.aap.behandlingsflyt.avklaringsbehov.AvklaringsbehovRepositoryImpl
import no.nav.aap.behandlingsflyt.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.avklaringsbehov.LøsAvklaringsbehovBehandlingHendelse
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.bistand.AvklarBistandsbehovLøsning
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.student.AvklarStudentLøsning
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.sykdom.AvklarSykdomLøsning
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.sykdom.AvklarYrkesskadeLøsning
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.sykdom.YrkesskadevurderingDto
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.vedtak.FatteVedtakLøsning
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.vedtak.ForeslåVedtakLøsning
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.vedtak.TotrinnsVurdering
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.dbconnect.transaction
import no.nav.aap.behandlingsflyt.dbtest.InitTestDatabase
import no.nav.aap.behandlingsflyt.dbtestdata.ident
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.ArbeidIPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektPerÅr
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.adapter.InntektRegisterMock
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Personopplysning
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.adapter.FakePersonopplysningGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.adapter.YrkesskadeRegisterMock
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.BistandVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.NedreGrense
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Sykdomsvurdering
import no.nav.aap.behandlingsflyt.hendelse.mottak.BehandlingSattPåVent
import no.nav.aap.behandlingsflyt.hendelse.mottak.DokumentMottattPersonHendelse
import no.nav.aap.behandlingsflyt.hendelse.mottak.HendelsesMottak
import no.nav.aap.behandlingsflyt.hendelse.mottak.dokument.StrukturertDokument
import no.nav.aap.behandlingsflyt.hendelse.mottak.dokument.pliktkort.Pliktkort
import no.nav.aap.behandlingsflyt.hendelse.mottak.dokument.søknad.Søknad
import no.nav.aap.behandlingsflyt.prosessering.ProsesseringsOppgaver
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.dokumenter.Brevkode
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.PdlIdentGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.db.PersonRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.db.SakRepositoryImpl
import no.nav.aap.motor.Motor
import no.nav.aap.motor.OppgaveRepository
import no.nav.aap.verdityper.Beløp
import no.nav.aap.verdityper.Periode
import no.nav.aap.verdityper.Prosent
import no.nav.aap.verdityper.TimerArbeid
import no.nav.aap.verdityper.dokument.JournalpostId
import no.nav.aap.verdityper.flyt.StegStatus
import no.nav.aap.verdityper.flyt.StegType
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import no.nav.aap.verdityper.sakogbehandling.Ident
import no.nav.aap.verdityper.sakogbehandling.SakId
import no.nav.aap.verdityper.sakogbehandling.Status
import no.nav.aap.verdityper.sakogbehandling.TypeBehandling
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Year

class FlytOrkestratorTest {

    companion object {
        val dataSource = InitTestDatabase.dataSource
        val motor = Motor(dataSource, 1, ProsesseringsOppgaver.alle())
        val hendelsesMottak = HendelsesMottak(dataSource)
        val fakes = Fakes()

        @BeforeAll
        @JvmStatic
        internal fun beforeAll() {
            motor.start()
            PdlIdentGateway.init(fakes.azureConf, fakes.pdlConf)
        }

        @AfterAll
        @JvmStatic
        internal fun afterAll() {
            motor.stop()
            fakes.close()
        }
    }

    @Test
    fun `skal avklare yrkesskade hvis det finnes spor av yrkesskade`() {
        val ident = ident()
        val fom = LocalDate.now().minusMonths(3)
        val periode = Periode(fom, fom.plusYears(3))

        // Simulerer et svar fra YS-løsning om at det finnes en yrkesskade
        FakePersonopplysningGateway.konstruer(ident, Personopplysning(Fødselsdato(fom.minusYears(18))))
        YrkesskadeRegisterMock.konstruer(ident = ident, periode = periode)
        InntektRegisterMock.konstruer(
            ident = ident, inntekterPerÅr = listOf(
                InntektPerÅr(
                    Year.now().minusYears(3),
                    Beløp(
                        BigDecimal(1000000)
                    )
                )
            )
        )

        // Sender inn en søknad
        hendelsesMottak.håndtere(
            ident, DokumentMottattPersonHendelse(
                journalpost = JournalpostId("20"),
                mottattTidspunkt = LocalDateTime.now().minusMonths(3),
                strukturertDokument = StrukturertDokument(Søknad(periode), Brevkode.SØKNAD)
            )
        )
        ventPåSvar()

        val sak = hentSak(ident, periode)
        var behandling = hentBehandling(sak.id)
        assertThat(behandling.typeBehandling()).isEqualTo(TypeBehandling.Førstegangsbehandling)

        dataSource.transaction {
            val avklaringsbehov = hentAvklaringsbehov(behandling.id, it)
            assertThat(avklaringsbehov.alle()).isNotEmpty()
            assertThat(behandling.status()).isEqualTo(Status.UTREDES)
        }

        dataSource.transaction {
            AvklaringsbehovHendelseHåndterer(it).håndtere(
                behandling.id,
                LøsAvklaringsbehovBehandlingHendelse(
                    løsning = AvklarStudentLøsning(
                        studentvurdering = StudentVurdering(
                            begrunnelse = "Er student",
                            dokumenterBruktIVurdering = listOf(JournalpostId("123123")),
                            oppfyller11_14 = false,
                            avbruttStudieDato = null
                        )
                    ),
                    behandlingVersjon = behandling.versjon
                )
            )
        }
        ventPåSvar()

        dataSource.transaction {
            AvklaringsbehovHendelseHåndterer(it).håndtere(
                behandling.id,
                LøsAvklaringsbehovBehandlingHendelse(
                    løsning = AvklarYrkesskadeLøsning(
                        yrkesskadevurdering = YrkesskadevurderingDto(
                            begrunnelse = "Er syk nok",
                            dokumenterBruktIVurdering = listOf(JournalpostId("123123")),
                            erÅrsakssammenheng = false,
                            skadetidspunkt = null,
                            andelAvNedsettelse = null,
                            antattÅrligInntekt = null
                        )
                    ),
                    behandlingVersjon = behandling.versjon
                )
            )
        }
        ventPåSvar()

        dataSource.transaction {
            AvklaringsbehovHendelseHåndterer(it).håndtere(
                behandling.id,
                LøsAvklaringsbehovBehandlingHendelse(
                    løsning = AvklarSykdomLøsning(
                        sykdomsvurdering = Sykdomsvurdering(
                            begrunnelse = "Er syk nok",
                            dokumenterBruktIVurdering = listOf(JournalpostId("123123")),
                            erSkadeSykdomEllerLyteVesentligdel = true,
                            erNedsettelseIArbeidsevneHøyereEnnNedreGrense = true,
                            nedreGrense = NedreGrense.FEMTI,
                            nedsattArbeidsevneDato = LocalDate.now(),
                            ytterligereNedsattArbeidsevneDato = null
                        )
                    ),
                    behandlingVersjon = behandling.versjon
                )
            )
        }
        ventPåSvar()

        dataSource.transaction {
            AvklaringsbehovHendelseHåndterer(it).håndtere(
                behandling.id,
                LøsAvklaringsbehovBehandlingHendelse(
                    løsning = AvklarBistandsbehovLøsning(
                        bistandVurdering = BistandVurdering(
                            begrunnelse = "Trenger hjelp fra nav",
                            erBehovForBistand = true
                        ),
                    ),
                    behandlingVersjon = behandling.versjon
                )
            )
        }
        ventPåSvar()

        // Saken står til en-trinnskontroll hos saksbehandler klar for å bli sendt til beslutter
        dataSource.transaction { dbConnection ->
            val avklaringsbehov = hentAvklaringsbehov(behandling.id, dbConnection)
            assertThat(avklaringsbehov.alle()).anySatisfy { it.erÅpent() && it.definisjon == Definisjon.FORESLÅ_VEDTAK }
            assertThat(behandling.status()).isEqualTo(Status.UTREDES)
        }

        hendelsesMottak.håndtere(
            ident, DokumentMottattPersonHendelse(
                journalpost = JournalpostId("21"),
                mottattTidspunkt = fom.plusDays(18).atStartOfDay(),
                strukturertDokument = StrukturertDokument(
                    Pliktkort(
                        timerArbeidPerPeriode = setOf(
                            ArbeidIPeriode(
                                Periode(LocalDate.now(), LocalDate.now().plusDays(13)), TimerArbeid(
                                    BigDecimal(20)
                                )
                            ),
                            ArbeidIPeriode(
                                Periode(LocalDate.now().plusDays(14), LocalDate.now().plusDays(27)), TimerArbeid(
                                    BigDecimal(60)
                                )
                            )
                        )
                    ), Brevkode.PLIKTKORT
                )
            )
        )
        ventPåSvar()
        // Saken er tilbake til en-trinnskontroll hos saksbehandler klar for å bli sendt til beslutter
        dataSource.transaction { dbConnection ->
            val avklaringsbehov = hentAvklaringsbehov(behandling.id, dbConnection)
            assertThat(avklaringsbehov.alle()).anySatisfy { it.erÅpent() && it.definisjon == Definisjon.FORESLÅ_VEDTAK }
            assertThat(behandling.status()).isEqualTo(Status.UTREDES)
        }

        dataSource.transaction {
            AvklaringsbehovHendelseHåndterer(it).håndtere(
                behandling.id,
                LøsAvklaringsbehovBehandlingHendelse(
                    løsning = ForeslåVedtakLøsning("Begrunnelse"),
                    behandlingVersjon = behandling.versjon
                )
            )
        }
        ventPåSvar()

        // Saken står til To-trinnskontroll hos beslutter
        dataSource.transaction { connection ->
            val avklaringsbehov = hentAvklaringsbehov(behandling.id, connection)
            assertThat(avklaringsbehov.alle()).anySatisfy { it.erÅpent() && it.definisjon == Definisjon.FATTE_VEDTAK }
            assertThat(behandling.status()).isEqualTo(Status.UTREDES)
        }
        behandling = hentBehandling(sak.id)

        dataSource.transaction { connection ->
            val avklaringsbehov = hentAvklaringsbehov(behandling.id, connection)
            AvklaringsbehovHendelseHåndterer(connection).håndtere(
                behandling.id,
                LøsAvklaringsbehovBehandlingHendelse(
                    løsning = FatteVedtakLøsning(avklaringsbehov.alle()
                        .filter { behov -> behov.erTotrinn() }
                        .map { behov -> TotrinnsVurdering(behov.definisjon.kode, true, "begrunnelse") }),
                    behandlingVersjon = behandling.versjon
                )
            )
        }
        ventPåSvar()

        behandling = hentBehandling(sak.id)
        assertThat(behandling.status()).isEqualTo(Status.AVSLUTTET)

        //Henter vurder alder-vilkår
        //Assert utfall
        val vilkårsresultat = hentVilkårsresultat(behandlingId = behandling.id)
        val aldersvilkår = vilkårsresultat.finnVilkår(Vilkårtype.ALDERSVILKÅRET)

        assertThat(aldersvilkår.vilkårsperioder())
            .hasSize(1)
            .allMatch { vilkårsperiode -> vilkårsperiode.erOppfylt() }

        val sykdomsvilkåret = vilkårsresultat.finnVilkår(Vilkårtype.SYKDOMSVILKÅRET)

        assertThat(sykdomsvilkåret.vilkårsperioder())
            .hasSize(1)
            .allMatch { vilkårsperiode -> vilkårsperiode.erOppfylt() }

        val underveisGrunnlag = dataSource.transaction { connection ->
            UnderveisRepository(connection).hent(behandling.id)
        }

        assertThat(underveisGrunnlag.perioder).isNotEmpty
        assertThat(underveisGrunnlag.perioder.any { (it.gradering?.gradering?.prosentverdi() ?: 0) > 0 }).isTrue()
    }

    @Test
    fun `skal avklare yrkesskade hvis det finnes spor av yrkesskade - yrkesskade har årsakssammenheng`() {
        val ident = ident()
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))

        // Simulerer et svar fra YS-løsning om at det finnes en yrkesskade
        FakePersonopplysningGateway.konstruer(ident, Personopplysning(Fødselsdato(LocalDate.now().minusYears(18))))
        YrkesskadeRegisterMock.konstruer(ident = ident, periode = periode)

        // Sender inn en søknad
        hendelsesMottak.håndtere(
            ident, DokumentMottattPersonHendelse(
                journalpost = JournalpostId("10"),
                mottattTidspunkt = LocalDateTime.now(),
                strukturertDokument = StrukturertDokument(Søknad(periode), Brevkode.SØKNAD)
            )
        )
        ventPåSvar()

        val sak = hentSak(ident, periode)
        var behandling = hentBehandling(sak.id)
        assertThat(behandling.typeBehandling()).isEqualTo(TypeBehandling.Førstegangsbehandling)

        dataSource.transaction {
            val avklaringsbehov = hentAvklaringsbehov(behandling.id, it)
            assertThat(avklaringsbehov.alle()).isNotEmpty()
            assertThat(behandling.status()).isEqualTo(Status.UTREDES)
        }

        dataSource.transaction {
            AvklaringsbehovHendelseHåndterer(it).håndtere(
                behandling.id,
                LøsAvklaringsbehovBehandlingHendelse(
                    løsning = AvklarStudentLøsning(
                        studentvurdering = StudentVurdering(
                            begrunnelse = "Er student",
                            dokumenterBruktIVurdering = listOf(JournalpostId("123123")),
                            oppfyller11_14 = false,
                            avbruttStudieDato = null
                        )
                    ),
                    behandlingVersjon = behandling.versjon
                )
            )
        }
        ventPåSvar()

        dataSource.transaction {
            AvklaringsbehovHendelseHåndterer(it).håndtere(
                behandling.id,
                LøsAvklaringsbehovBehandlingHendelse(
                    løsning = AvklarYrkesskadeLøsning(
                        yrkesskadevurdering = YrkesskadevurderingDto(
                            begrunnelse = "Er syk nok",
                            dokumenterBruktIVurdering = listOf(JournalpostId("123123")),
                            erÅrsakssammenheng = true,
                            skadetidspunkt = LocalDate.now().minusYears(1),
                            andelAvNedsettelse = Prosent.`30_PROSENT`.prosentverdi(),
                            antattÅrligInntekt = Beløp(1_000_000)
                        )
                    ),
                    behandlingVersjon = behandling.versjon
                )
            )
        }
        ventPåSvar()

        dataSource.transaction {
            AvklaringsbehovHendelseHåndterer(it).håndtere(
                behandling.id,
                LøsAvklaringsbehovBehandlingHendelse(
                    løsning = AvklarSykdomLøsning(
                        sykdomsvurdering = Sykdomsvurdering(
                            begrunnelse = "Er syk nok",
                            dokumenterBruktIVurdering = listOf(JournalpostId("123123")),
                            erSkadeSykdomEllerLyteVesentligdel = true,
                            erNedsettelseIArbeidsevneHøyereEnnNedreGrense = true,
                            nedreGrense = NedreGrense.TRETTI,
                            nedsattArbeidsevneDato = LocalDate.now(),
                            ytterligereNedsattArbeidsevneDato = null
                        )
                    ),
                    behandlingVersjon = behandling.versjon
                )
            )
        }
        ventPåSvar()

        dataSource.transaction {
            AvklaringsbehovHendelseHåndterer(it).håndtere(
                behandling.id,
                LøsAvklaringsbehovBehandlingHendelse(
                    løsning = AvklarBistandsbehovLøsning(
                        bistandVurdering = BistandVurdering(
                            begrunnelse = "Trenger hjelp fra nav",
                            erBehovForBistand = true
                        ),
                    ),
                    behandlingVersjon = behandling.versjon
                )
            )
        }
        ventPåSvar()

        // Saken står til en-trinnskontroll hos saksbehandler klar for å bli sendt til beslutter
        dataSource.transaction {
            val avklaringsbehovene = hentAvklaringsbehov(behandling.id, it)
            assertThat(avklaringsbehovene.alle()).anySatisfy { avklaringsbehov -> avklaringsbehov.erÅpent() && avklaringsbehov.definisjon == Definisjon.FORESLÅ_VEDTAK }
            assertThat(behandling.status()).isEqualTo(Status.UTREDES)
        }

        dataSource.transaction {
            AvklaringsbehovHendelseHåndterer(it).håndtere(
                behandling.id,
                LøsAvklaringsbehovBehandlingHendelse(
                    løsning = ForeslåVedtakLøsning("Begrunnelse"),
                    behandlingVersjon = behandling.versjon
                )
            )
        }
        ventPåSvar()

        // Saken står til To-trinnskontroll hos beslutter
        dataSource.transaction { connection ->
            val avklaringsbehov = hentAvklaringsbehov(behandling.id, connection)
            assertThat(avklaringsbehov.alle()).anySatisfy { it.erÅpent() && it.definisjon == Definisjon.FATTE_VEDTAK }
            assertThat(behandling.status()).isEqualTo(Status.UTREDES)
        }
        behandling = hentBehandling(sak.id)

        dataSource.transaction { connection ->
            val avklaringsbehov = hentAvklaringsbehov(behandling.id, connection)
            AvklaringsbehovHendelseHåndterer(connection).håndtere(
                behandling.id,
                LøsAvklaringsbehovBehandlingHendelse(
                    løsning = FatteVedtakLøsning(avklaringsbehov.alle()
                        .filter { behov -> behov.erTotrinn() }
                        .map { behov -> TotrinnsVurdering(behov.definisjon.kode, true, "begrunnelse") }),
                    behandlingVersjon = behandling.versjon
                )
            )
        }
        ventPåSvar()

        behandling = hentBehandling(sak.id)
        assertThat(behandling.status()).isEqualTo(Status.AVSLUTTET)

        //Henter vurder alder-vilkår
        //Assert utfall
        val vilkårsresultat = hentVilkårsresultat(behandlingId = behandling.id)
        val aldersvilkår = vilkårsresultat.finnVilkår(Vilkårtype.ALDERSVILKÅRET)

        assertThat(aldersvilkår.vilkårsperioder())
            .hasSize(1)
            .allMatch { vilkårsperiode -> vilkårsperiode.erOppfylt() }

        val sykdomsvilkåret = vilkårsresultat.finnVilkår(Vilkårtype.SYKDOMSVILKÅRET)

        assertThat(sykdomsvilkåret.vilkårsperioder())
            .hasSize(1)
            .allMatch { vilkårsperiode -> vilkårsperiode.erOppfylt() }
    }

    @Test
    fun `to-trinn og ingen endring i gruppe etter sendt tilbake fra beslutter`() {
        val ident = ident()
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))

        // Simulerer et svar fra YS-løsning om at det finnes en yrkesskade
        FakePersonopplysningGateway.konstruer(ident, Personopplysning(Fødselsdato(LocalDate.now().minusYears(18))))
        YrkesskadeRegisterMock.konstruer(ident = ident, periode = periode)
        InntektRegisterMock.konstruer(
            ident = ident, inntekterPerÅr = listOf(
                InntektPerÅr(
                    Year.now().minusYears(3),
                    Beløp(
                        BigDecimal(1000000)
                    )
                )
            )
        )

        // Sender inn en søknad
        hendelsesMottak.håndtere(
            ident, DokumentMottattPersonHendelse(
                journalpost = JournalpostId("11"),
                mottattTidspunkt = LocalDateTime.now(),
                strukturertDokument = StrukturertDokument(Søknad(periode), Brevkode.SØKNAD)
            )
        )
        ventPåSvar()

        val sak = hentSak(ident, periode)
        var behandling = hentBehandling(sak.id)
        assertThat(behandling.typeBehandling()).isEqualTo(TypeBehandling.Førstegangsbehandling)

        dataSource.transaction {
            val avklaringsbehov = hentAvklaringsbehov(behandling.id, it)
            assertThat(avklaringsbehov.alle()).isNotEmpty()
            assertThat(behandling.status()).isEqualTo(Status.UTREDES)
        }

        dataSource.transaction {
            AvklaringsbehovHendelseHåndterer(it).håndtere(
                behandling.id,
                LøsAvklaringsbehovBehandlingHendelse(
                    løsning = AvklarStudentLøsning(
                        studentvurdering = StudentVurdering(
                            begrunnelse = "Er student",
                            dokumenterBruktIVurdering = listOf(JournalpostId("123123")),
                            oppfyller11_14 = false,
                            avbruttStudieDato = null
                        )
                    ),
                    behandlingVersjon = behandling.versjon
                )
            )
        }
        ventPåSvar()

        dataSource.transaction {
            AvklaringsbehovHendelseHåndterer(it).håndtere(
                behandling.id,
                LøsAvklaringsbehovBehandlingHendelse(
                    løsning = AvklarYrkesskadeLøsning(
                        yrkesskadevurdering = YrkesskadevurderingDto(
                            begrunnelse = "Er ikke årsakssammenheng mellom yrkesskaden og nedsettelsen i arbeidsevne",
                            dokumenterBruktIVurdering = listOf(JournalpostId("123123")),
                            erÅrsakssammenheng = false,
                            skadetidspunkt = null,
                            andelAvNedsettelse = null,
                            antattÅrligInntekt = null
                        )
                    ),
                    behandlingVersjon = behandling.versjon
                )
            )
        }
        ventPåSvar()

        dataSource.transaction {
            AvklaringsbehovHendelseHåndterer(it).håndtere(
                behandling.id,
                LøsAvklaringsbehovBehandlingHendelse(
                    løsning = AvklarSykdomLøsning(
                        sykdomsvurdering = Sykdomsvurdering(
                            begrunnelse = "Arbeidsevnen er nedsatt med mer enn halvparten",
                            dokumenterBruktIVurdering = listOf(JournalpostId("123123")),
                            erSkadeSykdomEllerLyteVesentligdel = true,
                            erNedsettelseIArbeidsevneHøyereEnnNedreGrense = true,
                            nedreGrense = NedreGrense.FEMTI,
                            nedsattArbeidsevneDato = LocalDate.now(),
                            ytterligereNedsattArbeidsevneDato = null
                        )
                    ),
                    behandlingVersjon = behandling.versjon
                )
            )
        }
        ventPåSvar()

        dataSource.transaction {
            AvklaringsbehovHendelseHåndterer(it).håndtere(
                behandling.id,
                LøsAvklaringsbehovBehandlingHendelse(
                    løsning = AvklarBistandsbehovLøsning(
                        bistandVurdering = BistandVurdering(
                            begrunnelse = "Trenger hjelp fra nav",
                            erBehovForBistand = true
                        ),
                    ),
                    behandlingVersjon = behandling.versjon
                )
            )
        }
        ventPåSvar()

        // Saken står til en-trinnskontroll hos saksbehandler klar for å bli sendt til beslutter
        dataSource.transaction { connection ->
            val avklaringsbehov = hentAvklaringsbehov(behandling.id, connection)
            assertThat(avklaringsbehov.alle()).anySatisfy { behov -> behov.erÅpent() && behov.definisjon == Definisjon.FORESLÅ_VEDTAK }
            assertThat(behandling.status()).isEqualTo(Status.UTREDES)
        }

        dataSource.transaction {
            AvklaringsbehovHendelseHåndterer(it).håndtere(
                behandling.id,
                LøsAvklaringsbehovBehandlingHendelse(
                    løsning = ForeslåVedtakLøsning("Begrunnelse"),
                    behandlingVersjon = behandling.versjon
                )
            )
        }
        ventPåSvar()

        // Saken står til To-trinnskontroll hos beslutter
        dataSource.transaction { connection ->
            val avklaringsbehov = hentAvklaringsbehov(behandling.id, connection)
            assertThat(avklaringsbehov.alle()).anySatisfy { it.erÅpent() && it.definisjon == Definisjon.FATTE_VEDTAK }
            assertThat(behandling.status()).isEqualTo(Status.UTREDES)
        }
        behandling = hentBehandling(sak.id)

        dataSource.transaction { connection ->
            val avklaringsbehov = hentAvklaringsbehov(behandling.id, connection)
            AvklaringsbehovHendelseHåndterer(connection).håndtere(
                behandling.id,
                LøsAvklaringsbehovBehandlingHendelse(
                    løsning = FatteVedtakLøsning(avklaringsbehov.alle()
                        .filter { behov -> behov.erTotrinn() }
                        .map { behov ->
                            TotrinnsVurdering(
                                behov.definisjon.kode,
                                behov.definisjon != Definisjon.AVKLAR_SYKDOM,
                                "begrunnelse"
                            )
                        }),
                    behandlingVersjon = behandling.versjon
                )
            )
        }
        ventPåSvar()

        behandling = hentBehandling(sak.id)
        assertThat(behandling.status()).isEqualTo(Status.UTREDES)
        dataSource.transaction { connection ->
            val avklaringsbehov = hentAvklaringsbehov(behandling.id, connection)
            assertThat(avklaringsbehov.alle()).anySatisfy { it.erÅpent() && it.definisjon == Definisjon.AVKLAR_SYKDOM }
        }

        dataSource.transaction {
            AvklaringsbehovHendelseHåndterer(it).håndtere(
                behandling.id,
                LøsAvklaringsbehovBehandlingHendelse(
                    løsning = AvklarSykdomLøsning(
                        sykdomsvurdering = Sykdomsvurdering(
                            begrunnelse = "Er syk nok",
                            dokumenterBruktIVurdering = listOf(JournalpostId("123123")),
                            erSkadeSykdomEllerLyteVesentligdel = true,
                            erNedsettelseIArbeidsevneHøyereEnnNedreGrense = true,
                            nedreGrense = NedreGrense.FEMTI,
                            nedsattArbeidsevneDato = LocalDate.now(),
                            ytterligereNedsattArbeidsevneDato = null
                        )
                    ),
                    ingenEndringIGruppe = true,
                    behandlingVersjon = behandling.versjon
                )
            )
        }
        ventPåSvar()

        // Saken står til en-trinnskontroll hos saksbehandler klar for å bli sendt til beslutter
        dataSource.transaction { connection ->
            val avklaringsbehov = hentAvklaringsbehov(behandling.id, connection)
            assertThat(avklaringsbehov.alle()).anySatisfy { behov -> behov.erÅpent() && behov.definisjon == Definisjon.FORESLÅ_VEDTAK }
            assertThat(behandling.status()).isEqualTo(Status.UTREDES)
        }

        dataSource.transaction {
            AvklaringsbehovHendelseHåndterer(it).håndtere(
                behandling.id,
                LøsAvklaringsbehovBehandlingHendelse(
                    løsning = ForeslåVedtakLøsning("Begrunnelse"),
                    behandlingVersjon = behandling.versjon
                )
            )
        }
        ventPåSvar()

        // Saken står til To-trinnskontroll hos beslutter
        dataSource.transaction { connection ->
            val avklaringsbehov = hentAvklaringsbehov(behandling.id, connection)
            assertThat(avklaringsbehov.alle()).anySatisfy { it.erÅpent() && it.definisjon == Definisjon.FATTE_VEDTAK }
            assertThat(behandling.status()).isEqualTo(Status.UTREDES)
        }
        behandling = hentBehandling(sak.id)

        dataSource.transaction { connection ->
            val avklaringsbehov = hentAvklaringsbehov(behandling.id, connection)
            AvklaringsbehovHendelseHåndterer(connection).håndtere(
                behandling.id,
                LøsAvklaringsbehovBehandlingHendelse(
                    løsning = FatteVedtakLøsning(avklaringsbehov.alle()
                        .filter { behov -> behov.erTotrinn() }
                        .map { behov -> TotrinnsVurdering(behov.definisjon.kode, true, "begrunnelse") }),
                    behandlingVersjon = behandling.versjon
                )
            )
        }
        ventPåSvar()

        //Henter vurder alder-vilkår
        //Assert utfall
        val vilkårsresultat = hentVilkårsresultat(behandlingId = behandling.id)
        val aldersvilkår = vilkårsresultat.finnVilkår(Vilkårtype.ALDERSVILKÅRET)

        assertThat(aldersvilkår.vilkårsperioder())
            .hasSize(1)
            .allMatch { vilkårsperiode -> vilkårsperiode.erOppfylt() }

        val sykdomsvilkåret = vilkårsresultat.finnVilkår(Vilkårtype.SYKDOMSVILKÅRET)

        assertThat(sykdomsvilkåret.vilkårsperioder())
            .hasSize(1)
            .allMatch { vilkårsperiode -> vilkårsperiode.erOppfylt() }
    }

    private fun hentSak(ident: Ident, periode: Periode): Sak {
        return dataSource.transaction { connection ->
            SakRepositoryImpl(connection).finnEllerOpprett(
                PersonRepository(connection).finnEllerOpprett(listOf(ident)),
                periode
            )
        }
    }

    private fun hentVilkårsresultat(behandlingId: BehandlingId): Vilkårsresultat {
        return dataSource.transaction { connection ->
            VilkårsresultatRepository(connection).hent(behandlingId)
        }
    }

    private fun hentBehandling(sakId: SakId): Behandling {
        return dataSource.transaction { connection ->
            val finnSisteBehandlingFor = BehandlingRepositoryImpl(connection).finnSisteBehandlingFor(sakId)
            requireNotNull(finnSisteBehandlingFor)
        }
    }

    private fun hentAvklaringsbehov(behandlingId: BehandlingId, connection: DBConnection): Avklaringsbehovene {
        return AvklaringsbehovRepositoryImpl(connection).hentAvklaringsbehovene(behandlingId)
    }

    private fun ventPåSvar() {
        dataSource.transaction {
            val maxTid = LocalDateTime.now().plusMinutes(1)
            while ((OppgaveRepository(it).harOppgaver() || motor.harOppgaverKjørende()) && maxTid.isAfter(LocalDateTime.now())) {
                Thread.sleep(50L)
            }
        }
    }

    @Test
    fun `Ikke oppfylt på grunn av alder på søknadstidspunkt`() {
        val ident = ident()
        hentPerson(ident)
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))

        FakePersonopplysningGateway.konstruer(ident, Personopplysning(Fødselsdato(LocalDate.now().minusYears(17))))

        hendelsesMottak.håndtere(
            ident,
            DokumentMottattPersonHendelse(
                journalpost = JournalpostId("1"),
                mottattTidspunkt = LocalDateTime.now(),
                strukturertDokument = StrukturertDokument(Søknad(periode), Brevkode.SØKNAD)
            )
        )
        ventPåSvar()

        val sak = hentSak(ident, periode)
        val behandling = requireNotNull(hentBehandling(sak.id))
        assertThat(behandling.typeBehandling()).isEqualTo(TypeBehandling.Førstegangsbehandling)

        val stegHistorikk = behandling.stegHistorikk()
        assertThat(stegHistorikk.map { it.steg() }).contains(StegType.AVKLAR_STUDENT)
        assertThat(stegHistorikk.map { it.status() }).contains(StegStatus.AVKLARINGSPUNKT)

        //Henter vurder alder-vilkår
        //Assert utfall
        val vilkårsresultat = hentVilkårsresultat(behandlingId = behandling.id)
        val aldersvilkår = vilkårsresultat.finnVilkår(Vilkårtype.ALDERSVILKÅRET)

        assertThat(aldersvilkår.vilkårsperioder())
            .hasSize(1)
            .noneMatch { vilkårsperiodeForAlder -> vilkårsperiodeForAlder.erOppfylt() }
    }

    private fun hentPerson(ident: Ident): Person {
        var person: Person? = null
        dataSource.transaction {
            person = PersonRepository(it).finnEllerOpprett(listOf(ident))
        }
        return person!!
    }

    @Test
    fun `Blir satt på vent for etterspørring av informasjon`() {
        val ident = ident()
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))

        FakePersonopplysningGateway.konstruer(ident, Personopplysning(Fødselsdato(LocalDate.now().minusYears(20))))

        hendelsesMottak.håndtere(
            ident, DokumentMottattPersonHendelse(
                journalpost = JournalpostId("2"),
                mottattTidspunkt = LocalDateTime.now(),
                strukturertDokument = StrukturertDokument(Søknad(periode), Brevkode.SØKNAD)
            )
        )
        ventPåSvar()

        val sak = hentSak(ident, periode)
        var behandling = requireNotNull(hentBehandling(sak.id))

        assertThat(behandling.status()).isEqualTo(Status.UTREDES)

        dataSource.transaction { connection ->
            val avklaringsbehov = hentAvklaringsbehov(behandling.id, connection)
            assertThat(avklaringsbehov.alle()).anySatisfy { it.erÅpent() && it.definisjon == Definisjon.AVKLAR_SYKDOM }
        }

        hendelsesMottak.håndtere(
            behandling.id,
            BehandlingSattPåVent()
        )

        behandling = hentBehandling(sak.id)
        dataSource.transaction { connection ->
            val avklaringsbehov = hentAvklaringsbehov(behandling.id, connection)
            assertThat(avklaringsbehov.alle())
                .hasSize(2)
                .anySatisfy { it.erÅpent() && it.definisjon == Definisjon.MANUELT_SATT_PÅ_VENT }
                .anySatisfy { it.erÅpent() && it.definisjon == Definisjon.AVKLAR_SYKDOM }
        }

        hendelsesMottak.håndtere(
            ident, DokumentMottattPersonHendelse(
                journalpost = JournalpostId("3"),
                mottattTidspunkt = LocalDateTime.now(),
                strukturertDokument = StrukturertDokument(Søknad(periode), Brevkode.SØKNAD)
            )
        )
        ventPåSvar()

        behandling = hentBehandling(sak.id)
        assertThat(behandling.status()).isEqualTo(Status.UTREDES)

        dataSource.transaction { connection ->
            val avklaringsbehov = hentAvklaringsbehov(behandling.id, connection)
            assertThat(avklaringsbehov.alle())
                .hasSize(2)
                .anySatisfy { !it.erÅpent() && it.definisjon == Definisjon.MANUELT_SATT_PÅ_VENT }
                .anySatisfy { it.erÅpent() && it.definisjon == Definisjon.AVKLAR_SYKDOM }
        }

    }
}
