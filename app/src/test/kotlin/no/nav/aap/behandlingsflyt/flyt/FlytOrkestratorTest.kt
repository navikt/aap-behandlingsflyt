package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.SYSTEMBRUKER
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovHendelseHåndterer
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepositoryImpl
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.LøsAvklaringsbehovBehandlingHendelse
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.vedtak.TotrinnsVurdering
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.vedtak.ÅrsakTilReturKode
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.ÅrsakTilSettPåVent
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarBistandsbehovLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarStudentLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarSykdomLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.BrevbestillingLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.FastsettBeregningstidspunktLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.FatteVedtakLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.ForeslåVedtakLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.KvalitetssikringLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.SkrivBrevLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.ÅrsakTilRetur
import no.nav.aap.behandlingsflyt.behandling.brev.BREV_SYSTEMBRUKER
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingRepository
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.TypeBrev
import no.nav.aap.behandlingsflyt.dbtestdata.ident
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.BeregningsgrunnlagRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.GrunnlagYrkesskade
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
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.flate.BistandVurderingDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.NedreGrense
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.flate.SykdomsvurderingDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.flate.YrkesskadevurderingDto
import no.nav.aap.behandlingsflyt.faktasaksbehandler.student.StudentVurdering
import no.nav.aap.behandlingsflyt.flyt.flate.Venteinformasjon
import no.nav.aap.behandlingsflyt.flyt.internals.DokumentMottattPersonHendelse
import no.nav.aap.behandlingsflyt.flyt.internals.TestHendelsesMottak
import no.nav.aap.behandlingsflyt.hendelse.mottak.BehandlingSattPåVent
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AVKLAR_BISTANDSBEHOV_KODE
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AVKLAR_SYKDOM_KODE
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.FATTE_VEDTAK_KODE
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.KVALITETSSIKRING_KODE
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.brevbestilling.BrevbestillingLøsningStatus
import no.nav.aap.behandlingsflyt.kontrakt.brevbestilling.LøsBrevbestillingDto
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.dokumenter.Brevkode
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.db.PersonRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.db.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.server.prosessering.ProsesseringsJobber
import no.nav.aap.behandlingsflyt.test.FakePersoner
import no.nav.aap.behandlingsflyt.test.Fakes
import no.nav.aap.behandlingsflyt.test.modell.TestPerson
import no.nav.aap.behandlingsflyt.test.modell.TestYrkesskade
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.komponenter.httpklient.auth.Bruker
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.motor.Motor
import no.nav.aap.motor.testutil.TestUtil
import no.nav.aap.statistikk.api_kontrakt.BehandlingStatus
import no.nav.aap.statistikk.api_kontrakt.StoppetBehandling
import no.nav.aap.verdityper.Beløp
import no.nav.aap.verdityper.dokument.JournalpostId
import no.nav.aap.verdityper.flyt.StegStatus
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import no.nav.aap.verdityper.sakogbehandling.Ident
import no.nav.aap.verdityper.sakogbehandling.SakId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Year

@Fakes
class FlytOrkestratorTest {

    companion object {
        private val dataSource = InitTestDatabase.dataSource
        private val motor = Motor(dataSource, 2, jobber = ProsesseringsJobber.alle())
        private val hendelsesMottak = TestHendelsesMottak(dataSource)
        private val util = TestUtil(dataSource, ProsesseringsJobber.alle().filter { it.cron() != null }.map { it.type() })

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
        val ident = ident()
        val fom = LocalDate.now().minusMonths(3)
        val periode = Periode(fom, fom.plusYears(3))

        // Simulerer et svar fra YS-løsning om at det finnes en yrkesskade
        FakePersoner.leggTil(
            TestPerson(
                identer = setOf(ident),
                fødselsdato = Fødselsdato(LocalDate.now().minusYears(25)),
                yrkesskade = listOf(TestYrkesskade()),
                barn = listOf(
                    TestPerson(
                        identer = setOf(Ident("1234123")),
                        fødselsdato = Fødselsdato(LocalDate.now().minusYears(3)),
                        yrkesskade = listOf(),
                        barn = listOf(),
                        inntekter = listOf()
                    )
                ),
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
        sendInnDokument(
            ident, DokumentMottattPersonHendelse(
                journalpost = JournalpostId("20"),
                mottattTidspunkt = LocalDateTime.now().minusMonths(3),
                strukturertDokument = StrukturertDokument(
                    Søknad(student = SøknadStudentDto("NEI"), yrkesskade = "NEI", oppgitteBarn = null),
                    Brevkode.SØKNAD
                ),
                periode = periode
            )
        )
        util.ventPåSvar()

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
                            harSkadeSykdomEllerLyte = true,
                            erSkadeSykdomEllerLyteVesentligdel = true,
                            erNedsettelseIArbeidsevneHøyereEnnNedreGrense = true,
                            nedreGrense = NedreGrense.FEMTI,
                            nedsattArbeidsevneDato = LocalDate.now(),
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
        util.ventPåSvar(sak.id.toLong(), behandling.id.toLong())
        behandling = hentBehandling(sak.id)

        dataSource.transaction {
            AvklaringsbehovHendelseHåndterer(it).håndtere(
                behandling.id,
                LøsAvklaringsbehovBehandlingHendelse(
                    løsning = AvklarBistandsbehovLøsning(
                        bistandsVurdering = BistandVurderingDto(
                            begrunnelse = "Trenger hjelp fra nav",
                            erBehovForAktivBehandling = true,
                            erBehovForArbeidsrettetTiltak = false,
                            erBehovForAnnenOppfølging = null
                        ),
                    ),
                    behandlingVersjon = behandling.versjon,
                    bruker = Bruker("SAKSBEHANDLER")
                )
            )
        }
        util.ventPåSvar(sak.id.toLong(), behandling.id.toLong())
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
        util.ventPåSvar(sak.id.toLong(), behandling.id.toLong())
        behandling = hentBehandling(sak.id)

        // Saken står til en-trinnskontroll hos saksbehandler klar for å bli sendt til beslutter
        dataSource.transaction { dbConnection ->
            val avklaringsbehov = hentAvklaringsbehov(behandling.id, dbConnection)
            assertThat(avklaringsbehov.alle()).anySatisfy { assertThat(it.erÅpent() && it.definisjon == Definisjon.FORESLÅ_VEDTAK).isTrue() }
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
        util.ventPåSvar(sak.id.toLong(), behandling.id.toLong())
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
        util.ventPåSvar(sak.id.toLong(), behandling.id.toLong())
        behandling = hentBehandling(sak.id)

        dataSource.transaction {
            AvklaringsbehovHendelseHåndterer(it).håndtere(
                behandling.id,
                LøsAvklaringsbehovBehandlingHendelse(
                    løsning = AvklarSykdomLøsning(
                        sykdomsvurdering = SykdomsvurderingDto(
                            begrunnelse = "Er syk nok",
                            dokumenterBruktIVurdering = listOf(JournalpostId("123123")),
                            harSkadeSykdomEllerLyte = true,
                            erSkadeSykdomEllerLyteVesentligdel = true,
                            erNedsettelseIArbeidsevneHøyereEnnNedreGrense = true,
                            nedreGrense = NedreGrense.FEMTI,
                            nedsattArbeidsevneDato = LocalDate.now(),
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
        util.ventPåSvar(sak.id.toLong(), behandling.id.toLong())
        behandling = hentBehandling(sak.id)

        dataSource.transaction {
            AvklaringsbehovHendelseHåndterer(it).håndtere(
                behandling.id,
                LøsAvklaringsbehovBehandlingHendelse(
                    løsning = AvklarBistandsbehovLøsning(
                        bistandsVurdering = BistandVurderingDto(
                            begrunnelse = "Trenger hjelp fra nav",
                            erBehovForAktivBehandling = true,
                            erBehovForArbeidsrettetTiltak = false,
                            erBehovForAnnenOppfølging = null
                        ),
                    ),
                    behandlingVersjon = behandling.versjon,
                    bruker = Bruker("SAKSBEHANDLER")
                )
            )
        }
        util.ventPåSvar(sak.id.toLong(), behandling.id.toLong())
        behandling = hentBehandling(sak.id)
        // Saken er tilbake til en-trinnskontroll hos saksbehandler klar for å bli sendt til beslutter
        dataSource.transaction { dbConnection ->
            val avklaringsbehov = hentAvklaringsbehov(behandling.id, dbConnection)
            assertThat(avklaringsbehov.alle()).anySatisfy { assertTrue(it.erÅpent() && it.definisjon == Definisjon.FORESLÅ_VEDTAK) }
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
        util.ventPåSvar(sak.id.toLong(), behandling.id.toLong())

        // Saken står til To-trinnskontroll hos beslutter
        dataSource.transaction { connection ->
            val avklaringsbehov = hentAvklaringsbehov(behandling.id, connection)
            assertThat(avklaringsbehov.alle()).anySatisfy { assertTrue(it.erÅpent() && it.definisjon == Definisjon.FATTE_VEDTAK) }
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
        util.ventPåSvar(sak.id.toLong(), behandling.id.toLong())

        behandling = hentBehandling(sak.id)

        dataSource.transaction { connection ->
            val avklaringsbehov = hentAvklaringsbehov(behandling.id, connection)

            // Det er bestilt vedtaksbrev
            assertThat(avklaringsbehov.alle()).anySatisfy { assertTrue(it.erÅpent() && it.definisjon == Definisjon.BESTILL_BREV) }
            assertThat(behandling.status()).isEqualTo(Status.IVERKSETTES)

            val brevbestilling = BrevbestillingRepository(connection).hent(behandling.id, TypeBrev.VEDTAK_INNVILGELSE)!!
            AvklaringsbehovHendelseHåndterer(connection).håndtere(
                behandling.id,
                LøsAvklaringsbehovBehandlingHendelse(
                    løsning = BrevbestillingLøsning(
                        LøsBrevbestillingDto(
                            brevbestilling.referanse,
                            BrevbestillingLøsningStatus.KLAR_FOR_EDITERING
                        )
                    ),
                    behandlingVersjon = behandling.versjon,
                    bruker = BREV_SYSTEMBRUKER
                )
            )
            // Brevet er klar for forhåndsvisning og editering
            assertThat(BrevbestillingRepository(connection).hent(brevbestilling.referanse).status)
                .isEqualTo(no.nav.aap.behandlingsflyt.behandling.brev.bestilling.Status.FORHÅNDSVISNING_KLAR)
        }

        util.ventPåSvar(sak.id.toLong(), behandling.id.toLong())

        behandling = hentBehandling(sak.id)

        dataSource.transaction { connection ->
            val avklaringsbehov = hentAvklaringsbehov(behandling.id, connection)

            // Venter på at brevet skal fullføres
            assertThat(avklaringsbehov.alle()).anySatisfy { assertTrue(it.erÅpent() && it.definisjon == Definisjon.SKRIV_BREV) }

            val brevbestilling = BrevbestillingRepository(connection).hent(behandling.id, TypeBrev.VEDTAK_INNVILGELSE)!!
            AvklaringsbehovHendelseHåndterer(connection).håndtere(
                behandling.id,
                LøsAvklaringsbehovBehandlingHendelse(
                    løsning = SkrivBrevLøsning(brevbestillingReferanse = brevbestilling.referanse),
                    behandlingVersjon = behandling.versjon,
                    bruker = Bruker("SAKSBEHANDLER")
                )
            )

            // Brevet er fullført
            assertThat(BrevbestillingRepository(connection).hent(brevbestilling.referanse).status)
                .isEqualTo(no.nav.aap.behandlingsflyt.behandling.brev.bestilling.Status.FULLFØRT)
        }

        util.ventPåSvar(sak.id.toLong(), behandling.id.toLong())

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

        // Saken er avsluttet, så det skal ikke være flere åpne avklaringsbehov
        dataSource.transaction { connection ->
            val avklaringsbehov = hentAvklaringsbehov(behandling.id, connection)
            assertThat(avklaringsbehov.åpne()).isEmpty()
        }
    }

    @Test
    fun `skal avklare yrkesskade hvis det finnes spor av yrkesskade - yrkesskade har årsakssammenheng`() {
        val ident = ident()
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))

        FakePersoner.leggTil(
            TestPerson(
                identer = setOf(ident),
                fødselsdato = Fødselsdato(LocalDate.now().minusYears(20)),
                yrkesskade = listOf(TestYrkesskade())
            )
        )

        // Sender inn en søknad
        sendInnDokument(
            ident, DokumentMottattPersonHendelse(
                journalpost = JournalpostId("10"),
                mottattTidspunkt = LocalDateTime.now(),
                strukturertDokument = StrukturertDokument(
                    Søknad(student = SøknadStudentDto("NEI"), yrkesskade = "NEI", oppgitteBarn = null),
                    Brevkode.SØKNAD
                ),
                periode = periode
            )
        )
        util.ventPåSvar()

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
                            harSkadeSykdomEllerLyte = true,
                            erSkadeSykdomEllerLyteVesentligdel = true,
                            erNedsettelseIArbeidsevneHøyereEnnNedreGrense = true,
                            nedreGrense = NedreGrense.TRETTI,
                            nedsattArbeidsevneDato = LocalDate.now(),
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
        util.ventPåSvar(sak.id.toLong(), behandling.id.toLong())
        behandling = hentBehandling(sak.id)

        dataSource.transaction {
            AvklaringsbehovHendelseHåndterer(it).håndtere(
                behandling.id,
                LøsAvklaringsbehovBehandlingHendelse(
                    løsning = AvklarBistandsbehovLøsning(
                        bistandsVurdering = BistandVurderingDto(
                            begrunnelse = "Trenger hjelp fra nav",
                            erBehovForAktivBehandling = true,
                            erBehovForArbeidsrettetTiltak = false,
                            erBehovForAnnenOppfølging = null
                        ),
                    ),
                    behandlingVersjon = behandling.versjon,
                    bruker = Bruker("SAKSBEHANDLER")
                )
            )
        }
        util.ventPåSvar(sak.id.toLong(), behandling.id.toLong())
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
        util.ventPåSvar(sak.id.toLong(), behandling.id.toLong())
        behandling = hentBehandling(sak.id)

        dataSource.transaction {
            AvklaringsbehovHendelseHåndterer(it).håndtere(
                behandling.id,
                LøsAvklaringsbehovBehandlingHendelse(
                    løsning = FastsettBeregningstidspunktLøsning(
                        beregningVurdering = BeregningVurdering(
                            begrunnelse = "Trenger hjelp fra Nav",
                            ytterligereNedsattArbeidsevneDato = null,
                            antattÅrligInntekt = Beløp(700_000)
                        ),
                    ),
                    behandlingVersjon = behandling.versjon,
                    bruker = Bruker("SAKSBEHANDLER")
                )
            )
        }
        util.ventPåSvar(sak.id.toLong(), behandling.id.toLong())
        behandling = hentBehandling(sak.id)

        // Saken står til en-trinnskontroll hos saksbehandler klar for å bli sendt til beslutter
        dataSource.transaction {
            val avklaringsbehovene = hentAvklaringsbehov(behandling.id, it)
            assertThat(avklaringsbehovene.alle()).anySatisfy { avklaringsbehov -> assertThat(avklaringsbehov.erÅpent() && avklaringsbehov.definisjon == Definisjon.FORESLÅ_VEDTAK).isTrue() }
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
        util.ventPåSvar(sak.id.toLong(), behandling.id.toLong())

        // Saken står til To-trinnskontroll hos beslutter
        dataSource.transaction { connection ->
            val avklaringsbehov = hentAvklaringsbehov(behandling.id, connection)
            assertThat(avklaringsbehov.alle()).anySatisfy { assertThat(it.erÅpent() && it.definisjon == Definisjon.FATTE_VEDTAK).isTrue() }
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

        util.ventPåSvar(sak.id.toLong(), behandling.id.toLong())
        behandling = hentBehandling(sak.id)

        dataSource.transaction { connection ->
            val avklaringsbehov = hentAvklaringsbehov(behandling.id, connection)

            // Det er bestilt vedtaksbrev
            assertThat(avklaringsbehov.alle()).anySatisfy { assertTrue(it.erÅpent() && it.definisjon == Definisjon.BESTILL_BREV) }
            assertThat(behandling.status()).isEqualTo(Status.IVERKSETTES)

            val brevbestilling = BrevbestillingRepository(connection).hent(behandling.id, TypeBrev.VEDTAK_INNVILGELSE)!!
            AvklaringsbehovHendelseHåndterer(connection).håndtere(
                behandling.id,
                LøsAvklaringsbehovBehandlingHendelse(
                    løsning = BrevbestillingLøsning(
                        LøsBrevbestillingDto(
                            brevbestilling.referanse,
                            BrevbestillingLøsningStatus.KLAR_FOR_EDITERING
                        )
                    ),
                    behandlingVersjon = behandling.versjon,
                    bruker = BREV_SYSTEMBRUKER
                )
            )
            // Brevet er klar for forhåndsvisning og editering
            assertThat(BrevbestillingRepository(connection).hent(brevbestilling.referanse).status)
                .isEqualTo(no.nav.aap.behandlingsflyt.behandling.brev.bestilling.Status.FORHÅNDSVISNING_KLAR)
        }

        util.ventPåSvar(sak.id.toLong(), behandling.id.toLong())

        behandling = hentBehandling(sak.id)

        dataSource.transaction { connection ->
            val avklaringsbehov = hentAvklaringsbehov(behandling.id, connection)

            // Venter på at brevet skal fullføres
            assertThat(avklaringsbehov.alle()).anySatisfy { assertTrue(it.erÅpent() && it.definisjon == Definisjon.SKRIV_BREV) }

            val brevbestilling = BrevbestillingRepository(connection).hent(behandling.id, TypeBrev.VEDTAK_INNVILGELSE)!!
            AvklaringsbehovHendelseHåndterer(connection).håndtere(
                behandling.id,
                LøsAvklaringsbehovBehandlingHendelse(
                    løsning = SkrivBrevLøsning(brevbestillingReferanse = brevbestilling.referanse),
                    behandlingVersjon = behandling.versjon,
                    bruker = Bruker("SAKSBEHANDLER")
                )
            )

            // Brevet er fullført
            assertThat(BrevbestillingRepository(connection).hent(brevbestilling.referanse).status)
                .isEqualTo(no.nav.aap.behandlingsflyt.behandling.brev.bestilling.Status.FULLFØRT)
        }

        util.ventPåSvar(sak.id.toLong(), behandling.id.toLong())

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

        // Verifiser at beregningsgrunnlaget er av type yrkesskade
        dataSource.transaction {
            assertThat(BeregningsgrunnlagRepository(it).hentHvisEksisterer(behandling.id)?.javaClass).isEqualTo(
                GrunnlagYrkesskade::class.java
            )
        }
    }

    @Test
    fun `starter statistikk-jobb etter endt steg`(hendelser: List<StoppetBehandling>) {
        val ident = ident()
        val fom = LocalDate.now().minusMonths(3)
        val periode = Periode(fom, fom.plusYears(3))

        FakePersoner.leggTil(
            TestPerson(
                identer = setOf(ident),
                fødselsdato = Fødselsdato(LocalDate.now().minusYears(20)),
                yrkesskade = emptyList(),
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
                    Søknad(student = SøknadStudentDto("NEI"), yrkesskade = "NEI", oppgitteBarn = null),
                    Brevkode.SØKNAD
                ),
                periode = periode
            )
        )
        util.ventPåSvar()
        val sak = hentSak(ident, periode)

        assertThat(hendelser.first { it.saksnummer == sak.saksnummer.toString() }.saksnummer).isEqualTo(
            sak.saksnummer.toString()
        )
    }

    private fun sendInnDokument(
        ident: Ident,
        dokumentMottattPersonHendelse: DokumentMottattPersonHendelse
    ) {
        hendelsesMottak.håndtere(
            ident, dokumentMottattPersonHendelse
        )
    }

    @Test
    fun `to-trinn og ingen endring i gruppe etter sendt tilbake fra beslutter`() {
        val ident = ident()
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))

        FakePersoner.leggTil(
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
        sendInnDokument(
            ident, DokumentMottattPersonHendelse(
                journalpost = JournalpostId("11"),
                mottattTidspunkt = LocalDateTime.now(),
                strukturertDokument = StrukturertDokument(
                    Søknad(
                        student = SøknadStudentDto("JA", "JA"),
                        yrkesskade = "JA",
                        oppgitteBarn = null
                    ), Brevkode.SØKNAD
                ),
                periode = periode
            )
        )
        util.ventPåSvar()

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
                            avbruttStudieDato = LocalDate.now(),
                            avbruddMerEnn6Måneder = true,
                            harBehovForBehandling = true,
                            harAvbruttStudie = true,
                            avbruttPgaSykdomEllerSkade = true,
                            godkjentStudieAvLånekassen = false,
                        )
                    ),
                    behandlingVersjon = behandling.versjon,
                    bruker = Bruker("SAKSBEHANDLER")
                )
            )
        }
        util.ventPåSvar(sak.id.toLong(), behandling.id.toLong())
        behandling = hentBehandling(sak.id)

        dataSource.transaction {
            AvklaringsbehovHendelseHåndterer(it).håndtere(
                behandling.id,
                LøsAvklaringsbehovBehandlingHendelse(
                    løsning = AvklarSykdomLøsning(
                        sykdomsvurdering = SykdomsvurderingDto(
                            begrunnelse = "Arbeidsevnen er nedsatt med mer enn halvparten",
                            dokumenterBruktIVurdering = listOf(JournalpostId("123123")),
                            harSkadeSykdomEllerLyte = true,
                            erSkadeSykdomEllerLyteVesentligdel = true,
                            erNedsettelseIArbeidsevneHøyereEnnNedreGrense = true,
                            nedreGrense = NedreGrense.FEMTI,
                            nedsattArbeidsevneDato = LocalDate.now(),
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
        util.ventPåSvar(sak.id.toLong(), behandling.id.toLong())
        behandling = hentBehandling(sak.id)

        dataSource.transaction {
            AvklaringsbehovHendelseHåndterer(it).håndtere(
                behandling.id,
                LøsAvklaringsbehovBehandlingHendelse(
                    løsning = AvklarBistandsbehovLøsning(
                        bistandsVurdering = BistandVurderingDto(
                            begrunnelse = "Trenger hjelp fra nav",
                            erBehovForAktivBehandling = true,
                            erBehovForArbeidsrettetTiltak = false,
                            erBehovForAnnenOppfølging = null
                        ),
                    ),
                    behandlingVersjon = behandling.versjon,
                    bruker = Bruker("SAKSBEHANDLER")
                )
            )
        }
        util.ventPåSvar(sak.id.toLong(), behandling.id.toLong())
        behandling = hentBehandling(sak.id)

        // Saken står til en-trinnskontroll hos saksbehandler klar for å bli sendt til beslutter
        dataSource.transaction { connection ->
            val avklaringsbehov = hentAvklaringsbehov(behandling.id, connection)
            val actual = avklaringsbehov.alle()
            assertThat(actual).isNotEmpty()
            assertThat(actual).anySatisfy { behov -> assertThat(behov.erÅpent() && behov.definisjon == Definisjon.KVALITETSSIKRING).isTrue() }
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
        util.ventPåSvar(sak.id.toLong(), behandling.id.toLong())
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
        util.ventPåSvar(sak.id.toLong(), behandling.id.toLong())

        // Saken står til To-trinnskontroll hos beslutter
        dataSource.transaction { connection ->
            val avklaringsbehov = hentAvklaringsbehov(behandling.id, connection)
            assertThat(avklaringsbehov.alle()).anySatisfy { assertThat(it.erÅpent() && it.definisjon == Definisjon.FATTE_VEDTAK).isTrue() }
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
        util.ventPåSvar(sak.id.toLong(), behandling.id.toLong())

        behandling = hentBehandling(sak.id)
        assertThat(behandling.status()).isEqualTo(Status.UTREDES)
        dataSource.transaction { connection ->
            val avklaringsbehov = hentAvklaringsbehov(behandling.id, connection)
            assertThat(avklaringsbehov.alle()).anySatisfy { assertThat(it.erÅpent() && it.definisjon == Definisjon.AVKLAR_SYKDOM).isTrue() }
        }

        dataSource.transaction {
            AvklaringsbehovHendelseHåndterer(it).håndtere(
                behandling.id,
                LøsAvklaringsbehovBehandlingHendelse(
                    løsning = AvklarSykdomLøsning(
                        sykdomsvurdering = SykdomsvurderingDto(
                            begrunnelse = "Er syk nok",
                            dokumenterBruktIVurdering = listOf(JournalpostId("123123")),
                            harSkadeSykdomEllerLyte = true,
                            erSkadeSykdomEllerLyteVesentligdel = true,
                            erNedsettelseIArbeidsevneHøyereEnnNedreGrense = true,
                            nedreGrense = NedreGrense.FEMTI,
                            nedsattArbeidsevneDato = LocalDate.now(),
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
        util.ventPåSvar(sak.id.toLong(), behandling.id.toLong())
        behandling = hentBehandling(sak.id)

        // Saken står til en-trinnskontroll hos saksbehandler klar for å bli sendt til beslutter
        dataSource.transaction { connection ->
            val avklaringsbehov = hentAvklaringsbehov(behandling.id, connection)
            assertThat(avklaringsbehov.alle()).anySatisfy { behov -> assertThat(behov.erÅpent() && behov.definisjon == Definisjon.FORESLÅ_VEDTAK).isTrue() }
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
        util.ventPåSvar(sak.id.toLong(), behandling.id.toLong())

        // Saken står til To-trinnskontroll hos beslutter
        dataSource.transaction { connection ->
            val avklaringsbehov = hentAvklaringsbehov(behandling.id, connection)
            assertThat(avklaringsbehov.alle()).anySatisfy { assertThat(it.erÅpent() && it.definisjon == Definisjon.FATTE_VEDTAK).isTrue() }
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
        util.ventPåSvar(sak.id.toLong(), behandling.id.toLong())

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

    @Test
    fun `Ikke oppfylt på grunn av alder på søknadstidspunkt`(hendelser: List<StoppetBehandling>) {
        val ident = ident()
        hentPerson(ident)
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))

        FakePersoner.leggTil(
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
                strukturertDokument = StrukturertDokument(
                    Søknad(student = SøknadStudentDto("NEI"), yrkesskade = "NEI", oppgitteBarn = null),
                    Brevkode.SØKNAD
                ),
                periode = periode
            )
        )
        util.ventPåSvar()

        val sak = hentSak(ident, periode)
        val behandling = hentBehandling(sak.id)
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

        util.ventPåSvar(sak.id.toLong(), behandling.id.toLong())

        val status = dataSource.transaction { BehandlingRepositoryImpl(it).hent(behandling.id).status() }
        assertThat(status).isEqualTo(Status.UTREDES)

        dataSource.transaction {
            val behov = hentAvklaringsbehov(behandling.id, it)
            assertThat(behov.åpne()).allSatisfy { assertThat(it.definisjon.kode).isEqualTo(AVKLAR_SYKDOM_KODE) }
        }

        dataSource.transaction {
            AvklaringsbehovHendelseHåndterer(it).håndtere(
                behandling.id,
                LøsAvklaringsbehovBehandlingHendelse(
                    løsning = AvklarSykdomLøsning(
                        sykdomsvurdering = SykdomsvurderingDto(
                            begrunnelse = "Er syk nok",
                            dokumenterBruktIVurdering = listOf(JournalpostId("123123")),
                            harSkadeSykdomEllerLyte = false,
                            erSkadeSykdomEllerLyteVesentligdel = false,
                            erNedsettelseIArbeidsevneHøyereEnnNedreGrense = false,
                            nedreGrense = NedreGrense.FEMTI,
                            nedsattArbeidsevneDato = LocalDate.now(),
                            erArbeidsevnenNedsatt = false,
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

        util.ventPåSvar(sak.id.toLong(), behandling.id.toLong())

        dataSource.transaction {
            val behov = hentAvklaringsbehov(behandling.id, it)
            assertThat(behov.åpne()).allSatisfy { assertThat(it.definisjon.kode).isEqualTo(AVKLAR_BISTANDSBEHOV_KODE) }
        }

        dataSource.transaction {
            AvklaringsbehovHendelseHåndterer(it).håndtere(
                behandling.id,
                LøsAvklaringsbehovBehandlingHendelse(
                    løsning = AvklarBistandsbehovLøsning(
                        bistandsVurdering = BistandVurderingDto(
                            begrunnelse = "Trenger ikke hjelp fra nav",
                            erBehovForAktivBehandling = false,
                            erBehovForArbeidsrettetTiltak = false,
                            erBehovForAnnenOppfølging = false
                        ),
                    ),
                    behandlingVersjon = behandling.versjon,
                    bruker = Bruker("SAKSBEHANDLER")
                )
            )
        }

        util.ventPåSvar(sak.id.toLong(), behandling.id.toLong())

        dataSource.transaction {
            val behov = hentAvklaringsbehov(behandling.id, it)
            assertThat(behov.åpne()).allSatisfy { assertThat(it.definisjon.kode).isEqualTo(KVALITETSSIKRING_KODE) }
        }

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

        util.ventPåSvar(sak.id.toLong(), behandling.id.toLong())

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
        util.ventPåSvar(sak.id.toLong(), behandling.id.toLong())

        dataSource.transaction {
            val behov = hentAvklaringsbehov(behandling.id, it)
            assertThat(behov.åpne()).allSatisfy { assertThat(it.definisjon.kode).isEqualTo(FATTE_VEDTAK_KODE) }
        }


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

        util.ventPåSvar(sak.id.toLong(), behandling.id.toLong())

        dataSource.transaction { connection ->
            val brevbestilling = BrevbestillingRepository(connection).hent(behandling.id, TypeBrev.VEDTAK_AVSLAG)!!
            AvklaringsbehovHendelseHåndterer(connection).håndtere(
                behandling.id,
                LøsAvklaringsbehovBehandlingHendelse(
                    løsning = BrevbestillingLøsning(
                        LøsBrevbestillingDto(
                            brevbestilling.referanse,
                            BrevbestillingLøsningStatus.KLAR_FOR_EDITERING
                        )
                    ),
                    behandlingVersjon = behandling.versjon,
                    bruker = BREV_SYSTEMBRUKER
                )
            )
        }

        util.ventPåSvar(sak.id.toLong(), behandling.id.toLong())

        dataSource.transaction { connection ->
            val brevbestilling = BrevbestillingRepository(connection).hent(behandling.id, TypeBrev.VEDTAK_AVSLAG)!!
            AvklaringsbehovHendelseHåndterer(connection).håndtere(
                behandling.id,
                LøsAvklaringsbehovBehandlingHendelse(
                    løsning = SkrivBrevLøsning(brevbestillingReferanse = brevbestilling.referanse),
                    behandlingVersjon = behandling.versjon,
                    bruker = Bruker("SAKSBEHANDLER")
                )
            )
        }

        util.ventPåSvar(sak.id.toLong(), behandling.id.toLong())

        dataSource.transaction {
            val behov = hentAvklaringsbehov(behandling.id, it)
            assertThat(behov.åpne()).allSatisfy { assertThat(it.definisjon.kode).isEqualTo(FATTE_VEDTAK_KODE) }
        }

        util.ventPåSvar()

        assertThat(hentBehandling(sak.id).status()).isEqualTo(Status.AVSLUTTET)
        assertThat(hendelser.last().status).isEqualTo(BehandlingStatus.AVSLUTTET)
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
                strukturertDokument = StrukturertDokument(
                    Søknad(student = SøknadStudentDto("NEI"), yrkesskade = "NEI", oppgitteBarn = null),
                    Brevkode.SØKNAD
                ),
                periode = periode
            )
        )
        util.ventPåSvar()

        val sak = hentSak(ident, periode)
        var behandling = requireNotNull(hentBehandling(sak.id))

        assertThat(behandling.status()).isEqualTo(Status.UTREDES)

        dataSource.transaction { connection ->
            val avklaringsbehov = hentAvklaringsbehov(behandling.id, connection)
            assertThat(avklaringsbehov.alle()).anySatisfy { assertThat(it.erÅpent() && it.definisjon == Definisjon.AVKLAR_SYKDOM).isTrue() }
        }

        hendelsesMottak.håndtere(
            behandling.id,
            BehandlingSattPåVent(
                frist = null,
                begrunnelse = "Avventer dokumentasjon",
                bruker = SYSTEMBRUKER,
                behandlingVersjon = behandling.versjon,
                grunn = ÅrsakTilSettPåVent.VENTER_PÅ_OPPLYSNINGER
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
                .anySatisfy { assertThat(it.erÅpent() && it.definisjon == Definisjon.MANUELT_SATT_PÅ_VENT).isTrue() }
                .anySatisfy { assertThat(it.erÅpent() && it.definisjon == Definisjon.AVKLAR_SYKDOM).isTrue() }
        }
        util.ventPåSvar(sak.id.toLong(), behandling.id.toLong())
        hendelsesMottak.håndtere(
            ident, DokumentMottattPersonHendelse(
                journalpost = JournalpostId("3"),
                mottattTidspunkt = LocalDateTime.now(),
                strukturertDokument = StrukturertDokument(
                    Søknad(student = SøknadStudentDto("NEI"), yrkesskade = "NEI", oppgitteBarn = null),
                    Brevkode.SØKNAD
                ),
                periode = periode
            )
        )
        util.ventPåSvar(sak.id.toLong(), behandling.id.toLong())

        behandling = hentBehandling(sak.id)
        assertThat(behandling.status()).isEqualTo(Status.UTREDES)

        dataSource.transaction { connection ->
            val avklaringsbehov = hentAvklaringsbehov(behandling.id, connection)
            assertThat(avklaringsbehov.alle())
                .hasSize(2)
                .anySatisfy { assertThat(it.erÅpent() && it.definisjon == Definisjon.MANUELT_SATT_PÅ_VENT).isTrue() }
                .anySatisfy { assertThat(it.erÅpent() && it.definisjon == Definisjon.AVKLAR_SYKDOM).isTrue() }
        }

    }
}
