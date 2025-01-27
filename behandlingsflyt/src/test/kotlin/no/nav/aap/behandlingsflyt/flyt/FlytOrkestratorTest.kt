package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.SYSTEMBRUKER
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovHendelseHåndterer
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovOrkestrator
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.LøsAvklaringsbehovHendelse
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.BREV_SYSTEMBRUKER
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.vedtak.TotrinnsVurdering
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.vedtak.ÅrsakTilReturKode
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.ÅrsakTilSettPåVent
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarBistandsbehovLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarStudentLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarSykdomLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarYrkesskadeLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.BrevbestillingLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.FastsettBeregningstidspunktLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.FastsettYrkesskadeInntektLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.FatteVedtakLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.ForeslåVedtakLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.KvalitetssikringLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.SkrivBrevLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.ÅrsakTilRetur
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingRepositoryImpl
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.TypeBrev
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.TilkjentYtelseRepositoryImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.BeregningsgrunnlagRepositoryImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.GrunnlagYrkesskade
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepositoryImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.StrukturertDokument
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.AktivitetspliktRepositoryImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektPerÅr
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.UføreRepositoryImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningYrkeskaderBeløpVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningstidspunktVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.YrkesskadeBeløpVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.flate.BistandVurderingDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.flate.SykdomsvurderingDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.flate.YrkesskadevurderingDto
import no.nav.aap.behandlingsflyt.flyt.internals.DokumentMottattPersonHendelse
import no.nav.aap.behandlingsflyt.flyt.internals.TestHendelsesMottak
import no.nav.aap.behandlingsflyt.hendelse.avløp.BehandlingHendelseServiceImpl
import no.nav.aap.behandlingsflyt.hendelse.mottak.BehandlingSattPåVent
import no.nav.aap.behandlingsflyt.integrasjon.barn.PdlBarnGateway
import no.nav.aap.behandlingsflyt.integrasjon.ident.PdlIdentGateway
import no.nav.aap.behandlingsflyt.integrasjon.ident.PdlPersoninfoBulkGateway
import no.nav.aap.behandlingsflyt.integrasjon.ident.PdlPersoninfoGateway
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.brevbestilling.BrevbestillingLøsningStatus
import no.nav.aap.behandlingsflyt.kontrakt.brevbestilling.LøsBrevbestillingDto
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.SøknadMedlemskapDto
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.SøknadStudentDto
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.SøknadV0
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.StoppetBehandling
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.prosessering.HendelseMottattHåndteringJobbUtfører
import no.nav.aap.behandlingsflyt.prosessering.ProsesseringsJobber
import no.nav.aap.behandlingsflyt.repository.avklaringsbehov.AvklaringsbehovRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.barnetillegg.BarnetilleggRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.effektuer11_7.Effektuer11_7RepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.samordning.SamordningRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.underveis.UnderveisRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.dokument.arbeid.PliktkortRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.personopplysning.PersonopplysningRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.register.yrkesskade.YrkesskadeRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.arbeidsevne.ArbeidsevneRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.beregning.BeregningVurderingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.bistand.BistandRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.sykdom.SykdomRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.lås.TaSkriveLåsRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.pip.PipRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.StegStatus
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.behandlingsflyt.test.FakePersoner
import no.nav.aap.behandlingsflyt.test.Fakes
import no.nav.aap.behandlingsflyt.test.ident
import no.nav.aap.behandlingsflyt.test.modell.TestPerson
import no.nav.aap.behandlingsflyt.test.modell.TestYrkesskade
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.komponenter.httpklient.auth.Bruker
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.lookup.gateway.GatewayRegistry
import no.nav.aap.lookup.repository.RepositoryRegistry
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.Motor
import no.nav.aap.motor.testutil.TestUtil
import no.nav.aap.verdityper.dokument.JournalpostId
import no.nav.aap.verdityper.dokument.Kanal
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Year
import java.util.*

@Fakes
class FlytOrkestratorTest {

    companion object {
        private val dataSource = InitTestDatabase.dataSource
        private val motor = Motor(dataSource, 2, jobber = ProsesseringsJobber.alle())
        private val hendelsesMottak = TestHendelsesMottak(dataSource)
        private val util =
            TestUtil(dataSource, ProsesseringsJobber.alle().filter { it.cron() != null }.map { it.type() })

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

    @BeforeEach
    fun setUp() {
        RepositoryRegistry
            .register<BehandlingRepositoryImpl>()
            .register<PersonRepositoryImpl>()
            .register<SakRepositoryImpl>()
            .register<AvklaringsbehovRepositoryImpl>()
            .register<VilkårsresultatRepositoryImpl>()
            .register<PipRepositoryImpl>()
            .register<TaSkriveLåsRepositoryImpl>()
            .register<BeregningsgrunnlagRepositoryImpl>()
            .register<PersonopplysningRepositoryImpl>()
            .register<TilkjentYtelseRepositoryImpl>()
            .register<AktivitetspliktRepositoryImpl>()
            .register<BrevbestillingRepositoryImpl>()
            .register<SamordningRepositoryImpl>()
            .register<MottattDokumentRepositoryImpl>()
            .register<PliktkortRepositoryImpl>()
            .register<UnderveisRepositoryImpl>()
            .register<ArbeidsevneRepositoryImpl>()
            .register<Effektuer11_7RepositoryImpl>()
            .register<BarnetilleggRepositoryImpl>()
            .register<BistandRepositoryImpl>()
            .register<BeregningVurderingRepositoryImpl>()
            .register<SykdomRepositoryImpl>()
            .register<YrkesskadeRepositoryImpl>()
            .register<UføreRepositoryImpl>()
            .status()
        GatewayRegistry.register<PdlBarnGateway>()
            .register<PdlIdentGateway>()
            .register<PdlPersoninfoBulkGateway>()
            .register<PdlPersoninfoGateway>()
    }

    @Test
    fun `skal avklare yrkesskade hvis det finnes spor av yrkesskade`() {
        val fom = LocalDate.now().minusMonths(3)
        val periode = Periode(fom, fom.plusYears(3))

        // Simulerer et svar fra YS-løsning om at det finnes en yrkesskade
        val person = TestPerson(
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
        FakePersoner.leggTil(
            person
        )

        val ident = person.aktivIdent()

        // Sender inn en søknad
        sendInnDokument(
            ident, DokumentMottattPersonHendelse(
                journalpost = JournalpostId("20"),
                mottattTidspunkt = LocalDateTime.now().minusMonths(3),
                strukturertDokument = StrukturertDokument(
                    SøknadV0(
                        student = SøknadStudentDto("NEI"),
                        yrkesskade = "NEI",
                        oppgitteBarn = null,
                        medlemskap = null
                    ),
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
            AvklaringsbehovHendelseHåndterer(
                AvklaringsbehovOrkestrator(
                    it,
                    BehandlingHendelseServiceImpl(FlytJobbRepository(it), SakService(SakRepositoryImpl(it)))
                ), AvklaringsbehovRepositoryImpl(it), BehandlingRepositoryImpl(it)
            ).håndtere(
                behandling.id,
                LøsAvklaringsbehovHendelse(
                    løsning = AvklarSykdomLøsning(
                        sykdomsvurdering = SykdomsvurderingDto(
                            begrunnelse = "Er syk nok",
                            dokumenterBruktIVurdering = listOf(JournalpostId("123123")),
                            harSkadeSykdomEllerLyte = true,
                            erSkadeSykdomEllerLyteVesentligdel = true,
                            erNedsettelseIArbeidsevneMerEnnHalvparten = true,
                            erNedsettelseIArbeidsevneAvEnVissVarighet = true,
                            erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = null,
                            erArbeidsevnenNedsatt = true,
                            yrkesskadeBegrunnelse = null
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
            AvklaringsbehovHendelseHåndterer(
                AvklaringsbehovOrkestrator(
                    it,
                    BehandlingHendelseServiceImpl(FlytJobbRepository(it), SakService(SakRepositoryImpl(it)))
                ), AvklaringsbehovRepositoryImpl(it), BehandlingRepositoryImpl(it)
            ).håndtere(
                behandling.id,
                LøsAvklaringsbehovHendelse(
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
            AvklaringsbehovHendelseHåndterer(
                AvklaringsbehovOrkestrator(
                    it,
                    BehandlingHendelseServiceImpl(FlytJobbRepository(it), SakService(SakRepositoryImpl(it)))
                ), AvklaringsbehovRepositoryImpl(it), BehandlingRepositoryImpl(it)
            ).håndtere(
                behandling.id,
                LøsAvklaringsbehovHendelse(
                    løsning = KvalitetssikringLøsning(
                        avklaringsbehov.alle()
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
            AvklaringsbehovHendelseHåndterer(
                AvklaringsbehovOrkestrator(
                    it,
                    BehandlingHendelseServiceImpl(FlytJobbRepository(it), SakService(SakRepositoryImpl(it)))
                ), AvklaringsbehovRepositoryImpl(it), BehandlingRepositoryImpl(it)
            ).håndtere(
                behandling.id,
                LøsAvklaringsbehovHendelse(
                    løsning = AvklarYrkesskadeLøsning(
                        yrkesskadesvurdering = YrkesskadevurderingDto(
                            begrunnelse = "Ikke årsakssammenheng",
                            relevanteSaker = listOf(),
                            andelAvNedsettelsen = null,
                            erÅrsakssammenheng = false
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
            AvklaringsbehovHendelseHåndterer(
                AvklaringsbehovOrkestrator(
                    it,
                    BehandlingHendelseServiceImpl(FlytJobbRepository(it), SakService(SakRepositoryImpl(it)))
                ), AvklaringsbehovRepositoryImpl(it), BehandlingRepositoryImpl(it)
            ).håndtere(
                behandling.id,
                LøsAvklaringsbehovHendelse(
                    løsning = FastsettBeregningstidspunktLøsning(
                        beregningVurdering = BeregningstidspunktVurdering(
                            begrunnelse = "Trenger hjelp fra Nav",
                            nedsattArbeidsevneDato = LocalDate.now(),
                            ytterligereNedsattArbeidsevneDato = null,
                            ytterligereNedsattBegrunnelse = null
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
            AvklaringsbehovHendelseHåndterer(
                AvklaringsbehovOrkestrator(
                    it,
                    BehandlingHendelseServiceImpl(FlytJobbRepository(it), SakService(SakRepositoryImpl(it)))
                ), AvklaringsbehovRepositoryImpl(it), BehandlingRepositoryImpl(it)
            ).håndtere(
                behandling.id,
                LøsAvklaringsbehovHendelse(
                    løsning = FastsettBeregningstidspunktLøsning(
                        beregningVurdering = BeregningstidspunktVurdering(
                            begrunnelse = "Trenger hjelp fra Nav",
                            nedsattArbeidsevneDato = LocalDate.now(),
                            ytterligereNedsattArbeidsevneDato = null,
                            ytterligereNedsattBegrunnelse = null
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
        dataSource.transaction { dbConnection ->
            val avklaringsbehov = hentAvklaringsbehov(behandling.id, dbConnection)
            assertThat(avklaringsbehov.alle()).anySatisfy { assertThat(it.erÅpent() && it.definisjon == Definisjon.FORESLÅ_VEDTAK).isTrue() }
            assertThat(behandling.status()).isEqualTo(Status.UTREDES)
        }

        dataSource.transaction {
            AvklaringsbehovHendelseHåndterer(
                AvklaringsbehovOrkestrator(
                    it,
                    BehandlingHendelseServiceImpl(FlytJobbRepository(it), SakService(SakRepositoryImpl(it)))
                ), AvklaringsbehovRepositoryImpl(it), BehandlingRepositoryImpl(it)
            ).håndtere(
                behandling.id,
                LøsAvklaringsbehovHendelse(
                    løsning = ForeslåVedtakLøsning(),
                    behandlingVersjon = behandling.versjon,
                    bruker = Bruker("SAKSBEHANDLER")
                )
            )
        }
        util.ventPåSvar(sak.id.toLong(), behandling.id.toLong())
        behandling = hentBehandling(sak.id)

        dataSource.transaction { connection ->
            val avklaringsbehov = hentAvklaringsbehov(behandling.id, connection)
            AvklaringsbehovHendelseHåndterer(
                AvklaringsbehovOrkestrator(
                    connection,
                    BehandlingHendelseServiceImpl(
                        FlytJobbRepository(connection), SakService(
                            SakRepositoryImpl(
                                connection
                            )
                        )
                    )
                ), AvklaringsbehovRepositoryImpl(connection), BehandlingRepositoryImpl(connection)
            ).håndtere(
                behandling.id,
                LøsAvklaringsbehovHendelse(
                    løsning = FatteVedtakLøsning(
                        avklaringsbehov.alle()
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
            AvklaringsbehovHendelseHåndterer(
                AvklaringsbehovOrkestrator(
                    it,
                    BehandlingHendelseServiceImpl(FlytJobbRepository(it), SakService(SakRepositoryImpl(it)))
                ), AvklaringsbehovRepositoryImpl(it), BehandlingRepositoryImpl(it)
            ).håndtere(
                behandling.id,
                LøsAvklaringsbehovHendelse(
                    løsning = AvklarSykdomLøsning(
                        sykdomsvurdering = SykdomsvurderingDto(
                            begrunnelse = "Er syk nok",
                            dokumenterBruktIVurdering = listOf(JournalpostId("123123")),
                            harSkadeSykdomEllerLyte = true,
                            erSkadeSykdomEllerLyteVesentligdel = true,
                            erNedsettelseIArbeidsevneMerEnnHalvparten = true,
                            erNedsettelseIArbeidsevneAvEnVissVarighet = true,
                            erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = null,
                            erArbeidsevnenNedsatt = true,
                            yrkesskadeBegrunnelse = null
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
            AvklaringsbehovHendelseHåndterer(
                AvklaringsbehovOrkestrator(
                    it,
                    BehandlingHendelseServiceImpl(FlytJobbRepository(it), SakService(SakRepositoryImpl(it)))
                ), AvklaringsbehovRepositoryImpl(it), BehandlingRepositoryImpl(it)
            ).håndtere(
                behandling.id,
                LøsAvklaringsbehovHendelse(
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
            AvklaringsbehovHendelseHåndterer(
                AvklaringsbehovOrkestrator(
                    it,
                    BehandlingHendelseServiceImpl(FlytJobbRepository(it), SakService(SakRepositoryImpl(it)))
                ), AvklaringsbehovRepositoryImpl(it), BehandlingRepositoryImpl(it)
            ).håndtere(
                behandling.id,
                LøsAvklaringsbehovHendelse(
                    løsning = AvklarYrkesskadeLøsning(
                        yrkesskadesvurdering = YrkesskadevurderingDto(
                            begrunnelse = "Ikke årsakssammenheng",
                            relevanteSaker = listOf(),
                            andelAvNedsettelsen = null,
                            erÅrsakssammenheng = false
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
            AvklaringsbehovHendelseHåndterer(
                AvklaringsbehovOrkestrator(
                    it,
                    BehandlingHendelseServiceImpl(FlytJobbRepository(it), SakService(SakRepositoryImpl(it)))
                ), AvklaringsbehovRepositoryImpl(it), BehandlingRepositoryImpl(it)
            ).håndtere(
                behandling.id,
                LøsAvklaringsbehovHendelse(
                    løsning = FastsettBeregningstidspunktLøsning(
                        beregningVurdering = BeregningstidspunktVurdering(
                            begrunnelse = "Trenger hjelp fra Nav",
                            nedsattArbeidsevneDato = LocalDate.now(),
                            ytterligereNedsattArbeidsevneDato = null,
                            ytterligereNedsattBegrunnelse = null
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
            AvklaringsbehovHendelseHåndterer(
                AvklaringsbehovOrkestrator(
                    it,
                    BehandlingHendelseServiceImpl(FlytJobbRepository(it), SakService(SakRepositoryImpl(it)))
                ), AvklaringsbehovRepositoryImpl(it), BehandlingRepositoryImpl(it)
            ).håndtere(
                behandling.id,
                LøsAvklaringsbehovHendelse(
                    løsning = ForeslåVedtakLøsning(),
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
            AvklaringsbehovHendelseHåndterer(
                AvklaringsbehovOrkestrator(
                    connection,
                    BehandlingHendelseServiceImpl(
                        FlytJobbRepository(connection), SakService(
                            SakRepositoryImpl(
                                connection
                            )
                        )
                    )
                ), AvklaringsbehovRepositoryImpl(connection), BehandlingRepositoryImpl(connection)
            ).håndtere(
                behandling.id,
                LøsAvklaringsbehovHendelse(
                    løsning = FatteVedtakLøsning(
                        avklaringsbehov.alle()
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

        dataSource.transaction { connection ->
            val avklaringsbehov = hentAvklaringsbehov(behandling.id, connection)

            // Det er bestilt vedtaksbrev
            assertThat(avklaringsbehov.alle()).anySatisfy { assertTrue(it.erÅpent() && it.definisjon == Definisjon.BESTILL_BREV) }
            assertThat(behandling.status()).isEqualTo(Status.IVERKSETTES)

            val brevbestilling =
                BrevbestillingRepositoryImpl(connection).hent(behandling.id)
                    .first { it.typeBrev == TypeBrev.VEDTAK_INNVILGELSE }
            AvklaringsbehovHendelseHåndterer(
                AvklaringsbehovOrkestrator(
                    connection,
                    BehandlingHendelseServiceImpl(
                        FlytJobbRepository(connection), SakService(
                            SakRepositoryImpl(
                                connection
                            )
                        )
                    )
                ), AvklaringsbehovRepositoryImpl(connection), BehandlingRepositoryImpl(connection)
            ).håndtere(
                behandling.id,
                LøsAvklaringsbehovHendelse(
                    løsning = BrevbestillingLøsning(
                        LøsBrevbestillingDto(
                            behandlingReferanse = behandling.referanse.referanse,
                            bestillingReferanse = brevbestilling.referanse.brevbestillingReferanse,
                            status = BrevbestillingLøsningStatus.KLAR_FOR_EDITERING
                        )
                    ),
                    behandlingVersjon = behandling.versjon,
                    bruker = BREV_SYSTEMBRUKER
                )
            )
            // Brevet er klar for forhåndsvisning og editering
            assertThat(
                BrevbestillingRepositoryImpl(connection).hent(behandling.id)
                    .first { it.typeBrev == TypeBrev.VEDTAK_INNVILGELSE }.status
            ).isEqualTo(
                no.nav.aap.behandlingsflyt.behandling.brev.bestilling.Status.FORHÅNDSVISNING_KLAR
            )
        }

        util.ventPåSvar(sak.id.toLong(), behandling.id.toLong())

        behandling = hentBehandling(sak.id)

        dataSource.transaction { connection ->
            val avklaringsbehov = hentAvklaringsbehov(behandling.id, connection)

            // Venter på at brevet skal fullføres
            assertThat(avklaringsbehov.alle()).anySatisfy { assertTrue(it.erÅpent() && it.definisjon == Definisjon.SKRIV_BREV) }

            val brevbestilling =
                BrevbestillingRepositoryImpl(connection).hent(behandling.id)
                    .first { it.typeBrev == TypeBrev.VEDTAK_INNVILGELSE }
            AvklaringsbehovHendelseHåndterer(
                AvklaringsbehovOrkestrator(
                    connection,
                    BehandlingHendelseServiceImpl(
                        FlytJobbRepository(connection), SakService(
                            SakRepositoryImpl(
                                connection
                            )
                        )
                    )
                ), AvklaringsbehovRepositoryImpl(connection), BehandlingRepositoryImpl(connection)
            ).håndtere(
                behandling.id,
                LøsAvklaringsbehovHendelse(
                    løsning = SkrivBrevLøsning(brevbestillingReferanse = brevbestilling.referanse.brevbestillingReferanse),
                    behandlingVersjon = behandling.versjon,
                    bruker = Bruker("SAKSBEHANDLER")
                )
            )

            // Brevet er fullført
            assertThat(
                BrevbestillingRepositoryImpl(connection).hent(behandling.id)
                    .first { it.typeBrev == TypeBrev.VEDTAK_INNVILGELSE }.status
            ).isEqualTo(
                no.nav.aap.behandlingsflyt.behandling.brev.bestilling.Status.FULLFØRT
            )
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
            UnderveisRepositoryImpl(connection).hent(behandling.id)
        }

        assertThat(underveisGrunnlag.perioder).isNotEmpty
        assertThat(underveisGrunnlag.perioder.any { it.gradering.gradering.prosentverdi() > 0 }).isTrue()

        // Saken er avsluttet, så det skal ikke være flere åpne avklaringsbehov
        dataSource.transaction { connection ->
            val avklaringsbehov = hentAvklaringsbehov(behandling.id, connection)
            assertThat(avklaringsbehov.åpne()).isEmpty()
        }

        sendInnDokument(
            ident, DokumentMottattPersonHendelse(
                journalpost = JournalpostId("29"),
                mottattTidspunkt = LocalDateTime.now().minusMonths(3),
                strukturertDokument = StrukturertDokument(
                    SøknadV0(
                        student = SøknadStudentDto("NEI"),
                        yrkesskade = "NEI",
                        oppgitteBarn = null,
                        medlemskap = null
                    ),
                ),
                periode = periode
            )
        )
        util.ventPåSvar(sakId = sak.id.toLong())

        behandling = hentBehandling(sak.id)
        assertThat(behandling.typeBehandling()).isEqualTo(TypeBehandling.Revurdering)
        assertThat(behandling.status()).isEqualTo(Status.UTREDES)

        dataSource.transaction {
            AvklaringsbehovHendelseHåndterer(
                AvklaringsbehovOrkestrator(
                    it,
                    BehandlingHendelseServiceImpl(FlytJobbRepository(it), SakService(SakRepositoryImpl(it)))
                ), AvklaringsbehovRepositoryImpl(it), BehandlingRepositoryImpl(it)
            ).håndtere(
                behandling.id,
                LøsAvklaringsbehovHendelse(
                    løsning = AvklarSykdomLøsning(
                        sykdomsvurdering = SykdomsvurderingDto(
                            begrunnelse = "Er syk nok",
                            dokumenterBruktIVurdering = listOf(JournalpostId("1349532")),
                            harSkadeSykdomEllerLyte = true,
                            erSkadeSykdomEllerLyteVesentligdel = true,
                            erNedsettelseIArbeidsevneMerEnnHalvparten = true,
                            erNedsettelseIArbeidsevneAvEnVissVarighet = true,
                            erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = null,
                            erArbeidsevnenNedsatt = true,
                            yrkesskadeBegrunnelse = null
                        )
                    ),
                    behandlingVersjon = behandling.versjon,
                    bruker = Bruker("SAKSBEHANDLER")
                )
            )
        }
        util.ventPåSvar(sak.id.toLong(), behandling.id.toLong())
        behandling = hentBehandling(sak.id)

        dataSource.transaction { connection ->
            val avklaringsbehov = hentAvklaringsbehov(behandling.id, connection)
            assertThat(avklaringsbehov.alle()).anySatisfy { assertTrue(it.erÅpent() && it.definisjon == Definisjon.AVKLAR_BISTANDSBEHOV) }
            assertThat(behandling.status()).isEqualTo(Status.UTREDES)
        }
    }

    @Test
    fun `skal avklare yrkesskade hvis det finnes spor av yrkesskade - yrkesskade har årsakssammenheng`() {
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))

        val person = TestPerson(
            fødselsdato = Fødselsdato(LocalDate.now().minusYears(20)),
            yrkesskade = listOf(TestYrkesskade()),
            uføre = null
        )
        FakePersoner.leggTil(
            person
        )

        val ident = person.aktivIdent()

        // Sender inn en søknad
        sendInnDokument(
            ident, DokumentMottattPersonHendelse(
                journalpost = JournalpostId("10"),
                mottattTidspunkt = LocalDateTime.now(),
                strukturertDokument = StrukturertDokument(
                    SøknadV0(
                        student = SøknadStudentDto("NEI"),
                        yrkesskade = "NEI",
                        oppgitteBarn = null,
                        medlemskap = null
                    ),
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
            AvklaringsbehovHendelseHåndterer(
                AvklaringsbehovOrkestrator(
                    it,
                    BehandlingHendelseServiceImpl(FlytJobbRepository(it), SakService(SakRepositoryImpl(it)))
                ), AvklaringsbehovRepositoryImpl(it), BehandlingRepositoryImpl(it)
            ).håndtere(
                behandling.id,
                LøsAvklaringsbehovHendelse(
                    løsning = AvklarSykdomLøsning(
                        sykdomsvurdering = SykdomsvurderingDto(
                            begrunnelse = "Er syk nok",
                            dokumenterBruktIVurdering = listOf(JournalpostId("123123")),
                            harSkadeSykdomEllerLyte = true,
                            erSkadeSykdomEllerLyteVesentligdel = true,
                            erNedsettelseIArbeidsevneMerEnnHalvparten = true,
                            erNedsettelseIArbeidsevneAvEnVissVarighet = true,
                            erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = null,
                            erArbeidsevnenNedsatt = true,
                            yrkesskadeBegrunnelse = null
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
            AvklaringsbehovHendelseHåndterer(
                AvklaringsbehovOrkestrator(
                    it,
                    BehandlingHendelseServiceImpl(FlytJobbRepository(it), SakService(SakRepositoryImpl(it)))
                ), AvklaringsbehovRepositoryImpl(it), BehandlingRepositoryImpl(it)
            ).håndtere(
                behandling.id,
                LøsAvklaringsbehovHendelse(
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
            AvklaringsbehovHendelseHåndterer(
                AvklaringsbehovOrkestrator(
                    it,
                    BehandlingHendelseServiceImpl(FlytJobbRepository(it), SakService(SakRepositoryImpl(it)))
                ), AvklaringsbehovRepositoryImpl(it), BehandlingRepositoryImpl(it)
            ).håndtere(
                behandling.id,
                LøsAvklaringsbehovHendelse(
                    løsning = KvalitetssikringLøsning(
                        avklaringsbehov.alle()
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
            AvklaringsbehovHendelseHåndterer(
                AvklaringsbehovOrkestrator(
                    it,
                    BehandlingHendelseServiceImpl(FlytJobbRepository(it), SakService(SakRepositoryImpl(it)))
                ), AvklaringsbehovRepositoryImpl(it), BehandlingRepositoryImpl(it)
            ).håndtere(
                behandling.id,
                LøsAvklaringsbehovHendelse(
                    løsning = AvklarYrkesskadeLøsning(
                        yrkesskadesvurdering = YrkesskadevurderingDto(
                            begrunnelse = "Veldig relevante",
                            relevanteSaker = person.yrkesskade.map { it.saksreferanse },
                            andelAvNedsettelsen = 50,
                            erÅrsakssammenheng = true
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
            AvklaringsbehovHendelseHåndterer(
                AvklaringsbehovOrkestrator(
                    it,
                    BehandlingHendelseServiceImpl(FlytJobbRepository(it), SakService(SakRepositoryImpl(it)))
                ), AvklaringsbehovRepositoryImpl(it), BehandlingRepositoryImpl(it)
            ).håndtere(
                behandling.id,
                LøsAvklaringsbehovHendelse(
                    løsning = FastsettBeregningstidspunktLøsning(
                        beregningVurdering = BeregningstidspunktVurdering(
                            begrunnelse = "Trenger hjelp fra Nav",
                            nedsattArbeidsevneDato = LocalDate.now(),
                            ytterligereNedsattArbeidsevneDato = null,
                            ytterligereNedsattBegrunnelse = null
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
            AvklaringsbehovHendelseHåndterer(
                AvklaringsbehovOrkestrator(
                    it,
                    BehandlingHendelseServiceImpl(FlytJobbRepository(it), SakService(SakRepositoryImpl(it)))
                ), AvklaringsbehovRepositoryImpl(it), BehandlingRepositoryImpl(it)
            ).håndtere(
                behandling.id,
                LøsAvklaringsbehovHendelse(
                    løsning = FastsettYrkesskadeInntektLøsning(
                        yrkesskadeInntektVurdering = BeregningYrkeskaderBeløpVurdering(
                            vurderinger = person.yrkesskade.map {
                                YrkesskadeBeløpVurdering(
                                    antattÅrligInntekt = Beløp(5000000),
                                    referanse = it.saksreferanse,
                                    begrunnelse = "Trenger hjelp fra Nav"
                                )
                            },
                        )
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
            AvklaringsbehovHendelseHåndterer(
                AvklaringsbehovOrkestrator(
                    it,
                    BehandlingHendelseServiceImpl(FlytJobbRepository(it), SakService(SakRepositoryImpl(it)))
                ), AvklaringsbehovRepositoryImpl(it), BehandlingRepositoryImpl(it)
            ).håndtere(
                behandling.id,
                LøsAvklaringsbehovHendelse(
                    løsning = ForeslåVedtakLøsning(),
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
            AvklaringsbehovHendelseHåndterer(
                AvklaringsbehovOrkestrator(
                    connection,
                    BehandlingHendelseServiceImpl(
                        FlytJobbRepository(connection), SakService(
                            SakRepositoryImpl(
                                connection
                            )
                        )
                    )
                ), AvklaringsbehovRepositoryImpl(connection), BehandlingRepositoryImpl(connection)
            ).håndtere(
                behandling.id,
                LøsAvklaringsbehovHendelse(
                    løsning = FatteVedtakLøsning(
                        avklaringsbehov.alle()
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

        dataSource.transaction { connection ->
            val avklaringsbehov = hentAvklaringsbehov(behandling.id, connection)

            // Det er bestilt vedtaksbrev
            assertThat(avklaringsbehov.alle()).anySatisfy { assertTrue(it.erÅpent() && it.definisjon == Definisjon.BESTILL_BREV) }
            assertThat(behandling.status()).isEqualTo(Status.IVERKSETTES)

            val brevbestilling =
                BrevbestillingRepositoryImpl(connection).hent(behandling.id)
                    .first { it.typeBrev == TypeBrev.VEDTAK_INNVILGELSE }
            AvklaringsbehovHendelseHåndterer(
                AvklaringsbehovOrkestrator(
                    connection,
                    BehandlingHendelseServiceImpl(
                        FlytJobbRepository(connection), SakService(
                            SakRepositoryImpl(
                                connection
                            )
                        )
                    )
                ), AvklaringsbehovRepositoryImpl(connection), BehandlingRepositoryImpl(connection)
            ).håndtere(
                behandling.id,
                LøsAvklaringsbehovHendelse(
                    løsning = BrevbestillingLøsning(
                        LøsBrevbestillingDto(
                            behandlingReferanse = behandling.referanse.referanse,
                            bestillingReferanse = brevbestilling.referanse.brevbestillingReferanse,
                            status = BrevbestillingLøsningStatus.KLAR_FOR_EDITERING
                        )
                    ),
                    behandlingVersjon = behandling.versjon,
                    bruker = BREV_SYSTEMBRUKER
                )
            )
            // Brevet er klar for forhåndsvisning og editering
            assertThat(
                BrevbestillingRepositoryImpl(connection).hent(behandling.id)
                    .first { it.typeBrev == TypeBrev.VEDTAK_INNVILGELSE }.status
            ).isEqualTo(
                no.nav.aap.behandlingsflyt.behandling.brev.bestilling.Status.FORHÅNDSVISNING_KLAR
            )
        }

        util.ventPåSvar(sak.id.toLong(), behandling.id.toLong())

        behandling = hentBehandling(sak.id)

        dataSource.transaction { connection ->
            val avklaringsbehov = hentAvklaringsbehov(behandling.id, connection)

            // Venter på at brevet skal fullføres
            assertThat(avklaringsbehov.alle()).anySatisfy { assertTrue(it.erÅpent() && it.definisjon == Definisjon.SKRIV_BREV) }

            val brevbestilling =
                BrevbestillingRepositoryImpl(connection).hent(behandling.id)
                    .first { it.typeBrev == TypeBrev.VEDTAK_INNVILGELSE }
            AvklaringsbehovHendelseHåndterer(
                AvklaringsbehovOrkestrator(
                    connection,
                    BehandlingHendelseServiceImpl(
                        FlytJobbRepository(connection), SakService(
                            SakRepositoryImpl(
                                connection
                            )
                        )
                    )
                ), AvklaringsbehovRepositoryImpl(connection), BehandlingRepositoryImpl(connection)
            ).håndtere(
                behandling.id,
                LøsAvklaringsbehovHendelse(
                    løsning = SkrivBrevLøsning(brevbestillingReferanse = brevbestilling.referanse.brevbestillingReferanse),
                    behandlingVersjon = behandling.versjon,
                    bruker = Bruker("SAKSBEHANDLER")
                )
            )

            // Brevet er fullført
            assertThat(
                BrevbestillingRepositoryImpl(connection).hent(behandling.id)
                    .first { it.typeBrev == TypeBrev.VEDTAK_INNVILGELSE }.status
            ).isEqualTo(
                no.nav.aap.behandlingsflyt.behandling.brev.bestilling.Status.FULLFØRT
            )
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
            assertThat(BeregningsgrunnlagRepositoryImpl(it).hentHvisEksisterer(behandling.id)?.javaClass).isEqualTo(
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
                    SøknadV0(
                        student = SøknadStudentDto("NEI"),
                        yrkesskade = "NEI",
                        oppgitteBarn = null,
                        medlemskap = null
                    ),
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
                    SøknadV0(
                        student = SøknadStudentDto("JA", "JA"),
                        yrkesskade = "JA",
                        oppgitteBarn = null,
                        medlemskap = null
                    )
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
            AvklaringsbehovHendelseHåndterer(
                AvklaringsbehovOrkestrator(
                    it,
                    BehandlingHendelseServiceImpl(FlytJobbRepository(it), SakService(SakRepositoryImpl(it)))
                ), AvklaringsbehovRepositoryImpl(it), BehandlingRepositoryImpl(it)
            ).håndtere(
                behandling.id,
                LøsAvklaringsbehovHendelse(
                    løsning = AvklarStudentLøsning(
                        studentvurdering = StudentVurdering(
                            begrunnelse = "Er student",
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
            AvklaringsbehovHendelseHåndterer(
                AvklaringsbehovOrkestrator(
                    it,
                    BehandlingHendelseServiceImpl(FlytJobbRepository(it), SakService(SakRepositoryImpl(it)))
                ), AvklaringsbehovRepositoryImpl(it), BehandlingRepositoryImpl(it)
            ).håndtere(
                behandling.id,
                LøsAvklaringsbehovHendelse(
                    løsning = AvklarSykdomLøsning(
                        sykdomsvurdering = SykdomsvurderingDto(
                            begrunnelse = "Arbeidsevnen er nedsatt med mer enn halvparten",
                            dokumenterBruktIVurdering = listOf(JournalpostId("123123")),
                            harSkadeSykdomEllerLyte = true,
                            erSkadeSykdomEllerLyteVesentligdel = true,
                            erNedsettelseIArbeidsevneMerEnnHalvparten = true,
                            erNedsettelseIArbeidsevneAvEnVissVarighet = true,
                            erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = null,
                            erArbeidsevnenNedsatt = true,
                            yrkesskadeBegrunnelse = null
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
            AvklaringsbehovHendelseHåndterer(
                AvklaringsbehovOrkestrator(
                    it,
                    BehandlingHendelseServiceImpl(FlytJobbRepository(it), SakService(SakRepositoryImpl(it)))
                ), AvklaringsbehovRepositoryImpl(it), BehandlingRepositoryImpl(it)
            ).håndtere(
                behandling.id,
                LøsAvklaringsbehovHendelse(
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
            AvklaringsbehovHendelseHåndterer(
                AvklaringsbehovOrkestrator(
                    it,
                    BehandlingHendelseServiceImpl(FlytJobbRepository(it), SakService(SakRepositoryImpl(it)))
                ), AvklaringsbehovRepositoryImpl(it), BehandlingRepositoryImpl(it)
            ).håndtere(
                behandling.id,
                LøsAvklaringsbehovHendelse(
                    løsning = KvalitetssikringLøsning(
                        avklaringsbehov.alle()
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
            AvklaringsbehovHendelseHåndterer(
                AvklaringsbehovOrkestrator(
                    it,
                    BehandlingHendelseServiceImpl(FlytJobbRepository(it), SakService(SakRepositoryImpl(it)))
                ), AvklaringsbehovRepositoryImpl(it), BehandlingRepositoryImpl(it)
            ).håndtere(
                behandling.id,
                LøsAvklaringsbehovHendelse(
                    løsning = AvklarYrkesskadeLøsning(
                        yrkesskadesvurdering = YrkesskadevurderingDto(
                            begrunnelse = "",
                            relevanteSaker = listOf(),
                            andelAvNedsettelsen = null,
                            erÅrsakssammenheng = false
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
            AvklaringsbehovHendelseHåndterer(
                AvklaringsbehovOrkestrator(
                    it,
                    BehandlingHendelseServiceImpl(FlytJobbRepository(it), SakService(SakRepositoryImpl(it)))
                ), AvklaringsbehovRepositoryImpl(it), BehandlingRepositoryImpl(it)
            ).håndtere(
                behandling.id,
                LøsAvklaringsbehovHendelse(
                    løsning = FastsettBeregningstidspunktLøsning(
                        beregningVurdering = BeregningstidspunktVurdering(
                            begrunnelse = "Trenger hjelp fra Nav",
                            nedsattArbeidsevneDato = LocalDate.now(),
                            ytterligereNedsattArbeidsevneDato = null,
                            ytterligereNedsattBegrunnelse = null
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
            AvklaringsbehovHendelseHåndterer(
                AvklaringsbehovOrkestrator(
                    it,
                    BehandlingHendelseServiceImpl(FlytJobbRepository(it), SakService(SakRepositoryImpl(it)))
                ), AvklaringsbehovRepositoryImpl(it), BehandlingRepositoryImpl(it)
            ).håndtere(
                behandling.id,
                LøsAvklaringsbehovHendelse(
                    løsning = ForeslåVedtakLøsning(),
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
            AvklaringsbehovHendelseHåndterer(
                AvklaringsbehovOrkestrator(
                    connection,
                    BehandlingHendelseServiceImpl(
                        FlytJobbRepository(connection), SakService(
                            SakRepositoryImpl(
                                connection
                            )
                        )
                    )
                ), AvklaringsbehovRepositoryImpl(connection), BehandlingRepositoryImpl(connection)
            ).håndtere(
                behandling.id,
                LøsAvklaringsbehovHendelse(
                    løsning = FatteVedtakLøsning(
                        avklaringsbehov.alle()
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
            AvklaringsbehovHendelseHåndterer(
                AvklaringsbehovOrkestrator(
                    it,
                    BehandlingHendelseServiceImpl(FlytJobbRepository(it), SakService(SakRepositoryImpl(it)))
                ), AvklaringsbehovRepositoryImpl(it), BehandlingRepositoryImpl(it)
            ).håndtere(
                behandling.id,
                LøsAvklaringsbehovHendelse(
                    løsning = AvklarSykdomLøsning(
                        sykdomsvurdering = SykdomsvurderingDto(
                            begrunnelse = "Er syk nok",
                            dokumenterBruktIVurdering = listOf(JournalpostId("123123")),
                            harSkadeSykdomEllerLyte = true,
                            erSkadeSykdomEllerLyteVesentligdel = true,
                            erNedsettelseIArbeidsevneMerEnnHalvparten = true,
                            erNedsettelseIArbeidsevneAvEnVissVarighet = true,
                            erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = null,
                            erArbeidsevnenNedsatt = true,
                            yrkesskadeBegrunnelse = null
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

        dataSource.transaction {
            AvklaringsbehovHendelseHåndterer(
                AvklaringsbehovOrkestrator(
                    it,
                    BehandlingHendelseServiceImpl(FlytJobbRepository(it), SakService(SakRepositoryImpl(it)))
                ), AvklaringsbehovRepositoryImpl(it), BehandlingRepositoryImpl(it)
            ).håndtere(
                behandling.id,
                LøsAvklaringsbehovHendelse(
                    løsning = FastsettBeregningstidspunktLøsning(
                        beregningVurdering = BeregningstidspunktVurdering(
                            begrunnelse = "Trenger hjelp fra Nav",
                            nedsattArbeidsevneDato = LocalDate.now(),
                            ytterligereNedsattArbeidsevneDato = null,
                            ytterligereNedsattBegrunnelse = null
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
            assertThat(avklaringsbehov.alle()).anySatisfy { behov -> assertThat(behov.erÅpent() && behov.definisjon == Definisjon.FORESLÅ_VEDTAK).isTrue() }
            assertThat(behandling.status()).isEqualTo(Status.UTREDES)
        }

        dataSource.transaction {
            AvklaringsbehovHendelseHåndterer(
                AvklaringsbehovOrkestrator(
                    it,
                    BehandlingHendelseServiceImpl(FlytJobbRepository(it), SakService(SakRepositoryImpl(it)))
                ), AvklaringsbehovRepositoryImpl(it), BehandlingRepositoryImpl(it)
            ).håndtere(
                behandling.id,
                LøsAvklaringsbehovHendelse(
                    løsning = ForeslåVedtakLøsning(),
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
            AvklaringsbehovHendelseHåndterer(
                AvklaringsbehovOrkestrator(
                    connection,
                    BehandlingHendelseServiceImpl(
                        FlytJobbRepository(connection), SakService(
                            SakRepositoryImpl(
                                connection
                            )
                        )
                    )
                ), AvklaringsbehovRepositoryImpl(connection), BehandlingRepositoryImpl(connection)
            ).håndtere(
                behandling.id,
                LøsAvklaringsbehovHendelse(
                    løsning = FatteVedtakLøsning(
                        avklaringsbehov.alle()
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
                PersonRepositoryImpl(connection).finnEllerOpprett(listOf(ident)),
                periode
            )
        }
    }

    private fun hentVilkårsresultat(behandlingId: BehandlingId): Vilkårsresultat {
        return dataSource.transaction { connection ->
            VilkårsresultatRepositoryImpl(connection).hent(behandlingId)
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
                    SøknadV0(
                        student = SøknadStudentDto("NEI"),
                        yrkesskade = "NEI",
                        oppgitteBarn = null,
                        medlemskap = null
                    ),

                    ),
                periode = periode
            )
        )
        util.ventPåSvar()

        val sak = hentSak(ident, periode)
        var behandling = hentBehandling(sak.id)
        assertThat(behandling.typeBehandling()).isEqualTo(TypeBehandling.Førstegangsbehandling)

        val stegHistorikk = behandling.stegHistorikk()
        assertThat(stegHistorikk.map { it.steg() }).contains(StegType.BREV)
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
        assertThat(status).isEqualTo(Status.IVERKSETTES)

        dataSource.transaction {
            val behov = hentAvklaringsbehov(behandling.id, it)
            assertThat(behov.åpne()).allSatisfy { assertThat(it.definisjon.kode).isEqualTo(AvklaringsbehovKode.`9002`) }
        }

        dataSource.transaction { connection ->
            val brevbestilling = BrevbestillingRepositoryImpl(connection).hent(behandling.id)
                .first { it.typeBrev == TypeBrev.VEDTAK_AVSLAG }
            AvklaringsbehovHendelseHåndterer(
                AvklaringsbehovOrkestrator(
                    connection,
                    BehandlingHendelseServiceImpl(
                        FlytJobbRepository(connection), SakService(
                            SakRepositoryImpl(
                                connection
                            )
                        )
                    )
                ), AvklaringsbehovRepositoryImpl(connection), BehandlingRepositoryImpl(connection)
            ).håndtere(
                behandling.id,
                LøsAvklaringsbehovHendelse(
                    løsning = BrevbestillingLøsning(
                        LøsBrevbestillingDto(
                            behandlingReferanse = behandling.referanse.referanse,
                            bestillingReferanse = brevbestilling.referanse.brevbestillingReferanse,
                            status = BrevbestillingLøsningStatus.KLAR_FOR_EDITERING
                        )
                    ),
                    behandlingVersjon = behandling.versjon,
                    bruker = BREV_SYSTEMBRUKER
                )
            )
        }

        util.ventPåSvar(sak.id.toLong(), behandling.id.toLong())

        dataSource.transaction { connection ->
            val brevbestilling = BrevbestillingRepositoryImpl(connection).hent(behandling.id)
                .first { it.typeBrev == TypeBrev.VEDTAK_AVSLAG }
            AvklaringsbehovHendelseHåndterer(
                AvklaringsbehovOrkestrator(
                    connection,
                    BehandlingHendelseServiceImpl(
                        FlytJobbRepository(connection), SakService(
                            SakRepositoryImpl(
                                connection
                            )
                        )
                    )
                ), AvklaringsbehovRepositoryImpl(connection), BehandlingRepositoryImpl(connection)
            ).håndtere(
                behandling.id,
                LøsAvklaringsbehovHendelse(
                    løsning = SkrivBrevLøsning(brevbestillingReferanse = brevbestilling.referanse.brevbestillingReferanse),
                    behandlingVersjon = behandling.versjon,
                    bruker = Bruker("SAKSBEHANDLER")
                )
            )
        }

        util.ventPåSvar(sak.id.toLong(), behandling.id.toLong())

        dataSource.transaction {
            val behov = hentAvklaringsbehov(behandling.id, it)
            assertThat(behov.åpne()).isEmpty()
        }

        util.ventPåSvar()

        assertThat(hentBehandling(sak.id).status()).isEqualTo(Status.AVSLUTTET)
        assertThat(hendelser.last().behandlingStatus).isEqualTo(Status.AVSLUTTET)
    }

    private fun hentPerson(ident: Ident): Person {
        var person: Person? = null
        dataSource.transaction {
            person = PersonRepositoryImpl(it).finnEllerOpprett(listOf(ident))
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
                    SøknadV0(
                        student = SøknadStudentDto("NEI"), yrkesskade = "NEI", oppgitteBarn = null,
                        medlemskap = SøknadMedlemskapDto("JA", "JA", "JA", "NEI", null)
                    ),
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
        val frist = dataSource.transaction(readOnly = true) { connection ->
            val avklaringsbehovene = hentAvklaringsbehov(behandling.id, connection)

            if (avklaringsbehovene.erSattPåVent()) {
                val avklaringsbehov = avklaringsbehovene.hentÅpneVentebehov().first()
                avklaringsbehov.frist()
            } else {
                null
            }
        }
        assertThat(frist).isNotNull
        assertThat(frist).isNotNull
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
                    SøknadV0(
                        student = SøknadStudentDto("NEI"), yrkesskade = "NEI", oppgitteBarn = null,
                        medlemskap = SøknadMedlemskapDto("JA", "JA", "JA", "NEI", null)
                    ),
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

    @Test
    fun `Fjerner legeerklæring ventebehov ved mottak av avvist legeerklæring`() {
        val ident = ident()
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))

        // Oppretter vanlig søknad
        hendelsesMottak.håndtere(
            ident, DokumentMottattPersonHendelse(
                journalpost = JournalpostId("2"),
                mottattTidspunkt = LocalDateTime.now(),
                strukturertDokument = StrukturertDokument(
                    SøknadV0(
                        student = SøknadStudentDto("NEI"), yrkesskade = "NEI", oppgitteBarn = null,
                        medlemskap = SøknadMedlemskapDto("JA", "JA", "JA", "NEI", null)
                    ),
                ),
                periode = periode
            )
        )

        util.ventPåSvar()
        val sak = hentSak(ident, periode)
        var behandling = requireNotNull(hentBehandling(sak.id))

        // Validér avklaring
        dataSource.transaction { connection ->
            val avklaringsbehov = hentAvklaringsbehov(behandling.id, connection)
            assertThat(avklaringsbehov.alle()).anySatisfy { assertThat(it.erÅpent() && it.definisjon == Definisjon.AVKLAR_SYKDOM).isTrue() }
        }

        // Oppretter bestilling av legeerklæring
        dataSource.transaction { connection ->
            val avklaringsbehovene = hentAvklaringsbehov(behandling.id, connection)
            val sakService = SakService(SakRepositoryImpl(connection))
            val behandlingHendelseService = BehandlingHendelseServiceImpl(FlytJobbRepository((connection)), sakService)
            avklaringsbehovene.leggTil(
                definisjoner = listOf(Definisjon.BESTILL_LEGEERKLÆRING),
                funnetISteg = behandling.aktivtSteg(),
                grunn = ÅrsakTilSettPåVent.VENTER_PÅ_MEDISINSKE_OPPLYSNINGER,
                bruker = SYSTEMBRUKER
            )
            behandlingHendelseService.stoppet(behandling, avklaringsbehovene)
            util.ventPåSvar()

            assertThat(avklaringsbehovene.alle()).anySatisfy { assertThat(it.erÅpent() && it.definisjon == Definisjon.BESTILL_LEGEERKLÆRING).isTrue() }
        }

        // Validér avklaring
        dataSource.transaction { connection ->
            val avklaringsbehov = hentAvklaringsbehov(behandling.id, connection)
            assertThat(avklaringsbehov.åpne().all { it.definisjon == Definisjon.BESTILL_LEGEERKLÆRING })
        }

        // Send inn avvist legeerklæring
        val avvistLegeerklæringId = UUID.randomUUID().toString()
        dataSource.transaction { connection ->
            val flytJobbRepository = FlytJobbRepository(connection)
            flytJobbRepository.leggTil(
                HendelseMottattHåndteringJobbUtfører.nyJobb(
                    sakId = sak.id,
                    dokumentReferanse = InnsendingReferanse(
                        InnsendingReferanse.Type.AVVIST_LEGEERKLÆRING_ID,
                        avvistLegeerklæringId
                    ),
                    brevkategori = InnsendingType.LEGEERKLÆRING_AVVIST,
                    kanal = Kanal.DIGITAL,
                )
            )
        }
        util.ventPåSvar()

        // Validér avklaring
        dataSource.transaction { connection ->
            val avklaringsbehov = hentAvklaringsbehov(behandling.id, connection)
            val legeerklæringBestillingVenteBehov =
                avklaringsbehov.åpne().filter { it.definisjon == Definisjon.BESTILL_LEGEERKLÆRING }
            assertThat(legeerklæringBestillingVenteBehov.isEmpty()).isTrue()
        }
    }

    @Test
    fun `Fjerner legeerklæring ventebehov ved mottak av legeerklæring`() {
        val ident = ident()
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))

        // Oppretter vanlig søknad
        hendelsesMottak.håndtere(
            ident, DokumentMottattPersonHendelse(
                journalpost = JournalpostId("2"),
                mottattTidspunkt = LocalDateTime.now(),
                strukturertDokument = StrukturertDokument(
                    SøknadV0(
                        student = SøknadStudentDto("NEI"), yrkesskade = "NEI", oppgitteBarn = null,
                        medlemskap = SøknadMedlemskapDto("JA", "JA", "JA", "NEI", listOf())
                    ),
                ),
                periode = periode
            )
        )

        util.ventPåSvar()
        val sak = hentSak(ident, periode)
        var behandling = requireNotNull(hentBehandling(sak.id))

        // Validér avklaring
        dataSource.transaction { connection ->
            val avklaringsbehov = hentAvklaringsbehov(behandling.id, connection)
            assertThat(avklaringsbehov.alle()).anySatisfy { assertThat(it.erÅpent() && it.definisjon == Definisjon.AVKLAR_SYKDOM).isTrue() }
        }

        // Oppretter bestilling av legeerklæring
        dataSource.transaction { connection ->
            val avklaringsbehovene = hentAvklaringsbehov(behandling.id, connection)
            val sakService = SakService(SakRepositoryImpl(connection))
            val behandlingHendelseService = BehandlingHendelseServiceImpl(FlytJobbRepository((connection)), sakService)
            avklaringsbehovene.leggTil(
                definisjoner = listOf(Definisjon.BESTILL_LEGEERKLÆRING),
                funnetISteg = behandling.aktivtSteg(),
                grunn = ÅrsakTilSettPåVent.VENTER_PÅ_MEDISINSKE_OPPLYSNINGER,
                bruker = SYSTEMBRUKER
            )
            behandlingHendelseService.stoppet(behandling, avklaringsbehovene)
            util.ventPåSvar()

            assertThat(avklaringsbehovene.alle()).anySatisfy { assertThat(it.erÅpent() && it.definisjon == Definisjon.BESTILL_LEGEERKLÆRING).isTrue() }
        }
        util.ventPåSvar()

        // Validér avklaring
        dataSource.transaction { connection ->
            val avklaringsbehov = hentAvklaringsbehov(behandling.id, connection)
            assertThat(avklaringsbehov.åpne().all { it.definisjon == Definisjon.BESTILL_LEGEERKLÆRING })
        }

        // Mottar legeerklæring
        val journalpostId = UUID.randomUUID().toString()
        dataSource.transaction { connection ->
            val flytJobbRepository = FlytJobbRepository(connection)
            flytJobbRepository.leggTil(
                HendelseMottattHåndteringJobbUtfører.nyJobb(
                    sakId = sak.id,
                    dokumentReferanse = InnsendingReferanse(
                        InnsendingReferanse.Type.JOURNALPOST,
                        journalpostId
                    ),
                    brevkategori = InnsendingType.LEGEERKLÆRING,
                    kanal = Kanal.DIGITAL,
                )
            )
        }
        util.ventPåSvar()

        // Validér avklaring
        dataSource.transaction { connection ->
            val avklaringsbehov = hentAvklaringsbehov(behandling.id, connection)
            val legeerklæringBestillingVenteBehov =
                avklaringsbehov.åpne().filter { it.definisjon == Definisjon.BESTILL_LEGEERKLÆRING }
            assertThat(legeerklæringBestillingVenteBehov.isEmpty()).isTrue()
        }
    }

    @Test
    fun `Fjerner legeerklæring ventebehov ved mottak av dialogmelding`() {
        val ident = ident()
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))

        // Oppretter vanlig søknad
        hendelsesMottak.håndtere(
            ident, DokumentMottattPersonHendelse(
                journalpost = JournalpostId("2"),
                mottattTidspunkt = LocalDateTime.now(),
                strukturertDokument = StrukturertDokument(
                    SøknadV0(
                        student = SøknadStudentDto("NEI"), yrkesskade = "NEI", oppgitteBarn = null,
                        medlemskap = SøknadMedlemskapDto("JA", "JA", "JA", "NEI", null)
                    ),
                ),
                periode = periode
            )
        )

        util.ventPåSvar()
        val sak = hentSak(ident, periode)
        var behandling = requireNotNull(hentBehandling(sak.id))

        // Validér avklaring
        dataSource.transaction { connection ->
            val avklaringsbehov = hentAvklaringsbehov(behandling.id, connection)
            assertThat(avklaringsbehov.alle()).anySatisfy { assertThat(it.erÅpent() && it.definisjon == Definisjon.AVKLAR_SYKDOM).isTrue() }
        }

        // Oppretter bestilling av legeerklæring
        dataSource.transaction { connection ->
            val avklaringsbehovene = hentAvklaringsbehov(behandling.id, connection)
            val sakService = SakService(SakRepositoryImpl(connection))
            val behandlingHendelseService = BehandlingHendelseServiceImpl(FlytJobbRepository((connection)), sakService)
            avklaringsbehovene.leggTil(
                definisjoner = listOf(Definisjon.BESTILL_LEGEERKLÆRING),
                funnetISteg = behandling.aktivtSteg(),
                grunn = ÅrsakTilSettPåVent.VENTER_PÅ_MEDISINSKE_OPPLYSNINGER,
                bruker = SYSTEMBRUKER
            )
            behandlingHendelseService.stoppet(behandling, avklaringsbehovene)
            util.ventPåSvar()

            assertThat(avklaringsbehovene.alle()).anySatisfy { assertThat(it.erÅpent() && it.definisjon == Definisjon.BESTILL_LEGEERKLÆRING).isTrue() }
        }

        // Validér avklaring
        dataSource.transaction { connection ->
            val avklaringsbehov = hentAvklaringsbehov(behandling.id, connection)
            assertThat(avklaringsbehov.åpne().all { it.definisjon == Definisjon.BESTILL_LEGEERKLÆRING })
        }

        // Mottar dialogmelding
        val journalpostId = UUID.randomUUID().toString()
        dataSource.transaction { connection ->
            val flytJobbRepository = FlytJobbRepository(connection)
            flytJobbRepository.leggTil(
                HendelseMottattHåndteringJobbUtfører.nyJobb(
                    sakId = sak.id,
                    dokumentReferanse = InnsendingReferanse(
                        InnsendingReferanse.Type.JOURNALPOST,
                        journalpostId
                    ),
                    brevkategori = InnsendingType.DIALOGMELDING,
                    kanal = Kanal.DIGITAL,
                )
            )
        }
        util.ventPåSvar()

        // Validér avklaring
        dataSource.transaction { connection ->
            val avklaringsbehov = hentAvklaringsbehov(behandling.id, connection)
            val legeerklæringBestillingVenteBehov =
                avklaringsbehov.åpne().filter { it.definisjon == Definisjon.BESTILL_LEGEERKLÆRING }
            assertThat(legeerklæringBestillingVenteBehov.isEmpty()).isTrue()
        }
    }
}
