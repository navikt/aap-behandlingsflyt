package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.avklaringsbehov.AvklaringsbehovHendelseHåndterer
import no.nav.aap.behandlingsflyt.avklaringsbehov.AvklaringsbehovRepositoryImpl
import no.nav.aap.behandlingsflyt.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.avklaringsbehov.LøsAvklaringsbehovBehandlingHendelse
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.bistand.AvklarBistandsbehovLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.bistand.BistandVurdering
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.student.AvklarStudentLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.student.StudentVurdering
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.sykdom.AvklarSykdomLøsning
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.sykdom.AvklarYrkesskadeLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.sykdom.NedreGrense
import no.nav.aap.behandlingsflyt.faktagrunnlag.sykdom.Sykdomsvurdering
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.sykdom.YrkesskadevurderingDto
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.vedtak.FatteVedtakLøsning
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.vedtak.ForeslåVedtakLøsning
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.vedtak.TotrinnsVurdering
import no.nav.aap.behandlingsflyt.behandling.Behandling
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import no.nav.aap.verdityper.flyt.Status
import no.nav.aap.behandlingsflyt.behandling.behandlingRepository
import no.nav.aap.behandlingsflyt.behandling.dokumenter.Brevkode
import no.nav.aap.verdityper.dokument.JournalpostId
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.dbconnect.transaction
import no.nav.aap.behandlingsflyt.dbtest.InitTestDatabase
import no.nav.aap.behandlingsflyt.faktagrunnlag.arbeid.ArbeidIPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.inntekt.InntektPerÅr
import no.nav.aap.behandlingsflyt.faktagrunnlag.inntekt.adapter.InntektRegisterMock
import no.nav.aap.behandlingsflyt.faktagrunnlag.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.personopplysninger.Personopplysning
import no.nav.aap.behandlingsflyt.faktagrunnlag.personopplysninger.adapter.PersonRegisterMock
import no.nav.aap.behandlingsflyt.faktagrunnlag.yrkesskade.adapter.YrkesskadeRegisterMock
import no.nav.aap.behandlingsflyt.flyt.behandlingstyper.Førstegangsbehandling
import no.nav.aap.behandlingsflyt.flyt.steg.StegStatus
import no.nav.aap.verdityper.flyt.StegType
import no.nav.aap.behandlingsflyt.flyt.vilkår.Vilkårsresultat
import no.nav.aap.behandlingsflyt.flyt.vilkår.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.flyt.vilkår.Vilkårtype
import no.nav.aap.behandlingsflyt.hendelse.mottak.BehandlingSattPåVent
import no.nav.aap.behandlingsflyt.hendelse.mottak.DokumentMottattPersonHendelse
import no.nav.aap.behandlingsflyt.hendelse.mottak.HendelsesMottak
import no.nav.aap.behandlingsflyt.hendelse.mottak.dokument.StrukturertDokument
import no.nav.aap.behandlingsflyt.hendelse.mottak.dokument.pliktkort.Pliktkort
import no.nav.aap.behandlingsflyt.hendelse.mottak.dokument.søknad.Søknad
import no.nav.aap.behandlingsflyt.prosessering.Motor
import no.nav.aap.behandlingsflyt.prosessering.OppgaveRepository
import no.nav.aap.verdityper.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sak.Person
import no.nav.aap.behandlingsflyt.sak.PersonRepository
import no.nav.aap.behandlingsflyt.sak.Sak
import no.nav.aap.verdityper.sakogbehandling.SakId
import no.nav.aap.behandlingsflyt.sak.sakRepository
import no.nav.aap.verdityper.TimerArbeid
import no.nav.aap.verdityper.Beløp
import no.nav.aap.verdityper.Periode
import no.nav.aap.verdityper.Prosent
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
        val motor = Motor(dataSource, 1)
        val hendelsesMottak = HendelsesMottak(dataSource)

        @BeforeAll
        @JvmStatic
        internal fun beforeAll() {
            motor.start()
        }

        @AfterAll
        @JvmStatic
        internal fun afterAll() {
            motor.stop()
        }
    }

    @Test
    fun `skal avklare yrkesskade hvis det finnes spor av yrkesskade`() {
        val ident = Ident("123123123123")
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))

        // Simulerer et svar fra YS-løsning om at det finnes en yrkesskade
        PersonRegisterMock.konstruer(ident, Personopplysning(Fødselsdato(LocalDate.now().minusYears(18))))
        YrkesskadeRegisterMock.konstruer(ident = ident, periode = periode)

        // Sender inn en søknad
        hendelsesMottak.håndtere(
            ident, DokumentMottattPersonHendelse(
                journalpost = JournalpostId("20"),
                mottattTidspunkt = LocalDateTime.now(),
                strukturertDokument = StrukturertDokument(Søknad(periode), Brevkode.SØKNAD)
            )
        )
        ventPåSvar()

        val sak = hentSak(ident, periode)
        var behandling = hentBehandling(sak.id)
        assertThat(behandling.type).isEqualTo(Førstegangsbehandling)

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
                    )
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
                    )
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
                    )
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
                    )
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
                mottattTidspunkt = LocalDateTime.now(),
                strukturertDokument = StrukturertDokument(
                    Pliktkort(
                        timerArbeidPerPeriode = setOf(
                            ArbeidIPeriode(
                                Periode(LocalDate.now(), LocalDate.now().plusDays(14)), TimerArbeid(
                                    BigDecimal(20)
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
                    løsning = ForeslåVedtakLøsning("Begrunnelse")
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
                        .map { behov -> TotrinnsVurdering(behov.definisjon.kode, true, "begrunnelse") })
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
    fun `skal avklare yrkesskade hvis det finnes spor av yrkesskade - yrkesskade har årsakssammenheng`() {
        val ident = Ident("123123123321")
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))

        // Simulerer et svar fra YS-løsning om at det finnes en yrkesskade
        PersonRegisterMock.konstruer(ident, Personopplysning(Fødselsdato(LocalDate.now().minusYears(18))))
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
        assertThat(behandling.type).isEqualTo(Førstegangsbehandling)

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
                    )
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
                    )
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
                    )
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
                    )
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
                    løsning = ForeslåVedtakLøsning("Begrunnelse")
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
                        .map { behov -> TotrinnsVurdering(behov.definisjon.kode, true, "begrunnelse") })
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
        val ident = Ident("123123123199")
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))

        // Simulerer et svar fra YS-løsning om at det finnes en yrkesskade
        PersonRegisterMock.konstruer(ident, Personopplysning(Fødselsdato(LocalDate.now().minusYears(18))))
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
        assertThat(behandling.type).isEqualTo(Førstegangsbehandling)

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
                    )
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
                    )
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
                    )
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
                    )
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
                    løsning = ForeslåVedtakLøsning("Begrunnelse")
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
                        })
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
                    ingenEndringIGruppe = true
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
                    løsning = ForeslåVedtakLøsning("Begrunnelse")
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
                        .map { behov -> TotrinnsVurdering(behov.definisjon.kode, true, "begrunnelse") })
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
            sakRepository(connection).finnEllerOpprett(PersonRepository(connection).finnEllerOpprett(ident), periode)
        }
    }

    private fun hentVilkårsresultat(behandlingId: BehandlingId): Vilkårsresultat {
        return dataSource.transaction { connection ->
            VilkårsresultatRepository(connection).hent(behandlingId)
        }
    }

    private fun hentBehandling(sakId: SakId): Behandling {
        return dataSource.transaction { connection ->
            val finnSisteBehandlingFor = behandlingRepository(connection).finnSisteBehandlingFor(sakId)
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
        val ident = Ident("123123123125")
        hentPerson(ident)
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))

        PersonRegisterMock.konstruer(ident, Personopplysning(Fødselsdato(LocalDate.now().minusYears(17))))

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
        assertThat(behandling.type).isEqualTo(Førstegangsbehandling)

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
            person = PersonRepository(it).finnEllerOpprett(ident)
        }
        return person!!
    }

    @Test
    fun `Blir satt på vent for etterspørring av informasjon`() {
        val ident = Ident("123123123127")
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))

        PersonRegisterMock.konstruer(ident, Personopplysning(Fødselsdato(LocalDate.now().minusYears(20))))

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
