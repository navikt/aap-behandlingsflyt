package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.auth.Bruker
import no.nav.aap.behandlingsflyt.SYSTEMBRUKER
import no.nav.aap.behandlingsflyt.avklaringsbehov.AvklaringsbehovHendelseHåndterer
import no.nav.aap.behandlingsflyt.avklaringsbehov.AvklaringsbehovRepositoryImpl
import no.nav.aap.behandlingsflyt.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.avklaringsbehov.LøsAvklaringsbehovBehandlingHendelse
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.vedtak.TotrinnsVurdering
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.vedtak.ÅrsakTilReturKode
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.ÅrsakTilSettPåVent
import no.nav.aap.behandlingsflyt.avklaringsbehov.løsning.AvklarBistandsbehovLøsning
import no.nav.aap.behandlingsflyt.avklaringsbehov.løsning.AvklarStudentLøsning
import no.nav.aap.behandlingsflyt.avklaringsbehov.løsning.AvklarSykdomLøsning
import no.nav.aap.behandlingsflyt.avklaringsbehov.løsning.FastsettBeregningstidspunktLøsning
import no.nav.aap.behandlingsflyt.avklaringsbehov.løsning.FatteVedtakLøsning
import no.nav.aap.behandlingsflyt.avklaringsbehov.løsning.ForeslåVedtakLøsning
import no.nav.aap.behandlingsflyt.avklaringsbehov.løsning.KvalitetssikringLøsning
import no.nav.aap.behandlingsflyt.avklaringsbehov.ÅrsakTilRetur
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.dbconnect.transaction
import no.nav.aap.behandlingsflyt.dbtest.InitTestDatabase
import no.nav.aap.behandlingsflyt.dbtestdata.ident
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.StrukturertDokument
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.kontrakt.søknad.Søknad
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.kontrakt.søknad.SøknadStudentDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektPerÅr
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.BistandVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.NedreGrense
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.flate.SykdomsvurderingDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.flate.YrkesskadevurderingDto
import no.nav.aap.behandlingsflyt.flyt.flate.Venteinformasjon
import no.nav.aap.behandlingsflyt.flyt.internals.DokumentMottattPersonHendelse
import no.nav.aap.behandlingsflyt.flyt.internals.TestHendelsesMottak
import no.nav.aap.behandlingsflyt.flyt.internals.TestJobbRepository
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.TOGGLE_KVALITETSSIKRING
import no.nav.aap.behandlingsflyt.hendelse.mottak.BehandlingSattPåVent
import no.nav.aap.behandlingsflyt.prosessering.ProsesseringsJobber
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.dokumenter.Brevkode
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.db.PersonRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.db.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.test.Fakes
import no.nav.aap.behandlingsflyt.test.modell.TestPerson
import no.nav.aap.behandlingsflyt.test.modell.TestYrkesskade
import no.nav.aap.behandlingsflyt.toggles.FeatureToggle
import no.nav.aap.motor.Motor
import no.nav.aap.verdityper.Beløp
import no.nav.aap.verdityper.Periode
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
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Year
import kotlin.system.measureTimeMillis

private val log = LoggerFactory.getLogger(FlytOrkestratorTest::class.java)

class FlytOrkestratorTest {

    companion object {
        val dataSource = InitTestDatabase.dataSource
        val motor = Motor(dataSource, 2, jobber = ProsesseringsJobber.alle())
        val hendelsesMottak = TestHendelsesMottak(dataSource)
        val fakes = Fakes()

        @BeforeAll
        @JvmStatic
        internal fun beforeAll() {
            motor.start()
        }

        @AfterAll
        @JvmStatic
        internal fun afterAll() {
            motor.stop()
            fakes.close()
        }
    }

    init {
        FeatureToggle.aktiver(TOGGLE_KVALITETSSIKRING, true)
    }

    @Test
    fun `skal avklare yrkesskade hvis det finnes spor av yrkesskade`() {
        val ident = ident()
        val fom = LocalDate.now().minusMonths(3)
        val periode = Periode(fom, fom.plusYears(3))

        // Simulerer et svar fra YS-løsning om at det finnes en yrkesskade
        fakes.leggTil(
            TestPerson(
                identer = setOf(ident),
                fødselsdato = Fødselsdato(LocalDate.now().minusYears(20)),
                yrkesskade = listOf(TestYrkesskade()),
                inntekter = listOf(
                    InntektPerÅr(
                        Year.now().minusYears(1),
                        Beløp(1000000)
                    ),
                    InntektPerÅr(
                        Year.now().minusYears(2),
                        Beløp(1000000)
                    ),
                    InntektPerÅr(
                        Year.now().minusYears(3),
                        Beløp(1000000)
                    )
                )
            )
        )


        // Sender inn en søknad
        hendelsesMottak.håndtere(
            ident, DokumentMottattPersonHendelse(
                journalpost = JournalpostId("20"),
                mottattTidspunkt = LocalDateTime.now().minusMonths(3),
                strukturertDokument = StrukturertDokument(
                    Søknad(student = SøknadStudentDto("NEI"), yrkesskade = "NEI"),
                    Brevkode.SØKNAD
                ),
                periode = periode
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
                    løsning = AvklarSykdomLøsning(
                        sykdomsvurdering = SykdomsvurderingDto(
                            begrunnelse = "Er syk nok",
                            dokumenterBruktIVurdering = listOf(JournalpostId("123123")),
                            erSkadeSykdomEllerLyteVesentligdel = true,
                            erNedsettelseIArbeidsevneHøyereEnnNedreGrense = true,
                            nedreGrense = NedreGrense.FEMTI,
                            nedsattArbeidsevneDato = LocalDate.now().year,
                            erArbeidsevnenNedsatt = true,
                            yrkesskadevurdering = YrkesskadevurderingDto(
                                erÅrsakssammenheng = false
                            )
                        )
                    ),
                    behandlingVersjon = behandling.versjon,
                    bruker = Bruker("SAKSBEHANDLER")
                )
            )
        }
        ventPåSvar(sak.id, behandling.id)
        behandling = hentBehandling(sak.id)

        dataSource.transaction {
            AvklaringsbehovHendelseHåndterer(it).håndtere(
                behandling.id,
                LøsAvklaringsbehovBehandlingHendelse(
                    løsning = AvklarBistandsbehovLøsning(
                        bistandsVurdering = BistandVurdering(
                            begrunnelse = "Trenger hjelp fra nav",
                            erBehovForBistand = true
                        ),
                    ),
                    behandlingVersjon = behandling.versjon,
                    bruker = Bruker("SAKSBEHANDLER")
                )
            )
        }
        ventPåSvar(sak.id, behandling.id)
        behandling = hentBehandling(sak.id)

        dataSource.transaction {
            val avklaringsbehov = hentAvklaringsbehov(behandling.id, it)
            AvklaringsbehovHendelseHåndterer(it).håndtere(
                behandling.id,
                LøsAvklaringsbehovBehandlingHendelse(
                    løsning = KvalitetssikringLøsning(avklaringsbehov.alle()
                        .filter { behov -> behov.erTotrinn() }
                        .map { behov ->
                            TotrinnsVurdering(
                                behov.definisjon.kode,
                                true,
                                "begrunnelse",
                                emptyList()
                            )
                        }),
                    behandlingVersjon = behandling.versjon,
                    bruker = Bruker("SAKSBEHANDLER")
                )
            )
        }
        ventPåSvar(sak.id, behandling.id)
        behandling = hentBehandling(sak.id)

        // Saken står til en-trinnskontroll hos saksbehandler klar for å bli sendt til beslutter
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
                    behandlingVersjon = behandling.versjon,
                    bruker = Bruker("SAKSBEHANDLER")
                )
            )
        }
        ventPåSvar(sak.id, behandling.id)
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
                                behov.definisjon.kode != Definisjon.AVKLAR_SYKDOM.kode,
                                "begrunnelse",
                                if (behov.definisjon.kode != Definisjon.AVKLAR_SYKDOM.kode) {
                                    emptyList()
                                } else {
                                    listOf(ÅrsakTilRetur(ÅrsakTilReturKode.FEIL_LOVANVENDELSE, null))
                                }
                            )
                        }),
                    behandlingVersjon = behandling.versjon,
                    bruker = Bruker("SAKSBEHANDLER")
                )
            )
        }
        ventPåSvar(sak.id, behandling.id)
        behandling = hentBehandling(sak.id)

        dataSource.transaction {
            AvklaringsbehovHendelseHåndterer(it).håndtere(
                behandling.id,
                LøsAvklaringsbehovBehandlingHendelse(
                    løsning = AvklarSykdomLøsning(
                        sykdomsvurdering = SykdomsvurderingDto(
                            begrunnelse = "Er syk nok",
                            dokumenterBruktIVurdering = listOf(JournalpostId("123123")),
                            erSkadeSykdomEllerLyteVesentligdel = true,
                            erNedsettelseIArbeidsevneHøyereEnnNedreGrense = true,
                            nedreGrense = NedreGrense.FEMTI,
                            nedsattArbeidsevneDato = LocalDate.now().year,
                            erArbeidsevnenNedsatt = true,
                            yrkesskadevurdering = YrkesskadevurderingDto(
                                erÅrsakssammenheng = false
                            )
                        )
                    ),
                    behandlingVersjon = behandling.versjon,
                    bruker = Bruker("SAKSBEHANDLER")
                )
            )
        }
        ventPåSvar(sak.id, behandling.id)
        behandling = hentBehandling(sak.id)

        dataSource.transaction {
            AvklaringsbehovHendelseHåndterer(it).håndtere(
                behandling.id,
                LøsAvklaringsbehovBehandlingHendelse(
                    løsning = AvklarBistandsbehovLøsning(
                        bistandsVurdering = BistandVurdering(
                            begrunnelse = "Trenger hjelp fra nav",
                            erBehovForBistand = true
                        ),
                    ),
                    behandlingVersjon = behandling.versjon,
                    bruker = Bruker("SAKSBEHANDLER")
                )
            )
        }
        ventPåSvar(sak.id, behandling.id)
        behandling = hentBehandling(sak.id)
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
                    behandlingVersjon = behandling.versjon,
                    bruker = Bruker("SAKSBEHANDLER")
                )
            )
        }
        ventPåSvar(sak.id, behandling.id)

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
                        .map { behov -> TotrinnsVurdering(behov.definisjon.kode, true, "begrunnelse", emptyList()) }),
                    behandlingVersjon = behandling.versjon,
                    bruker = Bruker("SAKSBEHANDLER")
                )
            )
        }
        ventPåSvar(sak.id, behandling.id)

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
        fakes.returnerYrkesskade(ident.identifikator)

        // Sender inn en søknad
        hendelsesMottak.håndtere(
            ident, DokumentMottattPersonHendelse(
                journalpost = JournalpostId("10"),
                mottattTidspunkt = LocalDateTime.now(),
                strukturertDokument = StrukturertDokument(Søknad(student = SøknadStudentDto("NEI"), yrkesskade = "NEI"), Brevkode.SØKNAD),
                periode = periode
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
                    løsning = AvklarSykdomLøsning(
                        sykdomsvurdering = SykdomsvurderingDto(
                            begrunnelse = "Er syk nok",
                            dokumenterBruktIVurdering = listOf(JournalpostId("123123")),
                            erSkadeSykdomEllerLyteVesentligdel = true,
                            erNedsettelseIArbeidsevneHøyereEnnNedreGrense = true,
                            nedreGrense = NedreGrense.TRETTI,
                            nedsattArbeidsevneDato = LocalDate.now().year,
                            erArbeidsevnenNedsatt = true,
                            yrkesskadevurdering = YrkesskadevurderingDto(
                                erÅrsakssammenheng = true
                            )
                        )
                    ),
                    behandlingVersjon = behandling.versjon,
                    bruker = Bruker("SAKSBEHANDLER")
                )
            )
        }
        ventPåSvar(sak.id, behandling.id)
        behandling = hentBehandling(sak.id)

        dataSource.transaction {
            AvklaringsbehovHendelseHåndterer(it).håndtere(
                behandling.id,
                LøsAvklaringsbehovBehandlingHendelse(
                    løsning = AvklarBistandsbehovLøsning(
                        bistandsVurdering = BistandVurdering(
                            begrunnelse = "Trenger hjelp fra nav",
                            erBehovForBistand = true
                        ),
                    ),
                    behandlingVersjon = behandling.versjon,
                    bruker = Bruker("SAKSBEHANDLER")
                )
            )
        }
        ventPåSvar(sak.id, behandling.id)
        behandling = hentBehandling(sak.id)

        dataSource.transaction {
            val avklaringsbehov = hentAvklaringsbehov(behandling.id, it)
            AvklaringsbehovHendelseHåndterer(it).håndtere(
                behandling.id,
                LøsAvklaringsbehovBehandlingHendelse(
                    løsning = KvalitetssikringLøsning(avklaringsbehov.alle()
                        .filter { behov -> behov.erTotrinn() }
                        .map { behov ->
                            TotrinnsVurdering(
                                behov.definisjon.kode,
                                true,
                                "begrunnelse",
                                emptyList()
                            )
                        }),
                    behandlingVersjon = behandling.versjon,
                    bruker = Bruker("SAKSBEHANDLER")
                )
            )
        }
        ventPåSvar(sak.id, behandling.id)
        behandling = hentBehandling(sak.id)

        dataSource.transaction {
            AvklaringsbehovHendelseHåndterer(it).håndtere(
                behandling.id,
                LøsAvklaringsbehovBehandlingHendelse(
                    løsning = FastsettBeregningstidspunktLøsning(
                        beregningVurdering = BeregningVurdering(
                            begrunnelse = "Trenger hjelp fra nav",
                            ytterligereNedsattArbeidsevneDato = null,
                            antattÅrligInntekt = null
                        ),
                    ),
                    behandlingVersjon = behandling.versjon,
                    bruker = Bruker("SAKSBEHANDLER")
                )
            )
        }
        ventPåSvar(sak.id, behandling.id)
        behandling = hentBehandling(sak.id)

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
                    behandlingVersjon = behandling.versjon,
                    bruker = Bruker("SAKSBEHANDLER")
                )
            )
        }
        ventPåSvar(sak.id, behandling.id)

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
                        .map { behov -> TotrinnsVurdering(behov.definisjon.kode, true, "begrunnelse", emptyList()) }),
                    behandlingVersjon = behandling.versjon,
                    bruker = Bruker("SAKSBEHANDLER")
                )
            )
        }
        ventPåSvar(sak.id, behandling.id)

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
        fakes.returnerYrkesskade(ident.identifikator)
        fakes.leggTil(
            TestPerson(
                identer = setOf(ident),
                fødselsdato = Fødselsdato(LocalDate.now().minusYears(20)),
                yrkesskade = listOf(TestYrkesskade()),
                inntekter = listOf(
                    InntektPerÅr(Year.now().minusYears(1), Beløp("1000000.0")),
                    InntektPerÅr(Year.now().minusYears(2), Beløp("1000000.0")),
                    InntektPerÅr(Year.now().minusYears(3), Beløp("1000000.0")),
                )
            )
        )

        // Sender inn en søknad
        hendelsesMottak.håndtere(
            ident, DokumentMottattPersonHendelse(
                journalpost = JournalpostId("11"),
                mottattTidspunkt = LocalDateTime.now(),
                strukturertDokument = StrukturertDokument(Søknad(student = SøknadStudentDto("JA"), yrkesskade = "JA"), Brevkode.SØKNAD),
                periode = periode
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
                    behandlingVersjon = behandling.versjon,
                    bruker = Bruker("SAKSBEHANDLER")
                )
            )
        }
        ventPåSvar(sak.id, behandling.id)
        behandling = hentBehandling(sak.id)

        dataSource.transaction {
            AvklaringsbehovHendelseHåndterer(it).håndtere(
                behandling.id,
                LøsAvklaringsbehovBehandlingHendelse(
                    løsning = AvklarSykdomLøsning(
                        sykdomsvurdering = SykdomsvurderingDto(
                            begrunnelse = "Arbeidsevnen er nedsatt med mer enn halvparten",
                            dokumenterBruktIVurdering = listOf(JournalpostId("123123")),
                            erSkadeSykdomEllerLyteVesentligdel = true,
                            erNedsettelseIArbeidsevneHøyereEnnNedreGrense = true,
                            nedreGrense = NedreGrense.FEMTI,
                            nedsattArbeidsevneDato = LocalDate.now().year,
                            erArbeidsevnenNedsatt = true,
                            yrkesskadevurdering = YrkesskadevurderingDto(
                                erÅrsakssammenheng = false
                            )
                        )
                    ),
                    behandlingVersjon = behandling.versjon,
                    bruker = Bruker("SAKSBEHANDLER")
                )
            )
        }
        ventPåSvar(sak.id, behandling.id)
        behandling = hentBehandling(sak.id)

        dataSource.transaction {
            AvklaringsbehovHendelseHåndterer(it).håndtere(
                behandling.id,
                LøsAvklaringsbehovBehandlingHendelse(
                    løsning = AvklarBistandsbehovLøsning(
                        bistandsVurdering = BistandVurdering(
                            begrunnelse = "Trenger hjelp fra nav",
                            erBehovForBistand = true
                        ),
                    ),
                    behandlingVersjon = behandling.versjon,
                    bruker = Bruker("SAKSBEHANDLER")
                )
            )
        }
        ventPåSvar(sak.id, behandling.id)
        behandling = hentBehandling(sak.id)

        // Saken står til en-trinnskontroll hos saksbehandler klar for å bli sendt til beslutter
        dataSource.transaction { connection ->
            val avklaringsbehov = hentAvklaringsbehov(behandling.id, connection)
            assertThat(avklaringsbehov.alle()).anySatisfy { behov -> behov.erÅpent() && behov.definisjon == Definisjon.FORESLÅ_VEDTAK }
            assertThat(behandling.status()).isEqualTo(Status.UTREDES)
        }

        dataSource.transaction {
            val avklaringsbehov = hentAvklaringsbehov(behandling.id, it)
            AvklaringsbehovHendelseHåndterer(it).håndtere(
                behandling.id,
                LøsAvklaringsbehovBehandlingHendelse(
                    løsning = KvalitetssikringLøsning(avklaringsbehov.alle()
                        .filter { behov -> behov.kreverKvalitetssikring() }
                        .map { behov ->
                            TotrinnsVurdering(
                                behov.definisjon.kode,
                                true,
                                "begrunnelse",
                                emptyList()
                            )
                        }),
                    behandlingVersjon = behandling.versjon,
                    bruker = Bruker("SAKSBEHANDLER")
                )
            )
        }
        ventPåSvar(sak.id, behandling.id)
        behandling = hentBehandling(sak.id)

        dataSource.transaction {
            AvklaringsbehovHendelseHåndterer(it).håndtere(
                behandling.id,
                LøsAvklaringsbehovBehandlingHendelse(
                    løsning = ForeslåVedtakLøsning("Begrunnelse"),
                    behandlingVersjon = behandling.versjon,
                    bruker = Bruker("SAKSBEHANDLER")
                )
            )
        }
        ventPåSvar(sak.id, behandling.id)

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
                                "begrunnelse",
                                emptyList()
                            )
                        }),
                    behandlingVersjon = behandling.versjon,
                    bruker = Bruker("SAKSBEHANDLER")
                )
            )
        }
        ventPåSvar(sak.id, behandling.id)

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
                        sykdomsvurdering = SykdomsvurderingDto(
                            begrunnelse = "Er syk nok",
                            dokumenterBruktIVurdering = listOf(JournalpostId("123123")),
                            erSkadeSykdomEllerLyteVesentligdel = true,
                            erNedsettelseIArbeidsevneHøyereEnnNedreGrense = true,
                            nedreGrense = NedreGrense.FEMTI,
                            nedsattArbeidsevneDato = LocalDate.now().year,
                            erArbeidsevnenNedsatt = true,
                            yrkesskadevurdering = YrkesskadevurderingDto(
                                erÅrsakssammenheng = false
                            )
                        )
                    ),
                    ingenEndringIGruppe = true,
                    behandlingVersjon = behandling.versjon,
                    bruker = Bruker("SAKSBEHANDLER")
                )
            )
        }
        ventPåSvar(sak.id, behandling.id)
        behandling = hentBehandling(sak.id)

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
                    behandlingVersjon = behandling.versjon,
                    bruker = Bruker("SAKSBEHANDLER")
                )
            )
        }
        ventPåSvar(sak.id, behandling.id)

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
                        .map { behov -> TotrinnsVurdering(behov.definisjon.kode, true, "begrunnelse", emptyList()) }),
                    behandlingVersjon = behandling.versjon,
                    bruker = Bruker("SAKSBEHANDLER")
                )
            )
        }
        ventPåSvar(sak.id, behandling.id)

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

    private fun ventPåSvar(sakId: SakId? = null, behandlingId: BehandlingId? = null) {
        val timeInMillis = measureTimeMillis {
            dataSource.transaction(readOnly = true) {
                val maxTid = LocalDateTime.now().plusMinutes(1)
                val testJobbRepository = TestJobbRepository(it)
                while ((testJobbRepository.harOppgaver(sakId, behandlingId)) && maxTid.isAfter(LocalDateTime.now())) {
                    Thread.sleep(50L)
                }
            }
        }
        log.info("Ventet på at prosessering skulle fullføre, det tok {}", Duration.ofMillis(timeInMillis))
    }

    @Test
    fun `Ikke oppfylt på grunn av alder på søknadstidspunkt`() {
        val ident = ident()
        hentPerson(ident)
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))

        fakes.leggTil(
            TestPerson(
                identer = setOf(ident),
                fødselsdato = Fødselsdato(LocalDate.now().minusYears(17))
            )
        )

        hendelsesMottak.håndtere(
            ident,
            DokumentMottattPersonHendelse(
                journalpost = JournalpostId("1"),
                mottattTidspunkt = LocalDateTime.now(),
                strukturertDokument = StrukturertDokument(Søknad(student = SøknadStudentDto("NEI"), yrkesskade = "NEI"), Brevkode.SØKNAD),
                periode = periode
            )
        )
        ventPåSvar()

        val sak = hentSak(ident, periode)
        val behandling = requireNotNull(hentBehandling(sak.id))
        assertThat(behandling.typeBehandling()).isEqualTo(TypeBehandling.Førstegangsbehandling)

        val stegHistorikk = behandling.stegHistorikk()
        assertThat(stegHistorikk.map { it.steg() }).contains(StegType.AVKLAR_SYKDOM)
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

        hendelsesMottak.håndtere(
            ident, DokumentMottattPersonHendelse(
                journalpost = JournalpostId("2"),
                mottattTidspunkt = LocalDateTime.now(),
                strukturertDokument = StrukturertDokument(Søknad(student = SøknadStudentDto("NEI"), yrkesskade = "NEI"), Brevkode.SØKNAD),
                periode = periode
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
            BehandlingSattPåVent(
                frist = null,
                begrunnelse = "Avventer dokumentasjon",
                bruker = SYSTEMBRUKER,
                behandlingVersjon = behandling.versjon,
                grunn = ÅrsakTilSettPåVent.ET_ELLER_ANNET
            )
        )
        val dto = dataSource.transaction(readOnly = true) { connection ->
            val avklaringsbehovene = hentAvklaringsbehov(behandling.id, connection)

            if (avklaringsbehovene.erSattPåVent()) {
                val avklaringsbehov = avklaringsbehovene.hentVentepunkter().first()
                Venteinformasjon(avklaringsbehov.frist(), avklaringsbehov.begrunnelse(), avklaringsbehov.grunn())
            } else {
                null
            }
        }
        assertThat(dto).isNotNull
        assertThat(dto?.frist).isNotNull
        behandling = hentBehandling(sak.id)
        dataSource.transaction { connection ->
            val avklaringsbehov = hentAvklaringsbehov(behandling.id, connection)
            assertThat(avklaringsbehov.alle())
                .hasSize(2)
                .anySatisfy { it.erÅpent() && it.definisjon == Definisjon.MANUELT_SATT_PÅ_VENT }
                .anySatisfy { it.erÅpent() && it.definisjon == Definisjon.AVKLAR_SYKDOM }
        }
        ventPåSvar(sak.id, behandling.id)
        hendelsesMottak.håndtere(
            ident, DokumentMottattPersonHendelse(
                journalpost = JournalpostId("3"),
                mottattTidspunkt = LocalDateTime.now(),
                strukturertDokument = StrukturertDokument(Søknad(student = SøknadStudentDto("NEI"), yrkesskade = "NEI"), Brevkode.SØKNAD),
                periode = periode
            )
        )
        ventPåSvar(sak.id, behandling.id)

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
