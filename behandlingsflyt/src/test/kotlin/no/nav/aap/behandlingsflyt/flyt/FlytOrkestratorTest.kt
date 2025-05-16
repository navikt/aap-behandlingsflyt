package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.SYSTEMBRUKER
import no.nav.aap.behandlingsflyt.behandling.Resultat
import no.nav.aap.behandlingsflyt.behandling.ResultatUtleder
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehov
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovHendelseHåndterer
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovOrkestrator
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.LøsAvklaringsbehovHendelse
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.BREV_SYSTEMBRUKER
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.vedtak.TotrinnsVurdering
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.vedtak.ÅrsakTilReturKode
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.ÅrsakTilSettPåVent
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarBistandsbehovLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarForutgåendeMedlemskapLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarLovvalgMedlemskapLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarOverstyrtForutgåendeMedlemskapLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarOverstyrtLovvalgMedlemskapLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarSamordningGraderingLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarSamordningUføreLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarStudentLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarSykdomLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarSykepengerErstatningLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarYrkesskadeLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklaringsbehovLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.BrevbestillingLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.FastsettBehandlendeEnhetLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.FastsettBeregningstidspunktLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.FastsettPåklagetBehandlingLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.FastsettYrkesskadeInntektLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.FatteVedtakLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.ForeslåVedtakLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.FritakMeldepliktLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.KvalitetssikringLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.RefusjonkravLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.SamordningVentPaVirkningstidspunktLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.SkrivBrevAvklaringsbehovLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.SkrivVedtaksbrevLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.TrekkSøknadLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.VurderFormkravLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.VurderKlageKontorLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.VurderKlageNayLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.VurderRettighetsperiodeLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.ÅrsakTilRetur
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.Brevbestilling
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.TypeBrev
import no.nav.aap.behandlingsflyt.behandling.samordning.Ytelse
import no.nav.aap.behandlingsflyt.behandling.vedtak.Vedtak
import no.nav.aap.behandlingsflyt.behandling.vilkår.medlemskap.EØSLand
import no.nav.aap.behandlingsflyt.drift.Driftfunksjoner
import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.BeregningsgrunnlagRepositoryImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.GrunnlagYrkesskade
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.uførevurdering.SamordningUføreVurderingDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.uførevurdering.SamordningUføreVurderingPeriodeDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Innvilgelsesårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepositoryImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.StrukturertDokument
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.behandlendeenhet.flate.BehandlendeEnhetLøsningDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.formkrav.flate.FormkravVurderingLøsningDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.Hjemmel
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.klagebehandling.nay.KlageInnstilling
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.klagebehandling.kontor.flate.KlagevurderingKontorLøsningDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.klagebehandling.nay.flate.KlagevurderingNayLøsningDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.påklagetbehandling.PåklagetVedtakType
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.påklagetbehandling.flate.PåklagetBehandlingVurderingLøsningDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.LovvalgVedSøknadsTidspunktDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.ManuellVurderingForForutgåendeMedlemskapDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.ManuellVurderingForLovvalgMedlemskapDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.MedlemskapVedSøknadsTidspunktDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektPerÅr
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningYrkeskaderBeløpVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningstidspunktVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.YrkesskadeBeløpVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.flate.BistandVurderingLøsningDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.flate.FritaksvurderingDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.refusjonkrav.RefusjonkravVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.rettighetsperiode.RettighetsperiodeVurderingDTO
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.samordning.SamordningVurderingData
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.samordning.VurderingerForSamordning
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentVurderingDTO
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykepengerGrunn
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykepengerVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.flate.SykdomsvurderingLøsningDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.flate.YrkesskadevurderingDto
import no.nav.aap.behandlingsflyt.flyt.FlytOrkestratorTest.Companion.util
import no.nav.aap.behandlingsflyt.flyt.internals.DokumentMottattPersonHendelse
import no.nav.aap.behandlingsflyt.flyt.internals.TestHendelsesMottak
import no.nav.aap.behandlingsflyt.help.assertTidslinje
import no.nav.aap.behandlingsflyt.hendelse.mottak.BehandlingSattPåVent
import no.nav.aap.behandlingsflyt.integrasjon.aaregisteret.AARegisterGateway
import no.nav.aap.behandlingsflyt.integrasjon.barn.PdlBarnGateway
import no.nav.aap.behandlingsflyt.integrasjon.brev.BrevGateway
import no.nav.aap.behandlingsflyt.integrasjon.dokumentinnhenting.DokumentinnhentingGatewayImpl
import no.nav.aap.behandlingsflyt.integrasjon.ident.PdlIdentGateway
import no.nav.aap.behandlingsflyt.integrasjon.ident.PdlPersoninfoBulkGateway
import no.nav.aap.behandlingsflyt.integrasjon.ident.PdlPersoninfoGateway
import no.nav.aap.behandlingsflyt.integrasjon.inntekt.InntektGatewayImpl
import no.nav.aap.behandlingsflyt.integrasjon.medlemsskap.MedlemskapGateway
import no.nav.aap.behandlingsflyt.integrasjon.meldekort.MeldekortGatewayImpl
import no.nav.aap.behandlingsflyt.integrasjon.oppgave.OppgavestyringGatewayImpl
import no.nav.aap.behandlingsflyt.integrasjon.samordning.AbakusForeldrepengerGateway
import no.nav.aap.behandlingsflyt.integrasjon.samordning.AbakusSykepengerGateway
import no.nav.aap.behandlingsflyt.integrasjon.samordning.TjenestePensjonGatewayImpl
import no.nav.aap.behandlingsflyt.integrasjon.statistikk.StatistikkGatewayImpl
import no.nav.aap.behandlingsflyt.integrasjon.ufore.UføreGateway
import no.nav.aap.behandlingsflyt.integrasjon.utbetaling.UtbetalingGatewayImpl
import no.nav.aap.behandlingsflyt.integrasjon.yrkesskade.YrkesskadeRegisterGatewayImpl
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.brevbestilling.BrevbestillingLøsningStatus
import no.nav.aap.behandlingsflyt.kontrakt.brevbestilling.LøsBrevbestillingDto
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.ArbeidIPeriodeV0
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.KlageV0
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.MeldekortV0
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.SøknadMedlemskapDto
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.SøknadStudentDto
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.SøknadV0
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.UtenlandsPeriodeDto
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.StoppetBehandling
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.prosessering.HendelseMottattHåndteringJobbUtfører
import no.nav.aap.behandlingsflyt.prosessering.ProsesseringsJobber
import no.nav.aap.behandlingsflyt.repository.avklaringsbehov.AvklaringsbehovRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.behandling.brev.bestilling.BrevbestillingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.behandling.tilkjentytelse.TilkjentYtelseRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.behandling.vedtak.VedtakRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.underveis.UnderveisRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.medlemskaplovvalg.MedlemskapArbeidInntektRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.postgresRepositoryRegistry
import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.StegTilstand
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Årsak
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekst
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.StegStatus
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.ÅrsakTilBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.PdlFolkeregisterPersonStatus
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.PdlFolkeregistermetadata
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.PdlStatsborgerskap
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.PersonStatus
import no.nav.aap.behandlingsflyt.test.FakeApiInternGateway
import no.nav.aap.behandlingsflyt.test.FakePersoner
import no.nav.aap.behandlingsflyt.test.FakeUnleash
import no.nav.aap.behandlingsflyt.test.Fakes
import no.nav.aap.behandlingsflyt.test.ident
import no.nav.aap.behandlingsflyt.test.modell.TestPerson
import no.nav.aap.behandlingsflyt.test.modell.TestYrkesskade
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.komponenter.gateway.GatewayRegistry
import no.nav.aap.komponenter.httpklient.auth.Bruker
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.Motor
import no.nav.aap.motor.testutil.TestUtil
import no.nav.aap.verdityper.dokument.JournalpostId
import no.nav.aap.verdityper.dokument.Kanal
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Year
import java.util.*

@Fakes
class FlytOrkestratorTest {

    companion object {
        private val dataSource = InitTestDatabase.freshDatabase()
        private val motor =
            Motor(dataSource, 8, jobber = ProsesseringsJobber.alle(), repositoryRegistry = postgresRepositoryRegistry)
        private val hendelsesMottak = TestHendelsesMottak(dataSource)
        private val util =
            TestUtil(dataSource, ProsesseringsJobber.alle().filter { it.cron != null }.map { it.type })

        @BeforeAll
        @JvmStatic
        internal fun beforeAll() {
            System.setProperty("NAIS_CLUSTER_NAME", "LOCAL")
            GatewayRegistry
                .register<PdlBarnGateway>()
                .register<PdlIdentGateway>()
                .register<PdlPersoninfoBulkGateway>()
                .register<PdlPersoninfoGateway>()
                .register<AbakusSykepengerGateway>()
                .register<AbakusForeldrepengerGateway>()
                .register<DokumentinnhentingGatewayImpl>()
                .register<MedlemskapGateway>()
                .register<FakeApiInternGateway>()
                .register<UtbetalingGatewayImpl>()
                .register<AARegisterGateway>()
                .register<StatistikkGatewayImpl>()
                .register<InntektGatewayImpl>()
                .register<BrevGateway>()
                .register<OppgavestyringGatewayImpl>()
                .register<UføreGateway>()
                .register<YrkesskadeRegisterGatewayImpl>()
                .register<MeldekortGatewayImpl>()
                .register<TjenestePensjonGatewayImpl>()
                .register<FakeUnleash>()
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
        val fom = LocalDate.now().minusMonths(3)
        val periode = Periode(fom, fom.plusYears(3))

        // Simulerer et svar fra YS-løsning om at det finnes en yrkesskade
        val person = TestPerson(
            fødselsdato = Fødselsdato(LocalDate.now().minusYears(25)),
            yrkesskade = listOf(TestYrkesskade()),
            uføre = Prosent(50),
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
        FakePersoner.leggTil(person)

        val ident = person.aktivIdent()

        // Sender inn en søknad
        var behandling = sendInnDokument(
            ident, DokumentMottattPersonHendelse(
                journalpost = JournalpostId("20"),
                mottattTidspunkt = LocalDateTime.now().minusMonths(3),
                strukturertDokument = StrukturertDokument(
                    SøknadV0(
                        student = SøknadStudentDto("NEI"),
                        yrkesskade = "NEI",
                        oppgitteBarn = null,
                        medlemskap = SøknadMedlemskapDto("JA", "NEI", "NEI", "NEI", null)
                    ),
                ),
                periode = periode
            )
        )
        val sak = hentSak(ident, periode)
        assertThat(behandling.typeBehandling()).isEqualTo(TypeBehandling.Førstegangsbehandling)

        var alleAvklaringsbehov = hentAlleAvklaringsbehov(behandling)

        assertThat(alleAvklaringsbehov).isNotEmpty()
        assertThat(behandling.status()).isEqualTo(Status.UTREDES)

        behandling = løsSykdom(behandling)
        behandling = løsAvklaringsBehov(
            behandling, AvklarBistandsbehovLøsning(
                bistandsVurdering = BistandVurderingLøsningDto(
                    begrunnelse = "Trenger hjelp fra nav",
                    erBehovForAktivBehandling = true,
                    erBehovForArbeidsrettetTiltak = false,
                    erBehovForAnnenOppfølging = null,
                    skalVurdereAapIOvergangTilUføre = null,
                    skalVurdereAapIOvergangTilArbeid = null,
                    overgangBegrunnelse = null
                ),
            )
        )

        løsAvklaringsBehov(
            behandling,
            RefusjonkravLøsning(
                RefusjonkravVurdering(
                    harKrav = true,
                    fom = LocalDate.now(),
                    tom = null
                )
            )
        )

        // Sender inn en søknad
        behandling = sendInnDokument(
            ident, DokumentMottattPersonHendelse(
                journalpost = JournalpostId("22"),
                mottattTidspunkt = LocalDateTime.now(),
                strukturertDokument = StrukturertDokument(
                    MeldekortV0(
                        harDuArbeidet = false,
                        timerArbeidPerPeriode = listOf(
                            ArbeidIPeriodeV0(
                                fraOgMedDato = LocalDate.now().minusMonths(3),
                                tilOgMedDato = LocalDate.now().plusMonths(3),
                                timerArbeid = 0.0,
                            )
                        )
                    ),
                ),
                periode = periode
            )
        )

        behandling = kvalitetssikre(behandling)

        behandling = løsAvklaringsBehov(
            behandling,
            AvklarYrkesskadeLøsning(
                yrkesskadesvurdering = YrkesskadevurderingDto(
                    begrunnelse = "Ikke årsakssammenheng",
                    relevanteSaker = listOf(),
                    andelAvNedsettelsen = null,
                    erÅrsakssammenheng = false
                )
            ),
        )

        behandling = løsAvklaringsBehov(
            behandling,
            FastsettBeregningstidspunktLøsning(
                beregningVurdering = BeregningstidspunktVurdering(
                    begrunnelse = "Trenger hjelp fra Nav",
                    nedsattArbeidsevneDato = LocalDate.now(),
                    ytterligereNedsattArbeidsevneDato = null,
                    ytterligereNedsattBegrunnelse = null
                ),
            ),
        )

        behandling = løsAvklaringsBehov(
            behandling, AvklarSamordningUføreLøsning(
                samordningUføreVurdering = SamordningUføreVurderingDto(
                    begrunnelse = "Samordnet med uføre",
                    vurderingPerioder = listOf(
                        SamordningUføreVurderingPeriodeDto(
                            virkningstidspunkt = sak.rettighetsperiode.fom, uføregradTilSamordning = 45
                        )
                    )
                )
            )
        )

        // Saken står til en-trinnskontroll hos saksbehandler klar for å bli sendt til beslutter
        alleAvklaringsbehov = hentAlleAvklaringsbehov(behandling)
        assertThat(alleAvklaringsbehov).anySatisfy { assertThat(it.erÅpent() && it.definisjon == Definisjon.FORESLÅ_VEDTAK).isTrue() }
        assertThat(behandling.status()).isEqualTo(Status.UTREDES)

        behandling = løsAvklaringsBehov(behandling, ForeslåVedtakLøsning())

        behandling = løsAvklaringsBehov(
            behandling, FatteVedtakLøsning(
                hentAlleAvklaringsbehov(behandling)
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
                    }), Bruker("BESLUTTER")
        )

        løsSykdom(behandling)

        behandling = løsAvklaringsBehov(
            behandling,
            AvklarBistandsbehovLøsning(
                bistandsVurdering = BistandVurderingLøsningDto(
                    begrunnelse = "Trenger hjelp fra nav",
                    erBehovForAktivBehandling = true,
                    erBehovForArbeidsrettetTiltak = false,
                    erBehovForAnnenOppfølging = null,
                    skalVurdereAapIOvergangTilUføre = null,
                    skalVurdereAapIOvergangTilArbeid = null,
                    overgangBegrunnelse = null
                ),
            ),
        )

        behandling = løsAvklaringsBehov(
            behandling,
            RefusjonkravLøsning(
                RefusjonkravVurdering(
                    harKrav = true,
                    fom = LocalDate.now(),
                    tom = null
                )
            )
        )

        behandling = løsAvklaringsBehov(
            behandling,
            AvklarYrkesskadeLøsning(
                yrkesskadesvurdering = YrkesskadevurderingDto(
                    begrunnelse = "Ikke årsakssammenheng",
                    relevanteSaker = listOf(),
                    andelAvNedsettelsen = null,
                    erÅrsakssammenheng = false
                )
            )
        )

        behandling = løsAvklaringsBehov(
            behandling, FastsettBeregningstidspunktLøsning(
                beregningVurdering = BeregningstidspunktVurdering(
                    begrunnelse = "Trenger hjelp fra Nav",
                    nedsattArbeidsevneDato = LocalDate.now(),
                    ytterligereNedsattArbeidsevneDato = null,
                    ytterligereNedsattBegrunnelse = null
                ),
            )
        )

        behandling = løsAvklaringsBehov(
            behandling, AvklarSamordningUføreLøsning(
                samordningUføreVurdering = SamordningUføreVurderingDto(
                    begrunnelse = "Samordnet med uføre",
                    vurderingPerioder = listOf(
                        SamordningUføreVurderingPeriodeDto(
                            virkningstidspunkt = sak.rettighetsperiode.fom, uføregradTilSamordning = 45
                        )
                    )
                )
            )
        )

        // Saken er tilbake til en-trinnskontroll hos saksbehandler klar for å bli sendt til beslutter
        alleAvklaringsbehov = hentAlleAvklaringsbehov(behandling)
        assertThat(alleAvklaringsbehov).anySatisfy { assertTrue(it.erÅpent() && it.definisjon == Definisjon.FORESLÅ_VEDTAK) }
        assertThat(behandling.status()).isEqualTo(Status.UTREDES)

        behandling = løsAvklaringsBehov(behandling, ForeslåVedtakLøsning())

        // Saken står til To-trinnskontroll hos beslutter
        alleAvklaringsbehov = hentAlleAvklaringsbehov(behandling)
        assertThat(alleAvklaringsbehov).anySatisfy { assertTrue(it.erÅpent() && it.definisjon == Definisjon.FATTE_VEDTAK) }
        assertThat(behandling.status()).isEqualTo(Status.UTREDES)

        behandling = fattVedtak(behandling)

        assertThat(behandling.status()).isEqualTo(Status.IVERKSETTES)

        // Skal feile dersom man prøver å sende til beslutter etter at vedtaket er fattet
        val avklaringsbehovFeil = assertThrows<UgyldigForespørselException> {
            løsAvklaringsBehov(behandling, ForeslåVedtakLøsning())
        }
        assertThat(avklaringsbehovFeil.message).contains("Forsøker å løse avklaringsbehov FORESLÅ_VEDTAK(kode='5098') som er definert i et steg før nåværende steg[BREV]")
        val vedtak = hentVedtak(behandling.id)
        assertThat(vedtak.vedtakstidspunkt.toLocalDate()).isToday

        alleAvklaringsbehov = hentAlleAvklaringsbehov(behandling)
        // Det er bestilt vedtaksbrev
        assertThat(alleAvklaringsbehov).anySatisfy { assertTrue(it.erÅpent() && it.definisjon == Definisjon.BESTILL_BREV) }

        var brevbestilling = hentBrevAvType(behandling, TypeBrev.VEDTAK_INNVILGELSE)

        behandling =
            løsAvklaringsBehov(behandling, brevbestillingLøsning(behandling, brevbestilling), BREV_SYSTEMBRUKER)
        brevbestilling = hentBrevAvType(behandling, TypeBrev.VEDTAK_INNVILGELSE)
        // Brevet er klar for forhåndsvisning og editering
        assertThat(brevbestilling.status).isEqualTo(
            no.nav.aap.behandlingsflyt.behandling.brev.bestilling.Status.FORHÅNDSVISNING_KLAR
        )

        alleAvklaringsbehov = hentAlleAvklaringsbehov(behandling)
        // Venter på at brevet skal fullføres
        assertThat(alleAvklaringsbehov).anySatisfy { assertTrue(it.erÅpent() && it.definisjon == Definisjon.SKRIV_VEDTAKSBREV) }

        brevbestilling = hentBrevAvType(behandling, TypeBrev.VEDTAK_INNVILGELSE)

        behandling =
            løsAvklaringsBehov(behandling, vedtaksbrevLøsning(brevbestilling.referanse.brevbestillingReferanse))

        brevbestilling = hentBrevAvType(behandling, TypeBrev.VEDTAK_INNVILGELSE)
        // Brevet er fullført
        assertThat(brevbestilling.status).isEqualTo(
            no.nav.aap.behandlingsflyt.behandling.brev.bestilling.Status.FULLFØRT
        )
        assertThat(behandling.status()).isEqualTo(Status.AVSLUTTET)

        // Henter vurder alder-vilkår
        // Assert utfall
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
        assertThat(underveisGrunnlag.perioder.any { it.arbeidsgradering.gradering.prosentverdi() > 0 }).isTrue()

        // Saken er avsluttet, så det skal ikke være flere åpne avklaringsbehov
        val åpneAvklaringsbehov = hentÅpneAvklaringsbehov(behandling.id)
        assertThat(åpneAvklaringsbehov).isEmpty()

        behandling = sendInnDokument(
            ident, DokumentMottattPersonHendelse(
                journalpost = JournalpostId("29"),
                mottattTidspunkt = LocalDateTime.now().minusMonths(3),
                strukturertDokument = StrukturertDokument(
                    SøknadV0(
                        student = SøknadStudentDto("NEI"),
                        yrkesskade = "NEI",
                        oppgitteBarn = null,
                        medlemskap = SøknadMedlemskapDto("JA", "NEI", "NEI", "NEI", null)
                    ),
                ),
                periode = periode
            )
        )

        assertThat(behandling.typeBehandling()).isEqualTo(TypeBehandling.Revurdering)
        assertThat(behandling.status()).isEqualTo(Status.UTREDES)

        behandling = løsAvklaringsBehov(
            behandling,
            AvklarSykdomLøsning(
                sykdomsvurdering = SykdomsvurderingLøsningDto(
                    begrunnelse = "Er syk nok",
                    dokumenterBruktIVurdering = listOf(JournalpostId("1349532")),
                    harSkadeSykdomEllerLyte = true,
                    erSkadeSykdomEllerLyteVesentligdel = true,
                    erNedsettelseIArbeidsevneMerEnnHalvparten = true,
                    erNedsettelseIArbeidsevneAvEnVissVarighet = true,
                    erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = null,
                    erArbeidsevnenNedsatt = true,
                    yrkesskadeBegrunnelse = null,
                    vurderingenGjelderFra = null,
                )
            ),
        )
        alleAvklaringsbehov = hentAlleAvklaringsbehov(behandling)

        assertThat(alleAvklaringsbehov).anySatisfy { assertTrue(it.erÅpent() && it.definisjon == Definisjon.AVKLAR_BISTANDSBEHOV) }
        assertThat(behandling.status()).isEqualTo(Status.UTREDES)
    }

    @Test
    fun `trukket søknad blokkerer nye ytelsesbehandlinger`() {
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))

        val person = TestPerson(
            fødselsdato = Fødselsdato(LocalDate.now().minusYears(20)),
        )
        FakePersoner.leggTil(person)

        val ident = person.aktivIdent()

        // Sender inn en søknad
        var behandling = sendInnDokument(
            ident, DokumentMottattPersonHendelse(
                journalpost = JournalpostId("10"),
                mottattTidspunkt = LocalDateTime.now(),
                strukturertDokument = StrukturertDokument(
                    SøknadV0(
                        student = SøknadStudentDto("NEI"),
                        yrkesskade = "NEI",
                        oppgitteBarn = null,
                        medlemskap = SøknadMedlemskapDto("JA", "NEI", "NEI", "NEI", null)
                    ),
                ),
                periode = periode
            )
        )
        løsSykdom(behandling)
        leggTilÅrsakForBehandling(behandling, listOf(Årsak(ÅrsakTilBehandling.SØKNAD_TRUKKET)))
        assertThat(hentAlleAvklaringsbehov(behandling)).anySatisfy { avklaringsbehov -> assertThat(avklaringsbehov.erÅpent() && avklaringsbehov.definisjon == Definisjon.VURDER_TREKK_AV_SØKNAD).isTrue() }

        behandling = løsAvklaringsBehov(
            behandling,
            TrekkSøknadLøsning(begrunnelse = "trekker søknaden"),
        )

        assertThat(hentAlleAvklaringsbehov(behandling)).anySatisfy { avklaringsbehov -> assertThat(avklaringsbehov.erAvsluttet()).isTrue() }
        assertThat(behandling.status()).isEqualTo(Status.AVSLUTTET)

        behandling = sendInnDokument(
            ident, DokumentMottattPersonHendelse(
                journalpost = JournalpostId("10"),
                mottattTidspunkt = LocalDateTime.now(),
                strukturertDokument = StrukturertDokument(
                    SøknadV0(
                        student = SøknadStudentDto("NEI"),
                        yrkesskade = "NEI",
                        oppgitteBarn = null,
                        medlemskap = SøknadMedlemskapDto("JA", "NEI", "NEI", "NEI", null)
                    ),
                ),
                periode = periode
            )
        )

        assertThat(behandling.forrigeBehandlingId).isNull()
        assertThat(behandling.status()).isEqualTo(Status.AVSLUTTET)
    }

    @Test
    fun `skal avklare yrkesskade hvis det finnes spor av yrkesskade - yrkesskade har årsakssammenheng`() {
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))

        val person = TestPerson(
            fødselsdato = Fødselsdato(LocalDate.now().minusYears(20)),
            yrkesskade = listOf(TestYrkesskade()),
        )
        FakePersoner.leggTil(person)

        val ident = person.aktivIdent()

        // Sender inn en søknad
        var behandling = sendInnDokument(
            ident, DokumentMottattPersonHendelse(
                journalpost = JournalpostId("10"),
                mottattTidspunkt = LocalDateTime.now(),
                strukturertDokument = StrukturertDokument(
                    SøknadV0(
                        student = SøknadStudentDto("NEI"),
                        yrkesskade = "NEI",
                        oppgitteBarn = null,
                        medlemskap = SøknadMedlemskapDto("JA", "NEI", "NEI", "NEI", null)
                    ),
                ),
                periode = periode
            )
        )
        assertThat(behandling.typeBehandling()).isEqualTo(TypeBehandling.Førstegangsbehandling)

        var alleAvklaringsbehov = hentAlleAvklaringsbehov(behandling)
        assertThat(alleAvklaringsbehov).isNotEmpty()
        assertThat(behandling.status()).isEqualTo(Status.UTREDES)

        løsSykdom(behandling)

        behandling = løsAvklaringsBehov(
            behandling,
            AvklarBistandsbehovLøsning(
                bistandsVurdering = BistandVurderingLøsningDto(
                    begrunnelse = "Trenger hjelp fra nav",
                    erBehovForAktivBehandling = true,
                    erBehovForArbeidsrettetTiltak = false,
                    erBehovForAnnenOppfølging = null,
                    skalVurdereAapIOvergangTilUføre = null,
                    skalVurdereAapIOvergangTilArbeid = null,
                    overgangBegrunnelse = null
                ),
            ),
        )

        behandling = løsAvklaringsBehov(
            behandling,
            RefusjonkravLøsning(
                RefusjonkravVurdering(
                    harKrav = true,
                    fom = LocalDate.now(),
                    tom = null
                )
            )
        )

        behandling = kvalitetssikre(behandling)

        behandling = løsAvklaringsBehov(
            behandling,
            AvklarYrkesskadeLøsning(
                yrkesskadesvurdering = YrkesskadevurderingDto(
                    begrunnelse = "Veldig relevante",
                    relevanteSaker = person.yrkesskade.map { it.saksreferanse },
                    andelAvNedsettelsen = 50,
                    erÅrsakssammenheng = true
                )
            ),
        )

        behandling = løsAvklaringsBehov(
            behandling,
            FastsettBeregningstidspunktLøsning(
                beregningVurdering = BeregningstidspunktVurdering(
                    begrunnelse = "Trenger hjelp fra Nav",
                    nedsattArbeidsevneDato = LocalDate.now(),
                    ytterligereNedsattArbeidsevneDato = null,
                    ytterligereNedsattBegrunnelse = null
                ),
            ),
        )

        behandling = løsAvklaringsBehov(
            behandling,
            FastsettYrkesskadeInntektLøsning(
                yrkesskadeInntektVurdering = BeregningYrkeskaderBeløpVurdering(
                    vurderinger = person.yrkesskade.map {
                        YrkesskadeBeløpVurdering(
                            antattÅrligInntekt = Beløp(5000000),
                            referanse = it.saksreferanse,
                            begrunnelse = "Trenger hjelp fra Nav"
                        )
                    },
                )
            )
        )

        // Saken står til en-trinnskontroll hos saksbehandler klar for å bli sendt til beslutter
        alleAvklaringsbehov = hentAlleAvklaringsbehov(behandling)
        assertThat(alleAvklaringsbehov).anySatisfy { avklaringsbehov -> assertThat(avklaringsbehov.erÅpent() && avklaringsbehov.definisjon == Definisjon.FORESLÅ_VEDTAK).isTrue() }
        assertThat(behandling.status()).isEqualTo(Status.UTREDES)

        behandling = løsAvklaringsBehov(behandling, ForeslåVedtakLøsning())

        // Saken står til To-trinnskontroll hos beslutter
        alleAvklaringsbehov = hentAlleAvklaringsbehov(behandling)
        assertThat(alleAvklaringsbehov).anySatisfy { assertThat(it.erÅpent() && it.definisjon == Definisjon.FATTE_VEDTAK).isTrue() }
        assertThat(behandling.status()).isEqualTo(Status.UTREDES)

        behandling = fattVedtak(behandling)

        alleAvklaringsbehov = hentAlleAvklaringsbehov(behandling)
        // Det er bestilt vedtaksbrev
        assertThat(alleAvklaringsbehov).anySatisfy { assertTrue(it.erÅpent() && it.definisjon == Definisjon.BESTILL_BREV) }
        assertThat(behandling.status()).isEqualTo(Status.IVERKSETTES)

        var brevBestilling = hentBrevAvType(behandling, TypeBrev.VEDTAK_INNVILGELSE)

        behandling = løsAvklaringsBehov(
            behandling, brevbestillingLøsning(behandling, brevBestilling), BREV_SYSTEMBRUKER
        )

        brevBestilling = hentBrevAvType(behandling, TypeBrev.VEDTAK_INNVILGELSE)

        // Brevet er klar for forhåndsvisning og editering
        assertThat(brevBestilling.status).isEqualTo(no.nav.aap.behandlingsflyt.behandling.brev.bestilling.Status.FORHÅNDSVISNING_KLAR)

        alleAvklaringsbehov = hentAlleAvklaringsbehov(behandling)
        // Venter på at brevet skal fullføres
        assertThat(alleAvklaringsbehov).anySatisfy { assertTrue(it.erÅpent() && it.definisjon == Definisjon.SKRIV_VEDTAKSBREV) }

        brevBestilling = hentBrevAvType(behandling, TypeBrev.VEDTAK_INNVILGELSE)
        behandling =
            løsAvklaringsBehov(behandling, vedtaksbrevLøsning(brevBestilling.referanse.brevbestillingReferanse))

        brevBestilling = hentBrevAvType(behandling, TypeBrev.VEDTAK_INNVILGELSE)
        // Brevet er fullført
        assertThat(brevBestilling.status).isEqualTo(no.nav.aap.behandlingsflyt.behandling.brev.bestilling.Status.FULLFØRT)
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
    fun `ikke sykdom viss varighet, men skal få innvilget 11-13 sykepengererstatning`() {
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))
        val person = TestPerson(
            fødselsdato = Fødselsdato(LocalDate.now().minusYears(25)),
        )
        FakePersoner.leggTil(person)
        val ident = person.aktivIdent()

        // Sender inn en søknad
        var behandling = sendInnDokument(
            ident, DokumentMottattPersonHendelse(
                journalpost = JournalpostId("10"),
                mottattTidspunkt = LocalDateTime.now(),
                strukturertDokument = StrukturertDokument(
                    SøknadV0(
                        student = SøknadStudentDto("NEI"),
                        yrkesskade = "NEI",
                        oppgitteBarn = null,
                        medlemskap = SøknadMedlemskapDto("JA", "NEI", "NEI", "NEI", null)
                    ),
                ),
                periode = periode
            )
        )

        assertThat(behandling.status()).isEqualTo(Status.UTREDES)

        behandling = løsAvklaringsBehov(
            behandling,
            AvklarSykdomLøsning(
                sykdomsvurdering = SykdomsvurderingLøsningDto(
                    begrunnelse = "Er syk nok",
                    dokumenterBruktIVurdering = listOf(JournalpostId("123123")),
                    harSkadeSykdomEllerLyte = true,
                    erSkadeSykdomEllerLyteVesentligdel = true,
                    erNedsettelseIArbeidsevneMerEnnHalvparten = true,
                    // Nei på denne gir mulighet til å innvilge på 11-13
                    erNedsettelseIArbeidsevneAvEnVissVarighet = false,
                    erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = null,
                    erArbeidsevnenNedsatt = true,
                    yrkesskadeBegrunnelse = null,
                    vurderingenGjelderFra = null,
                )
            ),
        )

        behandling = løsAvklaringsBehov(
            behandling,
            RefusjonkravLøsning(
                RefusjonkravVurdering(
                    harKrav = false,
                    fom = LocalDate.now(),
                    tom = null
                )
            )
        )

        behandling = kvalitetssikre(behandling)

        val åpneAvklaringsbehov = hentÅpneAvklaringsbehov(behandling.id)
        assertThat(åpneAvklaringsbehov).anySatisfy { assertThat(it.definisjon).isEqualTo(Definisjon.AVKLAR_SYKEPENGEERSTATNING) }

        behandling = løsAvklaringsBehov(
            behandling, AvklarSykepengerErstatningLøsning(
                sykepengeerstatningVurdering = SykepengerVurdering(
                    begrunnelse = "...",
                    dokumenterBruktIVurdering = listOf(),
                    harRettPå = true,
                    grunn = SykepengerGrunn.SYKEPENGER_IGJEN_ARBEIDSUFOR
                ),
            )
        )


        behandling = løsAvklaringsBehov(
            behandling,
            FastsettBeregningstidspunktLøsning(
                beregningVurdering = BeregningstidspunktVurdering(
                    begrunnelse = "Trenger hjelp fra Nav",
                    nedsattArbeidsevneDato = LocalDate.now(),
                    ytterligereNedsattArbeidsevneDato = null,
                    ytterligereNedsattBegrunnelse = null
                ),
            ),
        )

        // Saken står til en-trinnskontroll hos saksbehandler klar for å bli sendt til beslutter
        var alleAvklaringsbehov = hentAlleAvklaringsbehov(behandling)
        assertThat(alleAvklaringsbehov).anySatisfy { avklaringsbehov -> assertThat(avklaringsbehov.erÅpent() && avklaringsbehov.definisjon == Definisjon.FORESLÅ_VEDTAK).isTrue() }
        assertThat(behandling.status()).isEqualTo(Status.UTREDES)

        behandling = løsAvklaringsBehov(behandling, ForeslåVedtakLøsning())

        // Saken står til To-trinnskontroll hos beslutter
        alleAvklaringsbehov = hentAlleAvklaringsbehov(behandling)
        assertThat(alleAvklaringsbehov).anySatisfy { assertThat(it.erÅpent() && it.definisjon == Definisjon.FATTE_VEDTAK).isTrue() }
        assertThat(behandling.status()).isEqualTo(Status.UTREDES)

        behandling = fattVedtak(behandling)

        assertThat(behandling.status()).isEqualTo(Status.IVERKSETTES)

        var resultat =
            dataSource.transaction { ResultatUtleder(postgresRepositoryRegistry.provider(it)).utledResultat(behandling.id) }
        assertThat(resultat).isEqualTo(Resultat.INNVILGELSE)

        var brevBestilling = hentBrevAvType(behandling, TypeBrev.VEDTAK_INNVILGELSE)

        behandling = løsAvklaringsBehov(
            behandling, brevbestillingLøsning(behandling, brevBestilling), BREV_SYSTEMBRUKER
        )


        brevBestilling = hentBrevAvType(behandling, TypeBrev.VEDTAK_INNVILGELSE)
        behandling =
            løsAvklaringsBehov(behandling, vedtaksbrevLøsning(brevBestilling.referanse.brevbestillingReferanse))

        assertThat(behandling.status()).isEqualTo(Status.AVSLUTTET)

        val vilkårsresultat = hentVilkårsresultat(behandlingId = behandling.id)
        val sykdomsvilkåret = vilkårsresultat.finnVilkår(Vilkårtype.SYKDOMSVILKÅRET)

        assertThat(sykdomsvilkåret.vilkårsperioder()).hasSize(1)
            .first()
            .extracting(Vilkårsperiode::erOppfylt, Vilkårsperiode::innvilgelsesårsak)
            .containsExactly(true, Innvilgelsesårsak.SYKEPENGEERSTATNING)

        resultat =
            dataSource.transaction { ResultatUtleder(postgresRepositoryRegistry.provider(it)).utledResultat(behandling.id) }
        assertThat(resultat).isEqualTo(Resultat.INNVILGELSE)

        assertTidslinje(
            vilkårsresultat.rettighetstypeTidslinje(),
            periode to {
                assertThat(it).isEqualTo(RettighetsType.SYKEPENGEERSTATNING)
            })
    }

    @Test
    fun `avslag på 11-6 er også inngang til 11-13`() {
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))
        val person = TestPerson(
            fødselsdato = Fødselsdato(LocalDate.now().minusYears(25)),
        )
        FakePersoner.leggTil(person)
        val ident = person.aktivIdent()

        // Sender inn en søknad
        var behandling = sendInnDokument(
            ident, DokumentMottattPersonHendelse(
                journalpost = JournalpostId("10"),
                mottattTidspunkt = LocalDateTime.now(),
                strukturertDokument = StrukturertDokument(
                    SøknadV0(
                        student = SøknadStudentDto("NEI"),
                        yrkesskade = "NEI",
                        oppgitteBarn = null,
                        medlemskap = SøknadMedlemskapDto("JA", "NEI", "NEI", "NEI", null)
                    ),
                ),
                periode = periode
            )
        )

        assertThat(behandling.status()).isEqualTo(Status.UTREDES)

        behandling = løsAvklaringsBehov(
            behandling,
            AvklarSykdomLøsning(
                sykdomsvurdering = SykdomsvurderingLøsningDto(
                    begrunnelse = "Er syk nok",
                    dokumenterBruktIVurdering = listOf(JournalpostId("123123")),
                    harSkadeSykdomEllerLyte = true,
                    erSkadeSykdomEllerLyteVesentligdel = true,
                    erNedsettelseIArbeidsevneMerEnnHalvparten = true,
                    erNedsettelseIArbeidsevneAvEnVissVarighet = true,
                    erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = null,
                    erArbeidsevnenNedsatt = true,
                    yrkesskadeBegrunnelse = null,
                    vurderingenGjelderFra = null,
                )
            ),
        )

        // Nei på 11-6
        behandling = løsAvklaringsBehov(
            behandling,
            AvklarBistandsbehovLøsning(
                bistandsVurdering = BistandVurderingLøsningDto(
                    begrunnelse = "Trenger  hjelp fra nav",
                    erBehovForAktivBehandling = false,
                    erBehovForArbeidsrettetTiltak = false,
                    erBehovForAnnenOppfølging = false,
                    skalVurdereAapIOvergangTilUføre = null,
                    skalVurdereAapIOvergangTilArbeid = null,
                    overgangBegrunnelse = null
                ),
            ),
        )

        behandling = kvalitetssikre(behandling)

        val åpneAvklaringsbehov = hentÅpneAvklaringsbehov(behandling.id)
        assertThat(åpneAvklaringsbehov).anySatisfy { assertThat(it.definisjon).isEqualTo(Definisjon.AVKLAR_SYKEPENGEERSTATNING) }

        behandling = løsAvklaringsBehov(
            behandling, AvklarSykepengerErstatningLøsning(
                sykepengeerstatningVurdering = SykepengerVurdering(
                    begrunnelse = "...",
                    dokumenterBruktIVurdering = listOf(),
                    harRettPå = true,
                    grunn = SykepengerGrunn.SYKEPENGER_IGJEN_ARBEIDSUFOR
                ),
            )
        )

        behandling = løsAvklaringsBehov(
            behandling,
            FastsettBeregningstidspunktLøsning(
                beregningVurdering = BeregningstidspunktVurdering(
                    begrunnelse = "Trenger hjelp fra Nav",
                    nedsattArbeidsevneDato = LocalDate.now(),
                    ytterligereNedsattArbeidsevneDato = null,
                    ytterligereNedsattBegrunnelse = null
                ),
            ),
        )

        // Saken står til en-trinnskontroll hos saksbehandler klar for å bli sendt til beslutter
        var alleAvklaringsbehov = hentAlleAvklaringsbehov(behandling)
        assertThat(alleAvklaringsbehov).anySatisfy { avklaringsbehov -> assertThat(avklaringsbehov.erÅpent() && avklaringsbehov.definisjon == Definisjon.FORESLÅ_VEDTAK).isTrue() }
        assertThat(behandling.status()).isEqualTo(Status.UTREDES)

        behandling = løsAvklaringsBehov(behandling, ForeslåVedtakLøsning())

        // Saken står til To-trinnskontroll hos beslutter
        alleAvklaringsbehov = hentAlleAvklaringsbehov(behandling)
        assertThat(alleAvklaringsbehov).anySatisfy { assertThat(it.erÅpent() && it.definisjon == Definisjon.FATTE_VEDTAK).isTrue() }
        assertThat(behandling.status()).isEqualTo(Status.UTREDES)

        behandling = fattVedtak(behandling)

        assertThat(behandling.status()).isEqualTo(Status.IVERKSETTES)

        var resultat =
            dataSource.transaction { ResultatUtleder(postgresRepositoryRegistry.provider(it)).utledResultat(behandling.id) }
        assertThat(resultat).isEqualTo(Resultat.INNVILGELSE)

        var brevBestilling = hentBrevAvType(behandling, TypeBrev.VEDTAK_INNVILGELSE)

        behandling = løsAvklaringsBehov(
            behandling, brevbestillingLøsning(behandling, brevBestilling), BREV_SYSTEMBRUKER
        )


        brevBestilling = hentBrevAvType(behandling, TypeBrev.VEDTAK_INNVILGELSE)
        behandling =
            løsAvklaringsBehov(behandling, vedtaksbrevLøsning(brevBestilling.referanse.brevbestillingReferanse))

        assertThat(behandling.status()).isEqualTo(Status.AVSLUTTET)

        val vilkårsresultat = hentVilkårsresultat(behandlingId = behandling.id)
        val sykdomsvilkåret = vilkårsresultat.finnVilkår(Vilkårtype.SYKDOMSVILKÅRET)

        // Sjekker at sykdomsvilkåret ble oppfylt med innvilgelsesårsak satt til 11-13.
        assertThat(sykdomsvilkåret.vilkårsperioder()).hasSize(1).first()
            .extracting(Vilkårsperiode::erOppfylt, Vilkårsperiode::innvilgelsesårsak)
            .containsExactly(true, Innvilgelsesårsak.SYKEPENGEERSTATNING)

        resultat =
            dataSource.transaction { ResultatUtleder(postgresRepositoryRegistry.provider(it)).utledResultat(behandling.id) }

        assertThat(resultat).isEqualTo(Resultat.INNVILGELSE)

        assertTidslinje(
            vilkårsresultat.rettighetstypeTidslinje(),
            periode to {
                assertThat(it).isEqualTo(RettighetsType.SYKEPENGEERSTATNING)
            })
    }

    @Test
    fun `ingen sykepenger i register, men skal vurdere sykepenger for samordning`() {
        val fom = LocalDate.now()
        val periode = Periode(fom, fom.plusYears(3))
        val sykePengerPeriode = Periode(LocalDate.now().minusMonths(1), LocalDate.now().plusMonths(1))
        // Simulerer et svar fra YS-løsning om at det finnes en yrkesskade
        val person = TestPerson(
            fødselsdato = Fødselsdato(LocalDate.now().minusYears(25)),
            sykepenger = emptyList()
        )
        FakePersoner.leggTil(person)

        val ident = person.aktivIdent()

        // Sender inn en søknad
        var behandling = sendInnDokument(
            ident, DokumentMottattPersonHendelse(
                journalpost = JournalpostId("20"),
                mottattTidspunkt = LocalDateTime.now().minusMonths(0),
                strukturertDokument = StrukturertDokument(
                    SøknadV0(
                        student = SøknadStudentDto("NEI"),
                        yrkesskade = "NEI",
                        oppgitteBarn = null,
                        medlemskap = SøknadMedlemskapDto("JA", "NEI", "NEI", "NEI", null)
                    ),
                ),
                periode = periode
            )
        )
        assertThat(behandling.typeBehandling()).isEqualTo(TypeBehandling.Førstegangsbehandling)

        val alleAvklaringsbehov = hentAlleAvklaringsbehov(behandling)
        assertThat(alleAvklaringsbehov).isNotEmpty()
        assertThat(behandling.status()).isEqualTo(Status.UTREDES)

        behandling = løsSykdom(behandling)

        behandling = løsAvklaringsBehov(
            behandling,
            AvklarBistandsbehovLøsning(
                bistandsVurdering = BistandVurderingLøsningDto(
                    begrunnelse = "Trenger hjelp fra nav",
                    erBehovForAktivBehandling = true,
                    erBehovForArbeidsrettetTiltak = false,
                    erBehovForAnnenOppfølging = null,
                    skalVurdereAapIOvergangTilUføre = null,
                    skalVurdereAapIOvergangTilArbeid = null,
                    overgangBegrunnelse = null
                ),
            ),
        )

        behandling = løsAvklaringsBehov(
            behandling,
            RefusjonkravLøsning(
                RefusjonkravVurdering(
                    harKrav = true,
                    fom = LocalDate.now(),
                    tom = null
                )
            )
        )

        behandling = løsAvklaringsBehov(
            behandling,
            avklaringsBehovLøsning = FritakMeldepliktLøsning(
                fritaksvurderinger = listOf(
                    FritaksvurderingDto(
                        harFritak = true,
                        fraDato = periode.fom,
                        begrunnelse = "...",
                    )
                ),
            ),
        )

        behandling = kvalitetssikre(behandling)

        behandling = løsAvklaringsBehov(
            behandling,
            FastsettBeregningstidspunktLøsning(
                beregningVurdering = BeregningstidspunktVurdering(
                    begrunnelse = "Trenger hjelp fra Nav",
                    nedsattArbeidsevneDato = LocalDate.now(),
                    ytterligereNedsattArbeidsevneDato = null,
                    ytterligereNedsattBegrunnelse = null
                ),
            ),
        )

        assertThat(hentÅpneAvklaringsbehov(behandling.id).map { it.definisjon }).containsExactly(Definisjon.FORESLÅ_VEDTAK)

        behandling = løsAvklaringsBehov(
            behandling,
            AvklarSamordningGraderingLøsning(
                vurderingerForSamordning = VurderingerForSamordning(
                    vurderteSamordningerData = listOf(
                        SamordningVurderingData(
                            ytelseType = Ytelse.SYKEPENGER,
                            periode = sykePengerPeriode,
                            gradering = 100,
                            kronesum = null,
                        )
                    ),
                    begrunnelse = "En god begrunnelse",
                    maksDatoEndelig = false,
                    maksDato = LocalDate.now().plusMonths(1),
                ),
            ),
        )
        assertThat(hentÅpneAvklaringsbehov(behandling.id).map { it.definisjon }).isEqualTo(listOf(Definisjon.FORESLÅ_VEDTAK))

        // Vilkår skal ikke være oppfylt med 100% gradert samordning
        val vilkår = hentVilkårsresultat(behandling.id).finnVilkår(Vilkårtype.SAMORDNING)
        assertThat(vilkår.vilkårsperioder()).hasSize(1)
            .first()
            .extracting(Vilkårsperiode::utfall).isEqualTo(Utfall.IKKE_OPPFYLT)

        behandling = løsAvklaringsBehov(
            behandling,
            AvklarSamordningGraderingLøsning(
                vurderingerForSamordning = VurderingerForSamordning(
                    vurderteSamordningerData = listOf(
                        SamordningVurderingData(
                            ytelseType = Ytelse.SYKEPENGER,
                            periode = sykePengerPeriode,
                            gradering = 50,
                            kronesum = null,
                        ),
                        SamordningVurderingData(
                            ytelseType = Ytelse.PLEIEPENGER,
                            periode = sykePengerPeriode,
                            gradering = 50,
                            kronesum = null,
                        )
                    ),
                    begrunnelse = "En god begrunnelse",
                    maksDatoEndelig = false,
                    maksDato = LocalDate.now().plusMonths(1),
                ),
            ),
        )

        // Vilkår skal være ikke vurdert når samordningen har mindre enn 100% gradering
        val vilkårOppdatert = hentVilkårsresultat(behandling.id).finnVilkår(Vilkårtype.SAMORDNING)
        assertThat(vilkårOppdatert.vilkårsperioder()).hasSize(1)
        assertThat(vilkårOppdatert.vilkårsperioder().first().utfall).isEqualTo(Utfall.IKKE_VURDERT)

        behandling = løsAvklaringsBehov(behandling, ForeslåVedtakLøsning())
        behandling = fattVedtak(behandling)

        val uthentetTilkjentYtelse =
            requireNotNull(dataSource.transaction { TilkjentYtelseRepositoryImpl(it).hentHvisEksisterer(behandling.id) })
            { "Tilkjent ytelse skal være beregnet her." }

        val periodeMedPositivSamordning =
            uthentetTilkjentYtelse.map { Segment(it.periode, it.tilkjent.gradering.samordningGradering) }
                .let(::Tidslinje)
                .filter { (it.verdi?.prosentverdi() ?: 0) > 0 }.helePerioden()

        // Verifiser at samordningen ble fanget opp
        assertThat(periodeMedPositivSamordning.tom).isEqualTo(sykePengerPeriode.tom)

        var brevbestilling = hentBrevAvType(behandling, TypeBrev.VEDTAK_INNVILGELSE)
        behandling = løsAvklaringsBehov(
            behandling, brevbestillingLøsning(behandling, brevbestilling), BREV_SYSTEMBRUKER
        )
        brevbestilling = hentBrevAvType(behandling, TypeBrev.VEDTAK_INNVILGELSE)
        val behandlingReferanse = behandling.referanse
        behandling =
            løsAvklaringsBehov(behandling, vedtaksbrevLøsning(brevbestilling.referanse.brevbestillingReferanse))

        // Siden samordning overlappet, skal en revurdering opprettes med en gang
        assertThat(behandling.referanse).isNotEqualTo(behandlingReferanse)
        assertThat(behandling.typeBehandling()).isEqualTo(TypeBehandling.Revurdering)
        util.ventPåSvar(sakId = behandling.sakId.id)

        // Verifiser at den er satt på vent
        var åpneAvklaringsbehovPåNyBehandling = hentÅpneAvklaringsbehov(behandling.id)
        util.ventPåSvar(behandlingId = behandling.id.id, sakId = behandling.sakId.id)
        assertThat(åpneAvklaringsbehovPåNyBehandling.map { it.definisjon }).containsExactly(Definisjon.SAMORDNING_VENT_PA_VIRKNINGSTIDSPUNKT)

        // Ta av vent
        behandling = løsAvklaringsBehov(behandling, SamordningVentPaVirkningstidspunktLøsning())

        åpneAvklaringsbehovPåNyBehandling = hentÅpneAvklaringsbehov(behandling.id)
        assertThat(åpneAvklaringsbehovPåNyBehandling.map { it.definisjon }).containsExactly(Definisjon.FORESLÅ_VEDTAK)
    }

    @Test
    fun `ved avslag på 11-5 hoppes det rett til beslutter-steget`() {
        val fom = LocalDate.now().minusMonths(3)
        val periode = Periode(fom, fom.plusYears(3))

        // Simulerer et svar fra YS-løsning om at det finnes en yrkesskade
        val person = TestPerson(fødselsdato = Fødselsdato(LocalDate.now().minusYears(25)))
        FakePersoner.leggTil(person)

        val ident = person.aktivIdent()

        // Sender inn en søknad
        var behandling = sendInnDokument(
            ident, DokumentMottattPersonHendelse(
                journalpost = JournalpostId("20"),
                mottattTidspunkt = LocalDateTime.now().minusMonths(3),
                strukturertDokument = StrukturertDokument(
                    SøknadV0(
                        student = SøknadStudentDto("NEI"),
                        yrkesskade = "NEI",
                        oppgitteBarn = null,
                        medlemskap = SøknadMedlemskapDto("JA", "NEI", "NEI", "NEI", null)
                    ),
                ),
                periode = periode
            )
        )
        var alleAvklaringsbehov = hentAlleAvklaringsbehov(behandling)
        assertThat(alleAvklaringsbehov).isNotEmpty()
        assertThat(behandling.status()).isEqualTo(Status.UTREDES)

        behandling = løsAvklaringsBehov(
            behandling,
            AvklarSykdomLøsning(
                sykdomsvurdering = SykdomsvurderingLøsningDto(
                    begrunnelse = "Er ikke syk nok",
                    dokumenterBruktIVurdering = listOf(JournalpostId("123123")),
                    harSkadeSykdomEllerLyte = false,
                    vurderingenGjelderFra = null,
                    erArbeidsevnenNedsatt = null,
                    erSkadeSykdomEllerLyteVesentligdel = null,
                    erNedsettelseIArbeidsevneAvEnVissVarighet = null,
                    erNedsettelseIArbeidsevneMerEnnHalvparten = null,
                    erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = null,
                    yrkesskadeBegrunnelse = null,
                )
            ),
        )

        behandling = kvalitetssikre(behandling)


        // Saken er tilbake til en-trinnskontroll hos saksbehandler klar for å bli sendt til beslutter
        alleAvklaringsbehov = hentÅpneAvklaringsbehov(behandling.id)
        assertThat(alleAvklaringsbehov).anySatisfy {
            assertThat(it)
                .extracting(Avklaringsbehov::erÅpent, Avklaringsbehov::definisjon)
                .containsExactly(true, Definisjon.FORESLÅ_VEDTAK)
        }
        assertThat(behandling.status()).isEqualTo(Status.UTREDES)

        behandling = løsAvklaringsBehov(behandling, ForeslåVedtakLøsning())

        // Saken står til To-trinnskontroll hos beslutter
        alleAvklaringsbehov = hentAlleAvklaringsbehov(behandling)
        assertThat(alleAvklaringsbehov).anySatisfy { assertTrue(it.erÅpent() && it.definisjon == Definisjon.FATTE_VEDTAK) }
        assertThat(behandling.status()).isEqualTo(Status.UTREDES)

        behandling = fattVedtak(behandling)
        assertThat(behandling.status()).isEqualTo(Status.IVERKSETTES)

        val vedtak = hentVedtak(behandling.id)
        assertThat(vedtak.vedtakstidspunkt.toLocalDate()).isToday

        alleAvklaringsbehov = hentAlleAvklaringsbehov(behandling)
        // Det er bestilt vedtaksbrev
        assertThat(alleAvklaringsbehov).anySatisfy { assertTrue(it.erÅpent() && it.definisjon == Definisjon.BESTILL_BREV) }

        val resultat = dataSource.transaction {
            ResultatUtleder(postgresRepositoryRegistry.provider(it)).utledResultat(behandling.id)
        }
        assertThat(resultat).isEqualTo(Resultat.AVSLAG)
        var brevbestilling = hentBrevAvType(behandling, TypeBrev.VEDTAK_AVSLAG)

        behandling =
            løsAvklaringsBehov(behandling, brevbestillingLøsning(behandling, brevbestilling), BREV_SYSTEMBRUKER)
        brevbestilling = hentBrevAvType(behandling, TypeBrev.VEDTAK_AVSLAG)

        behandling =
            løsAvklaringsBehov(behandling, vedtaksbrevLøsning(brevbestilling.referanse.brevbestillingReferanse))
        assertThat(behandling.status()).isEqualTo(Status.AVSLUTTET)

        // Verifiserer at sykdomsvilkåret ikke er oppfylt
        val vilkårsresultat = hentVilkårsresultat(behandlingId = behandling.id)
        val sykdomsvilkåret = vilkårsresultat.finnVilkår(Vilkårtype.SYKDOMSVILKÅRET)

        assertThat(sykdomsvilkåret.vilkårsperioder()).hasSize(1)
            .allMatch { vilkårsperiode -> !vilkårsperiode.erOppfylt() }

        // Saken er avsluttet, så det skal ikke være flere åpne avklaringsbehov
        val åpneAvklaringsbehov = hentÅpneAvklaringsbehov(behandling.id)
        assertThat(åpneAvklaringsbehov).isEmpty()
    }

    @Test
    fun `stopper opp ved samordning ved funn av sykepenger, og løses ved info fra saksbehandler`() {
        val fom = LocalDate.now()
        val periode = Periode(fom, fom.plusYears(3))

        // Simulerer et svar fra YS-løsning om at det finnes en yrkesskade
        val sykePengerPeriode = Periode(LocalDate.now().minusMonths(1), LocalDate.now().plusMonths(1))
        val person = TestPerson(
            fødselsdato = Fødselsdato(LocalDate.now().minusYears(25)),
            sykepenger = listOf(
                TestPerson.Sykepenger(
                    grad = 50,
                    periode = sykePengerPeriode
                )
            )
        )
        FakePersoner.leggTil(person)

        val ident = person.aktivIdent()

        // Sender inn en søknad
        var behandling = sendInnDokument(
            ident, DokumentMottattPersonHendelse(
                journalpost = JournalpostId("20"),
                mottattTidspunkt = LocalDateTime.now().minusMonths(0),
                strukturertDokument = StrukturertDokument(
                    SøknadV0(
                        student = SøknadStudentDto("NEI"),
                        yrkesskade = "NEI",
                        oppgitteBarn = null,
                        medlemskap = SøknadMedlemskapDto("JA", "NEI", "NEI", "NEI", null)
                    ),
                ),
                periode = periode
            )
        )
        assertThat(behandling.typeBehandling()).isEqualTo(TypeBehandling.Førstegangsbehandling)

        val alleAvklaringsbehov = hentAlleAvklaringsbehov(behandling)

        assertThat(alleAvklaringsbehov).isNotEmpty()
        assertThat(behandling.status()).isEqualTo(Status.UTREDES)

        behandling = løsSykdom(behandling)

        behandling = løsAvklaringsBehov(
            behandling,
            AvklarBistandsbehovLøsning(
                bistandsVurdering = BistandVurderingLøsningDto(
                    begrunnelse = "Trenger hjelp fra nav",
                    erBehovForAktivBehandling = true,
                    erBehovForArbeidsrettetTiltak = false,
                    erBehovForAnnenOppfølging = null,
                    skalVurdereAapIOvergangTilUføre = null,
                    skalVurdereAapIOvergangTilArbeid = null,
                    overgangBegrunnelse = null
                ),
            ),
        )

        behandling = løsAvklaringsBehov(
            behandling,
            RefusjonkravLøsning(
                RefusjonkravVurdering(
                    harKrav = true,
                    fom = LocalDate.now(),
                    tom = null
                )
            )
        )

        behandling = løsAvklaringsBehov(
            behandling,
            avklaringsBehovLøsning = FritakMeldepliktLøsning(
                fritaksvurderinger = listOf(
                    FritaksvurderingDto(
                        harFritak = true,
                        fraDato = periode.fom,
                        begrunnelse = "...",
                    )
                ),
            ),
        )

        behandling = kvalitetssikre(behandling)

        behandling = løsAvklaringsBehov(
            behandling,
            FastsettBeregningstidspunktLøsning(
                beregningVurdering = BeregningstidspunktVurdering(
                    begrunnelse = "Trenger hjelp fra Nav",
                    nedsattArbeidsevneDato = LocalDate.now(),
                    ytterligereNedsattArbeidsevneDato = null,
                    ytterligereNedsattBegrunnelse = null
                ),
            ),
        )

        assertThat(hentÅpneAvklaringsbehov(behandling.id).map { it.definisjon }).containsExactly(Definisjon.AVKLAR_SAMORDNING_GRADERING)

        behandling = løsAvklaringsBehov(
            behandling,
            AvklarSamordningGraderingLøsning(
                vurderingerForSamordning = VurderingerForSamordning(
                    vurderteSamordningerData = listOf(
                        SamordningVurderingData(
                            ytelseType = Ytelse.SYKEPENGER,
                            periode = sykePengerPeriode,
                            gradering = 90,
                            kronesum = null,
                        )
                    ),
                    begrunnelse = "En god begrunnelse",
                    maksDatoEndelig = false,
                    maksDato = LocalDate.now().plusMonths(1),
                ),
            ),
        )
        assertThat(hentÅpneAvklaringsbehov(behandling.id).map { it.definisjon }).isEqualTo(listOf(Definisjon.FORESLÅ_VEDTAK))

        behandling = løsAvklaringsBehov(behandling, ForeslåVedtakLøsning())
        behandling = fattVedtak(behandling)

        val uthentetTilkjentYtelse =
            requireNotNull(dataSource.transaction { TilkjentYtelseRepositoryImpl(it).hentHvisEksisterer(behandling.id) }) { "Tilkjent ytelse skal være beregnet her." }

        val periodeMedPositivSamordning =
            uthentetTilkjentYtelse.map { Segment(it.periode, it.tilkjent.gradering.samordningGradering) }
                .let(::Tidslinje)
                .filter { (it.verdi?.prosentverdi() ?: 0) > 0 }.helePerioden()

        // Verifiser at samordningen ble fanget opp
        assertThat(periodeMedPositivSamordning.tom).isEqualTo(sykePengerPeriode.tom)

        var brevbestilling = hentBrevAvType(behandling, TypeBrev.VEDTAK_INNVILGELSE)
        behandling = løsAvklaringsBehov(
            behandling, brevbestillingLøsning(behandling, brevbestilling), BREV_SYSTEMBRUKER
        )
        brevbestilling = hentBrevAvType(behandling, TypeBrev.VEDTAK_INNVILGELSE)
        val behandlingReferanse = behandling.referanse
        behandling =
            løsAvklaringsBehov(behandling, vedtaksbrevLøsning(brevbestilling.referanse.brevbestillingReferanse))

        // Siden samordning overlappet, skal en revurdering opprettes med en gang
        assertThat(behandling.referanse).isNotEqualTo(behandlingReferanse)
        assertThat(behandling.typeBehandling()).isEqualTo(TypeBehandling.Revurdering)
        util.ventPåSvar(sakId = behandling.sakId.id)

        // Verifiser at den er satt på vent
        var åpneAvklaringsbehovPåNyBehandling = hentÅpneAvklaringsbehov(behandling.id)
        util.ventPåSvar(behandlingId = behandling.id.id, sakId = behandling.sakId.id)
        assertThat(åpneAvklaringsbehovPåNyBehandling.map { it.definisjon }).containsExactly(Definisjon.SAMORDNING_VENT_PA_VIRKNINGSTIDSPUNKT)

        // Ta av vent
        behandling = løsAvklaringsBehov(behandling, SamordningVentPaVirkningstidspunktLøsning())

        åpneAvklaringsbehovPåNyBehandling = hentÅpneAvklaringsbehov(behandling.id)
        assertThat(åpneAvklaringsbehovPåNyBehandling.map { it.definisjon }).containsExactly(Definisjon.FORESLÅ_VEDTAK)
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
        var behandling = sendInnDokument(
            ident, DokumentMottattPersonHendelse(
                journalpost = JournalpostId("11"),
                mottattTidspunkt = LocalDateTime.now(),
                strukturertDokument = StrukturertDokument(
                    SøknadV0(
                        student = SøknadStudentDto("JA", "JA"),
                        yrkesskade = "JA",
                        oppgitteBarn = null,
                        medlemskap = SøknadMedlemskapDto("JA", "NEI", "NEI", "NEI", null)
                    )
                ),
                periode = periode
            )
        )
        assertThat(behandling.typeBehandling()).isEqualTo(TypeBehandling.Førstegangsbehandling)

        var alleAvklaringsbehov = hentAlleAvklaringsbehov(behandling)
        assertThat(alleAvklaringsbehov).isNotEmpty()
        assertThat(behandling.status()).isEqualTo(Status.UTREDES)

        behandling = løsAvklaringsBehov(
            behandling,
            AvklarStudentLøsning(
                studentvurdering = StudentVurderingDTO(
                    begrunnelse = "Er student",
                    avbruttStudieDato = LocalDate.now(),
                    avbruddMerEnn6Måneder = true,
                    harBehovForBehandling = true,
                    harAvbruttStudie = true,
                    avbruttPgaSykdomEllerSkade = true,
                    godkjentStudieAvLånekassen = false,
                )
            ),
        )

        behandling = løsAvklaringsBehov(
            behandling, AvklarSykdomLøsning(
                sykdomsvurdering = SykdomsvurderingLøsningDto(
                    begrunnelse = "Arbeidsevnen er nedsatt med mer enn halvparten",
                    dokumenterBruktIVurdering = listOf(JournalpostId("123123")),
                    harSkadeSykdomEllerLyte = true,
                    erSkadeSykdomEllerLyteVesentligdel = true,
                    erNedsettelseIArbeidsevneMerEnnHalvparten = true,
                    erNedsettelseIArbeidsevneAvEnVissVarighet = true,
                    erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = null,
                    erArbeidsevnenNedsatt = true,
                    yrkesskadeBegrunnelse = null,
                    vurderingenGjelderFra = null,
                )
            )
        )

        behandling = løsAvklaringsBehov(
            behandling,
            AvklarBistandsbehovLøsning(
                bistandsVurdering = BistandVurderingLøsningDto(
                    begrunnelse = "Trenger hjelp fra nav",
                    erBehovForAktivBehandling = true,
                    erBehovForArbeidsrettetTiltak = false,
                    erBehovForAnnenOppfølging = null,
                    skalVurdereAapIOvergangTilUføre = null,
                    skalVurdereAapIOvergangTilArbeid = null,
                    overgangBegrunnelse = null
                ),
            ),
        )

        behandling = løsAvklaringsBehov(
            behandling,
            RefusjonkravLøsning(
                RefusjonkravVurdering(
                    harKrav = true,
                    fom = LocalDate.now(),
                    tom = null
                )
            )
        )

        // Saken står til en-trinnskontroll hos saksbehandler klar for å bli sendt til beslutter
        alleAvklaringsbehov = hentAlleAvklaringsbehov(behandling)
        assertThat(alleAvklaringsbehov).isNotEmpty()
        assertThat(alleAvklaringsbehov).anySatisfy { behov -> assertThat(behov.erÅpent() && behov.definisjon == Definisjon.KVALITETSSIKRING).isTrue() }
        assertThat(behandling.status()).isEqualTo(Status.UTREDES)

        alleAvklaringsbehov = hentAlleAvklaringsbehov(behandling)
        behandling = løsAvklaringsBehov(
            behandling,
            KvalitetssikringLøsning(
                alleAvklaringsbehov
                    .filter { behov -> behov.kreverKvalitetssikring() }
                    .map { behov ->
                        TotrinnsVurdering(
                            behov.definisjon.kode,
                            true,
                            "begrunnelse",
                            emptyList()
                        )
                    }),

            )

        behandling = løsAvklaringsBehov(
            behandling,
            AvklarYrkesskadeLøsning(
                yrkesskadesvurdering = YrkesskadevurderingDto(
                    begrunnelse = "",
                    relevanteSaker = listOf(),
                    andelAvNedsettelsen = null,
                    erÅrsakssammenheng = false
                )
            ),
        )

        behandling = løsAvklaringsBehov(
            behandling,
            FastsettBeregningstidspunktLøsning(
                beregningVurdering = BeregningstidspunktVurdering(
                    begrunnelse = "Trenger hjelp fra Nav",
                    nedsattArbeidsevneDato = LocalDate.now(),
                    ytterligereNedsattArbeidsevneDato = null,
                    ytterligereNedsattBegrunnelse = null
                ),
            ),
        )

        behandling = løsAvklaringsBehov(behandling, ForeslåVedtakLøsning())

        // Saken står til To-trinnskontroll hos beslutter
        alleAvklaringsbehov = hentAlleAvklaringsbehov(behandling)
        assertThat(alleAvklaringsbehov).anySatisfy { assertThat(it.erÅpent() && it.definisjon == Definisjon.FATTE_VEDTAK).isTrue() }
        assertThat(behandling.status()).isEqualTo(Status.UTREDES)

        alleAvklaringsbehov = hentAlleAvklaringsbehov(behandling)
        behandling = løsAvklaringsBehov(
            behandling, FatteVedtakLøsning(
                alleAvklaringsbehov
                    .filter { behov -> behov.erTotrinn() }
                    .map { behov ->
                        TotrinnsVurdering(
                            behov.definisjon.kode,
                            behov.definisjon != Definisjon.AVKLAR_SYKDOM,
                            "begrunnelse",
                            emptyList()
                        )
                    }), Bruker("BESLUTTER")
        )
        assertThat(behandling.status()).isEqualTo(Status.UTREDES)
        alleAvklaringsbehov = hentAlleAvklaringsbehov(behandling)
        assertThat(alleAvklaringsbehov).anySatisfy { assertThat(it.erÅpent() && it.definisjon == Definisjon.AVKLAR_SYKDOM).isTrue() }

        behandling = løsAvklaringsBehov(
            behandling, AvklarSykdomLøsning(
                sykdomsvurdering = SykdomsvurderingLøsningDto(
                    begrunnelse = "Er syk nok",
                    dokumenterBruktIVurdering = listOf(JournalpostId("123123")),
                    harSkadeSykdomEllerLyte = true,
                    erSkadeSykdomEllerLyteVesentligdel = true,
                    erNedsettelseIArbeidsevneMerEnnHalvparten = true,
                    erNedsettelseIArbeidsevneAvEnVissVarighet = true,
                    erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = null,
                    erArbeidsevnenNedsatt = true,
                    yrkesskadeBegrunnelse = null,
                    vurderingenGjelderFra = null,
                )
            ),
            ingenEndringIGruppe = true,
            bruker = Bruker("SAKSBEHANDLER")
        )

        behandling = løsAvklaringsBehov(
            behandling, FastsettBeregningstidspunktLøsning(
                beregningVurdering = BeregningstidspunktVurdering(
                    begrunnelse = "Trenger hjelp fra Nav",
                    nedsattArbeidsevneDato = LocalDate.now(),
                    ytterligereNedsattArbeidsevneDato = null,
                    ytterligereNedsattBegrunnelse = null
                ),
            ),
            Bruker("SAKSBEHANDLER")
        )

        // Saken står til en-trinnskontroll hos saksbehandler klar for å bli sendt til beslutter
        alleAvklaringsbehov = hentAlleAvklaringsbehov(behandling)
        assertThat(alleAvklaringsbehov).anySatisfy { behov -> assertThat(behov.erÅpent() && behov.definisjon == Definisjon.FORESLÅ_VEDTAK).isTrue() }
        assertThat(behandling.status()).isEqualTo(Status.UTREDES)

        behandling = løsAvklaringsBehov(behandling, ForeslåVedtakLøsning())

        // Saken står til To-trinnskontroll hos beslutter
        alleAvklaringsbehov = hentAlleAvklaringsbehov(behandling)
        assertThat(alleAvklaringsbehov).anySatisfy { assertThat(it.erÅpent() && it.definisjon == Definisjon.FATTE_VEDTAK).isTrue() }
        assertThat(behandling.status()).isEqualTo(Status.UTREDES)

        behandling = fattVedtak(behandling)

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
                        yrkesskade = "JA",
                        oppgitteBarn = null,
                        medlemskap = SøknadMedlemskapDto("JA", "NEI", "NEI", "NEI", null),
                    ),

                    ),
                periode = periode
            )
        )
        util.ventPåSvar()

        val sak = hentSak(ident, periode)
        var behandling = hentBehandling(sak.id)
        assertThat(behandling.typeBehandling()).isEqualTo(TypeBehandling.Førstegangsbehandling)

        val stegHistorikk = hentStegHistorikk(behandling.id)
        assertThat(stegHistorikk.map { it.steg() }).contains(StegType.BREV)
        assertThat(stegHistorikk.map { it.status() }).contains(StegStatus.AVKLARINGSPUNKT)

        //Henter vurder alder-vilkår
        //Assert utfall
        val vilkårsresultat = hentVilkårsresultat(behandlingId = behandling.id)
        val aldersvilkår = vilkårsresultat.finnVilkår(Vilkårtype.ALDERSVILKÅRET)

        assertThat(aldersvilkår.vilkårsperioder())
            .hasSize(1)
            .noneMatch { vilkårsperiodeForAlder -> vilkårsperiodeForAlder.erOppfylt() }

        val status = behandling.status()
        assertThat(status).isEqualTo(Status.IVERKSETTES)

        val alleAvklaringsbehov = hentAlleAvklaringsbehov(behandling)
        assertThat(alleAvklaringsbehov).allSatisfy { assertThat(it.definisjon.kode).isEqualTo(AvklaringsbehovKode.`9002`) }

        var brevbestilling = hentBrevAvType(behandling, TypeBrev.VEDTAK_AVSLAG)
        behandling = løsAvklaringsBehov(
            behandling, brevbestillingLøsning(behandling, brevbestilling), BREV_SYSTEMBRUKER
        )

        brevbestilling = hentBrevAvType(behandling, TypeBrev.VEDTAK_AVSLAG)
        behandling =
            løsAvklaringsBehov(behandling, vedtaksbrevLøsning(brevbestilling.referanse.brevbestillingReferanse))

        val behov = hentÅpneAvklaringsbehov(behandling.id)
        assertThat(behov).isEmpty()

        util.ventPåSvar()

        assertThat(behandling.status()).isEqualTo(Status.AVSLUTTET)
        assertThat(hendelser.last().behandlingStatus).isEqualTo(Status.AVSLUTTET)
    }

    private fun brevbestillingLøsning(
        behandling: Behandling,
        brevbestilling: Brevbestilling
    ) = BrevbestillingLøsning(
        LøsBrevbestillingDto(
            behandlingReferanse = behandling.referanse.referanse,
            bestillingReferanse = brevbestilling.referanse.brevbestillingReferanse,
            status = BrevbestillingLøsningStatus.KLAR_FOR_EDITERING
        )
    )

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
                        medlemskap = SøknadMedlemskapDto("JA", "JA", "NEI", "NEI", null)
                    ),
                ),
                periode = periode
            )
        )
        util.ventPåSvar()

        val sak = hentSak(ident, periode)
        var behandling = hentBehandling(sak.id)

        assertThat(behandling.status()).isEqualTo(Status.UTREDES)

        var alleAvklaringsbehov = hentAlleAvklaringsbehov(behandling)
        assertThat(alleAvklaringsbehov).anySatisfy { assertThat(it.erÅpent() && it.definisjon == Definisjon.AVKLAR_SYKDOM).isTrue() }

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
        behandling = hentBehandling(sak.id)

        alleAvklaringsbehov = hentAlleAvklaringsbehov(behandling)
        assertThat(alleAvklaringsbehov)
            .hasSize(2)
            .anySatisfy { assertThat(it.erÅpent() && it.definisjon == Definisjon.MANUELT_SATT_PÅ_VENT).isTrue() }
            .anySatisfy { assertThat(it.erÅpent() && it.definisjon == Definisjon.AVKLAR_SYKDOM).isTrue() }

        hendelsesMottak.håndtere(
            ident, DokumentMottattPersonHendelse(
                journalpost = JournalpostId("3"),
                mottattTidspunkt = LocalDateTime.now(),
                strukturertDokument = StrukturertDokument(
                    SøknadV0(
                        student = SøknadStudentDto("NEI"), yrkesskade = "NEI", oppgitteBarn = null,
                        medlemskap = SøknadMedlemskapDto("JA", "JA", "NEI", "NEI", null)
                    ),
                ),
                periode = periode
            )
        )
        util.ventPåSvar(sak.id.toLong(), behandling.id.toLong())

        behandling = hentBehandling(sak.id)
        assertThat(behandling.status()).isEqualTo(Status.UTREDES)

        alleAvklaringsbehov = hentAlleAvklaringsbehov(behandling)
        assertThat(alleAvklaringsbehov)
            .hasSize(2)
            .anySatisfy { assertThat(it.erÅpent() && it.definisjon == Definisjon.MANUELT_SATT_PÅ_VENT).isTrue() }
            .anySatisfy { assertThat(it.erÅpent() && it.definisjon == Definisjon.AVKLAR_SYKDOM).isTrue() }
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
                        medlemskap = SøknadMedlemskapDto("JA", "JA", "NE)", "NEI", null)
                    ),
                ),
                periode = periode
            )
        )

        util.ventPåSvar()
        val sak = hentSak(ident, periode)
        val behandling = hentBehandling(sak.id)

        // Validér avklaring
        var alleAvklaringsbehov = hentAlleAvklaringsbehov(behandling)
        assertThat(alleAvklaringsbehov).anySatisfy { assertThat(it.erÅpent() && it.definisjon == Definisjon.AVKLAR_SYKDOM).isTrue() }

        // Oppretter bestilling av legeerklæring
        hendelsesMottak.bestillLegeerklæring(behandling.id)
        util.ventPåSvar(behandling.id.toLong())

        alleAvklaringsbehov = hentAlleAvklaringsbehov(behandling)
        assertThat(alleAvklaringsbehov).anySatisfy { assertThat(it.erÅpent() && it.definisjon == Definisjon.BESTILL_LEGEERKLÆRING).isTrue() }

        // Validér avklaring
        var åpneAvklaringsbehov = hentÅpneAvklaringsbehov(behandling.id)
        assertThat(åpneAvklaringsbehov.all { it.definisjon == Definisjon.BESTILL_LEGEERKLÆRING })

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
                    mottattTidspunkt = LocalDateTime.now()
                )
            )
        }
        util.ventPåSvar()

        // Validér avklaring
        åpneAvklaringsbehov = hentÅpneAvklaringsbehov(behandling.id)

        val legeerklæringBestillingVenteBehov =
            åpneAvklaringsbehov.filter { it.definisjon == Definisjon.BESTILL_LEGEERKLÆRING }
        assertThat(legeerklæringBestillingVenteBehov.isEmpty()).isTrue()

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
                        medlemskap = SøknadMedlemskapDto("JA", "JA", "NEI", "NEI", listOf())
                    ),
                ),
                periode = periode
            )
        )

        util.ventPåSvar()
        val sak = hentSak(ident, periode)
        val behandling = hentBehandling(sak.id)

        // Validér avklaring
        var alleAvklaringsbehov = hentAlleAvklaringsbehov(behandling)
        assertThat(alleAvklaringsbehov).anySatisfy { assertThat(it.erÅpent() && it.definisjon == Definisjon.AVKLAR_SYKDOM).isTrue() }

        // Oppretter bestilling av legeerklæring
        hendelsesMottak.bestillLegeerklæring(behandling.id)
        util.ventPåSvar(behandling.id.toLong())

        alleAvklaringsbehov = hentAlleAvklaringsbehov(behandling)
        assertThat(alleAvklaringsbehov).anySatisfy { assertThat(it.erÅpent() && it.definisjon == Definisjon.BESTILL_LEGEERKLÆRING).isTrue() }

        // Validér avklaring
        var åpneAvklaringsbehov = hentÅpneAvklaringsbehov(behandling.id)
        assertThat(åpneAvklaringsbehov.all { it.definisjon == Definisjon.BESTILL_LEGEERKLÆRING })

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
                    mottattTidspunkt = LocalDateTime.now()
                )
            )
        }
        util.ventPåSvar(sak.id.toLong())

        // Validér avklaring
        åpneAvklaringsbehov = hentÅpneAvklaringsbehov(behandling.id)

        val legeerklæringBestillingVenteBehov =
            åpneAvklaringsbehov.filter { it.definisjon == Definisjon.BESTILL_LEGEERKLÆRING }
        assertThat(legeerklæringBestillingVenteBehov.isEmpty()).isTrue()

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
                        medlemskap = SøknadMedlemskapDto("JA", "JA", "NEI", "NEI", null)
                    ),
                ),
                periode = periode
            )
        )

        util.ventPåSvar()
        val sak = hentSak(ident, periode)
        val behandling = hentBehandling(sak.id)

        // Validér avklaring
        var alleAvklaringsbehov = hentAlleAvklaringsbehov(behandling)
        assertThat(alleAvklaringsbehov).anySatisfy { assertThat(it.erÅpent() && it.definisjon == Definisjon.AVKLAR_SYKDOM).isTrue() }

        // Oppretter bestilling av legeerklæring
        hendelsesMottak.bestillLegeerklæring(behandling.id)

        assertThat(hentAlleAvklaringsbehov(behandling)).anySatisfy { assertThat(it.erÅpent() && it.definisjon == Definisjon.BESTILL_LEGEERKLÆRING).isTrue() }

        // Validér avklaring
        alleAvklaringsbehov = hentAlleAvklaringsbehov(behandling)
        assertThat(alleAvklaringsbehov.all { it.definisjon == Definisjon.BESTILL_LEGEERKLÆRING })

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
                    mottattTidspunkt = LocalDateTime.now()
                )
            )
        }
        util.ventPåSvar()

        // Validér avklaring
        val åpneAvklaringsbehov = hentÅpneAvklaringsbehov(behandling.id)
        val legeerklæringBestillingVenteBehov =
            åpneAvklaringsbehov.filter { it.definisjon == Definisjon.BESTILL_LEGEERKLÆRING }
        assertThat(legeerklæringBestillingVenteBehov.isEmpty()).isTrue()
    }

    @Test
    fun `Lager avklaringsbehov i medlemskap når kravene til manuell avklaring oppfylles`() {
        val ident = ident()
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))

        // Oppretter vanlig søknad
        hendelsesMottak.håndtere(
            ident, DokumentMottattPersonHendelse(
                journalpost = JournalpostId("26789"),
                mottattTidspunkt = LocalDateTime.now(),
                strukturertDokument = StrukturertDokument(
                    SøknadV0(
                        student = SøknadStudentDto("NEI"), yrkesskade = "NEI", oppgitteBarn = null,
                        medlemskap = SøknadMedlemskapDto("NEI", null, "JA", null, null)
                    ),
                ),
                periode = periode
            )
        )

        util.ventPåSvar()
        val sak = hentSak(ident, periode)
        val behandling = hentBehandling(sak.id)

        // Validér avklaring
        val åpenAvklaringsbehov = hentÅpneAvklaringsbehov(behandling.id)
        assertTrue(åpenAvklaringsbehov.all { it.definisjon == Definisjon.AVKLAR_LOVVALG_MEDLEMSKAP })
    }

    @Test
    fun `Går automatisk forbi medlemskap når kravene til manuell avklaring ikke oppfylles`() {
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
                        medlemskap = SøknadMedlemskapDto("JA", null, "NEI", null, null)
                    ),
                ),
                periode = periode
            )
        )

        util.ventPåSvar()
        val sak = hentSak(ident, periode)
        val behandling = hentBehandling(sak.id)

        // Validér avklaring
        val åpneAvklaringsbehov = hentÅpneAvklaringsbehov(behandling.id)
        assertTrue(åpneAvklaringsbehov.none { it.definisjon == Definisjon.AVKLAR_LOVVALG_MEDLEMSKAP })
    }

    @Test
    fun `Går videre i forutgåendemedlemskapsteget når manuell vurdering mottas`() {
        val ident = nyPerson(harYrkesskade = false, harUtenlandskOpphold = true)
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))

        // Oppretter vanlig søknad
        hendelsesMottak.håndtere(
            ident, DokumentMottattPersonHendelse(
                journalpost = JournalpostId("101"),
                mottattTidspunkt = LocalDateTime.now(),
                strukturertDokument = StrukturertDokument(
                    SøknadV0(
                        student = SøknadStudentDto("NEI"), yrkesskade = "NEI", oppgitteBarn = null,
                        medlemskap = SøknadMedlemskapDto("JA", null, "NEI", null, null),
                    ),
                ),
                periode = periode
            )
        )
        util.ventPåSvar()

        val sak = hentSak(ident, periode)
        var behandling = hentBehandling(sak.id)

        løsFramTilForutgåendeMedlemskap(behandling, harYrkesskade = false)

        // Validér avklaring
        var åpneAvklaringsbehov = hentÅpneAvklaringsbehov(behandling.id)
        assertTrue(åpneAvklaringsbehov.all { it.definisjon == Definisjon.AVKLAR_FORUTGÅENDE_MEDLEMSKAP })

        // Trigger manuell vurdering
        behandling = løsAvklaringsBehov(
            behandling,
            AvklarForutgåendeMedlemskapLøsning(
                manuellVurderingForForutgåendeMedlemskap = ManuellVurderingForForutgåendeMedlemskapDto(
                    "begrunnelse", true, null, null
                ),
            ),
        )

        // Validér avklaring
        åpneAvklaringsbehov = hentÅpneAvklaringsbehov(behandling.id)
        assertTrue(åpneAvklaringsbehov.none { it.definisjon == Definisjon.AVKLAR_FORUTGÅENDE_MEDLEMSKAP })
    }

    @Test
    fun `Oppfyller ikke forutgående medlemskap når unntak ikke oppfylles og ikke medlem i folketrygden`() {
        val ident = nyPerson(harYrkesskade = false, harUtenlandskOpphold = true)
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))

        // Oppretter vanlig søknad
        hendelsesMottak.håndtere(
            ident, DokumentMottattPersonHendelse(
                journalpost = JournalpostId("1502"),
                mottattTidspunkt = LocalDateTime.now(),
                strukturertDokument = StrukturertDokument(
                    SøknadV0(
                        student = SøknadStudentDto("NEI"), yrkesskade = "NEI", oppgitteBarn = null,
                        medlemskap = SøknadMedlemskapDto(
                            "JA", null, "NEI", null, null
                        ),
                    ),
                ),
                periode = periode
            )
        )
        util.ventPåSvar()

        val sak = hentSak(ident, periode)
        var behandling = hentBehandling(sak.id)

        løsFramTilForutgåendeMedlemskap(behandling, harYrkesskade = false)

        // Validér avklaring
        var åpneAvklaringsbehov = hentÅpneAvklaringsbehov(behandling.id)
        assertTrue(åpneAvklaringsbehov.all { it.definisjon == Definisjon.AVKLAR_FORUTGÅENDE_MEDLEMSKAP })

        // Trigger manuell vurdering
        behandling = løsAvklaringsBehov(
            behandling,
            AvklarForutgåendeMedlemskapLøsning(
                manuellVurderingForForutgåendeMedlemskap = ManuellVurderingForForutgåendeMedlemskapDto(
                    "begrunnelseforutgående", false, false, null
                )
            )
        )

        // Validér riktig resultat
        åpneAvklaringsbehov = hentÅpneAvklaringsbehov(behandling.id)
        val vilkårsResultat = hentVilkårsresultat(behandling.id).finnVilkår(Vilkårtype.MEDLEMSKAP).vilkårsperioder()
        assertTrue(åpneAvklaringsbehov.none { it.definisjon == Definisjon.AVKLAR_FORUTGÅENDE_MEDLEMSKAP })
        assertTrue(vilkårsResultat.none { it.erOppfylt() })
    }

    @Test
    fun `Oppfyller forutgående medlemskap når unntak finnes`() {
        val ident = nyPerson(harYrkesskade = false, harUtenlandskOpphold = true)
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))

        // Oppretter vanlig søknad
        hendelsesMottak.håndtere(
            ident, DokumentMottattPersonHendelse(
                journalpost = JournalpostId("10111"),
                mottattTidspunkt = LocalDateTime.now(),
                strukturertDokument = StrukturertDokument(
                    SøknadV0(
                        student = SøknadStudentDto("NEI"), yrkesskade = "NEI", oppgitteBarn = null,
                        medlemskap = SøknadMedlemskapDto(
                            "JA", null, "NEI", null, null
                        ),
                    ),
                ),
                periode = periode
            )
        )
        util.ventPåSvar()

        val sak = hentSak(ident, periode)
        var behandling = hentBehandling(sak.id)

        løsFramTilForutgåendeMedlemskap(behandling, harYrkesskade = false)

        // Validér avklaring
        var åpneAvklaringsbehov = hentÅpneAvklaringsbehov(behandling.id)
        assertTrue(åpneAvklaringsbehov.all { it.definisjon == Definisjon.AVKLAR_FORUTGÅENDE_MEDLEMSKAP })

        // Trigger manuell vurdering
        behandling = løsAvklaringsBehov(
            behandling, AvklarForutgåendeMedlemskapLøsning(
                manuellVurderingForForutgåendeMedlemskap = ManuellVurderingForForutgåendeMedlemskapDto(
                    "begrunnelse", true, true, null
                ),
                behovstype = AvklaringsbehovKode.`5020`
            )
        )

        // Validér riktig resultat
        åpneAvklaringsbehov = hentÅpneAvklaringsbehov(behandling.id)
        val vilkårsResultat = hentVilkårsresultat(behandling.id).finnVilkår(Vilkårtype.MEDLEMSKAP).vilkårsperioder()
        assertTrue(åpneAvklaringsbehov.none { it.definisjon == Definisjon.AVKLAR_FORUTGÅENDE_MEDLEMSKAP })
        assertTrue(vilkårsResultat.all { it.erOppfylt() })
    }

    @Test
    fun `Går forbi forutgåendemedlemskapsteget når yrkesskade eksisterer`() {
        val ident = nyPerson(harYrkesskade = true, harUtenlandskOpphold = false)
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))

        // Oppretter vanlig søknad
        hendelsesMottak.håndtere(
            ident, DokumentMottattPersonHendelse(
                journalpost = JournalpostId("1050"),
                mottattTidspunkt = LocalDateTime.now(),
                strukturertDokument = StrukturertDokument(
                    SøknadV0(
                        student = SøknadStudentDto("NEI"), yrkesskade = "JA", oppgitteBarn = null,
                        medlemskap = SøknadMedlemskapDto("JA", null, "NEI", null, null),
                    ),
                ),
                periode = periode
            )
        )
        util.ventPåSvar()

        val sak = hentSak(ident, periode)
        val behandling = hentBehandling(sak.id)

        løsFramTilForutgåendeMedlemskap(behandling = behandling, harYrkesskade = true)

        // Validér avklaring
        val åpneAvklaringsbehov = hentÅpneAvklaringsbehov(behandling.id)
        assertTrue(åpneAvklaringsbehov.none { it.definisjon == Definisjon.AVKLAR_FORUTGÅENDE_MEDLEMSKAP })
    }

    @Test
    fun `Gir oppfylt når bruker ikke har lovvalgsland men oppfyller trygdeloven`() {
        val ident = ident()
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))

        // Oppretter vanlig søknad
        hendelsesMottak.håndtere(
            ident, DokumentMottattPersonHendelse(
                journalpost = JournalpostId("102"),
                mottattTidspunkt = LocalDateTime.now(),
                strukturertDokument = StrukturertDokument(
                    SøknadV0(
                        student = SøknadStudentDto("NEI"), yrkesskade = "NEI", oppgitteBarn = null,
                        medlemskap = SøknadMedlemskapDto(
                            "JA", null, "JA", null,
                            listOf(
                                UtenlandsPeriodeDto(
                                    "SWE",
                                    LocalDate.now().plusMonths(1),
                                    LocalDate.now().minusMonths(1),
                                    "JA",
                                    null,
                                    LocalDate.now().plusMonths(1),
                                    LocalDate.now().minusMonths(1),
                                )
                            )
                        )
                    ),
                ),
                periode = periode
            )
        )

        util.ventPåSvar()
        val sak = hentSak(ident, periode)
        var behandling = hentBehandling(sak.id)

        // Validér avklaring
        var åpneAvklaringsbehov = hentÅpneAvklaringsbehov(behandling.id)
        assertTrue(åpneAvklaringsbehov.all { it.definisjon == Definisjon.AVKLAR_LOVVALG_MEDLEMSKAP })

        // Trigger manuell vurdering
        behandling = løsAvklaringsBehov(
            behandling,
            AvklarLovvalgMedlemskapLøsning(
                manuellVurderingForLovvalgMedlemskap = ManuellVurderingForLovvalgMedlemskapDto(
                    LovvalgVedSøknadsTidspunktDto("crazy lovvalgsland vurdering", null),
                    MedlemskapVedSøknadsTidspunktDto("crazy medlemskap vurdering", true)
                ),
                behovstype = AvklaringsbehovKode.`5017`
            )
        )

        // Validér riktig resultat
        åpneAvklaringsbehov = hentÅpneAvklaringsbehov(behandling.id)
        val vilkårsResultat = hentVilkårsresultat(behandling.id).finnVilkår(Vilkårtype.LOVVALG).vilkårsperioder()
        assertTrue(åpneAvklaringsbehov.none { it.definisjon == Definisjon.AVKLAR_FORUTGÅENDE_MEDLEMSKAP })
        assertTrue(vilkårsResultat.all { it.erOppfylt() })
    }

    @Test
    fun `Gir avslag når bruker har annet lovvalgsland`() {
        val ident = ident()
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))

        // Oppretter vanlig søknad
        hendelsesMottak.håndtere(
            ident, DokumentMottattPersonHendelse(
                journalpost = JournalpostId("102"),
                mottattTidspunkt = LocalDateTime.now(),
                strukturertDokument = StrukturertDokument(
                    SøknadV0(
                        student = SøknadStudentDto("NEI"), yrkesskade = "NEI", oppgitteBarn = null,
                        medlemskap = SøknadMedlemskapDto(
                            "JA", null, "JA", null,
                            listOf(
                                UtenlandsPeriodeDto(
                                    "SWE",
                                    LocalDate.now().plusMonths(1),
                                    LocalDate.now().minusMonths(1),
                                    "JA",
                                    null,
                                    LocalDate.now().plusMonths(1),
                                    LocalDate.now().minusMonths(1),
                                )
                            )
                        ),
                    ),
                ),
                periode = periode
            )
        )

        util.ventPåSvar()
        val sak = hentSak(ident, periode)
        var behandling = hentBehandling(sak.id)

        // Validér avklaring
        var åpneAvklaringsbehov = hentÅpneAvklaringsbehov(behandling.id)
        assertTrue(åpneAvklaringsbehov.all { it.definisjon == Definisjon.AVKLAR_LOVVALG_MEDLEMSKAP })

        // Trigger manuell vurdering
        behandling = løsAvklaringsBehov(
            behandling, AvklarLovvalgMedlemskapLøsning(
                manuellVurderingForLovvalgMedlemskap = ManuellVurderingForLovvalgMedlemskapDto(
                    LovvalgVedSøknadsTidspunktDto("crazy lovvalgsland vurdering", EØSLand.DNK),
                    MedlemskapVedSøknadsTidspunktDto(null, null)
                )
            )
        )
        // Validér riktig resultat
        åpneAvklaringsbehov = hentÅpneAvklaringsbehov(behandling.id)
        val vilkårsResultat = hentVilkårsresultat(behandling.id).finnVilkår(Vilkårtype.LOVVALG).vilkårsperioder()
        assertTrue(åpneAvklaringsbehov.none { it.definisjon == Definisjon.AVKLAR_LOVVALG_MEDLEMSKAP })
        assertTrue(vilkårsResultat.none { it.erOppfylt() })
    }

    @Test
    fun `Gir avslag når bruker ikke er medlem i trygden`() {
        val ident = ident()
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))

        // Oppretter vanlig søknad
        hendelsesMottak.håndtere(
            ident, DokumentMottattPersonHendelse(
                journalpost = JournalpostId("103"),
                mottattTidspunkt = LocalDateTime.now(),
                strukturertDokument = StrukturertDokument(
                    SøknadV0(
                        student = SøknadStudentDto("NEI"), yrkesskade = "NEI", oppgitteBarn = null,
                        medlemskap = SøknadMedlemskapDto(
                            "JA", null, "JA", null,
                            listOf(
                                UtenlandsPeriodeDto(
                                    "SWE",
                                    LocalDate.now().plusMonths(1),
                                    LocalDate.now().minusMonths(1),
                                    "JA",
                                    null,
                                    LocalDate.now().plusMonths(1),
                                    LocalDate.now().minusMonths(1),
                                )
                            )
                        ),
                    ),
                ),
                periode = periode
            )
        )

        util.ventPåSvar()
        val sak = hentSak(ident, periode)
        var behandling = hentBehandling(sak.id)

        // Validér avklaring
        var åpneAvklaringsbehov = hentÅpneAvklaringsbehov(behandling.id)
        assertTrue(åpneAvklaringsbehov.all { it.definisjon == Definisjon.AVKLAR_LOVVALG_MEDLEMSKAP })

        // Trigger manuell vurdering
        behandling = løsAvklaringsBehov(
            behandling, AvklarLovvalgMedlemskapLøsning(
                manuellVurderingForLovvalgMedlemskap = ManuellVurderingForLovvalgMedlemskapDto(
                    LovvalgVedSøknadsTidspunktDto("crazy lovvalgsland vurdering", EØSLand.NOR),
                    MedlemskapVedSøknadsTidspunktDto("crazy medlemskap vurdering", false)
                )
            )
        )

        // Validér avklaring
        åpneAvklaringsbehov = hentÅpneAvklaringsbehov(behandling.id)
        assertThat(åpneAvklaringsbehov.none())

        // Validér riktig resultat
        val vilkårsResultat = hentVilkårsresultat(behandling.id).finnVilkår(Vilkårtype.LOVVALG).vilkårsperioder()
        assertThat(vilkårsResultat).noneMatch { it.erOppfylt() }
        assertTrue(Avslagsårsak.IKKE_MEDLEM == vilkårsResultat.first().avslagsårsak)
    }

    @Test
    fun `Kan løse forutgående overstyringsbehov til ikke oppfylt`() {
        val ident = nyPerson(harYrkesskade = false, harUtenlandskOpphold = false)
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))

        // Oppretter vanlig søknad
        hendelsesMottak.håndtere(
            ident, DokumentMottattPersonHendelse(
                journalpost = JournalpostId("451"),
                mottattTidspunkt = LocalDateTime.now(),
                strukturertDokument = StrukturertDokument(
                    SøknadV0(
                        student = SøknadStudentDto("NEI"), yrkesskade = "NEI", oppgitteBarn = null,
                        medlemskap = SøknadMedlemskapDto("JA", null, "NEI", null, null)
                    ),
                ),
                periode = periode
            )
        )
        util.ventPåSvar()
        val sak = hentSak(ident, periode)
        var behandling = hentBehandling(sak.id)

        løsFramTilForutgåendeMedlemskap(behandling, harYrkesskade = false)

        // Validér avklaring
        var åpneAvklaringsbehov = hentÅpneAvklaringsbehov(behandling.id)
        assertThat(åpneAvklaringsbehov).noneMatch { it.definisjon == Definisjon.MANUELL_OVERSTYRING_MEDLEMSKAP }

        // Validér riktig resultat
        var vilkårsResultat = hentVilkårsresultat(behandling.id).finnVilkår(Vilkårtype.MEDLEMSKAP).vilkårsperioder()
        assertThat(vilkårsResultat).allMatch { it.erOppfylt() }

        behandling = løsAvklaringsBehov(
            behandling, AvklarOverstyrtForutgåendeMedlemskapLøsning(
                manuellVurderingForForutgåendeMedlemskap = ManuellVurderingForForutgåendeMedlemskapDto(
                    "because", false, false, false
                ),
            )
        )

        // Validér avklaring
        åpneAvklaringsbehov = hentÅpneAvklaringsbehov(behandling.id)
        assertTrue(åpneAvklaringsbehov.none { Definisjon.MANUELL_OVERSTYRING_MEDLEMSKAP == it.definisjon })

        // Validér riktig resultat
        vilkårsResultat = hentVilkårsresultat(behandling.id).finnVilkår(Vilkårtype.MEDLEMSKAP).vilkårsperioder()
        assertTrue(vilkårsResultat.none { it.erOppfylt() })
        assertThat(Avslagsårsak.IKKE_MEDLEM_FORUTGÅENDE).isEqualTo(vilkårsResultat.first().avslagsårsak)
    }

    @Test
    fun `Kan løse overstyringsbehov til ikke oppfylt`() {
        val ident = ident()
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))

        // Oppretter vanlig søknad
        hendelsesMottak.håndtere(
            ident, DokumentMottattPersonHendelse(
                journalpost = JournalpostId("451"),
                mottattTidspunkt = LocalDateTime.now(),
                strukturertDokument = StrukturertDokument(
                    SøknadV0(
                        student = SøknadStudentDto("NEI"), yrkesskade = "NEI", oppgitteBarn = null,
                        medlemskap = SøknadMedlemskapDto("JA", null, "NEI", null, null)
                    ),
                ),
                periode = periode
            )
        )
        util.ventPåSvar()
        val sak = hentSak(ident, periode)
        var behandling = hentBehandling(sak.id)

        behandling = løsAvklaringsBehov(
            behandling, AvklarOverstyrtLovvalgMedlemskapLøsning(
                manuellVurderingForLovvalgMedlemskap = ManuellVurderingForLovvalgMedlemskapDto(
                    LovvalgVedSøknadsTidspunktDto("crazy lovvalgsland vurdering", EØSLand.NOR),
                    MedlemskapVedSøknadsTidspunktDto("crazy medlemskap vurdering", false)
                )
            )
        )

        // Validér avklaring
        val åpneAvklaringsbehov = hentÅpneAvklaringsbehov(behandling.id)
        assertTrue(åpneAvklaringsbehov.none { Definisjon.MANUELL_OVERSTYRING_LOVVALG == it.definisjon })

        // Validér riktig resultat
        val vilkårsResultat = hentVilkårsresultat(behandling.id).finnVilkår(Vilkårtype.LOVVALG).vilkårsperioder()
        assertTrue(vilkårsResultat.none { it.erOppfylt() })
        assertTrue(Avslagsårsak.IKKE_MEDLEM == vilkårsResultat.first().avslagsårsak)
    }

    @Test
    fun `Kan løse overstyringsbehov til oppfylt`() {
        val ident = ident()
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))

        // Oppretter vanlig søknad
        hendelsesMottak.håndtere(
            ident, DokumentMottattPersonHendelse(
                journalpost = JournalpostId("451"),
                mottattTidspunkt = LocalDateTime.now(),
                strukturertDokument = StrukturertDokument(
                    SøknadV0(
                        student = SøknadStudentDto("NEI"), yrkesskade = "NEI", oppgitteBarn = null,
                        medlemskap = SøknadMedlemskapDto("JA", null, "NEI", null, null)
                    ),
                ),
                periode = periode
            )
        )
        util.ventPåSvar()
        val sak = hentSak(ident, periode)
        var behandling = hentBehandling(sak.id)

        behandling = løsAvklaringsBehov(
            behandling, AvklarOverstyrtLovvalgMedlemskapLøsning(
                manuellVurderingForLovvalgMedlemskap = ManuellVurderingForLovvalgMedlemskapDto(
                    LovvalgVedSøknadsTidspunktDto("crazy lovvalgsland vurdering", EØSLand.NOR),
                    MedlemskapVedSøknadsTidspunktDto("crazy medlemskap vurdering", true)
                )
            )
        )

        // Validér avklaring
        val åpneAvklaringsbehov = hentÅpneAvklaringsbehov(behandling.id)
        assertTrue(åpneAvklaringsbehov.none { Definisjon.MANUELL_OVERSTYRING_LOVVALG == it.definisjon })

        // Validér riktig resultat
        val vilkårsResultat = hentVilkårsresultat(behandling.id).finnVilkår(Vilkårtype.LOVVALG).vilkårsperioder()
        val overstyrtManuellVurdering = dataSource.transaction {
            MedlemskapArbeidInntektRepositoryImpl(it).hentHvisEksisterer(behandling.id)?.manuellVurdering?.overstyrt
        }
        assertTrue(vilkårsResultat.all { it.erOppfylt() })
        assertTrue(overstyrtManuellVurdering == true)
    }

    @Test
    fun `kan tilbakeføre behandling til start`() {
        val ident = ident()
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))

        // Oppretter vanlig søknad
        hendelsesMottak.håndtere(
            ident, DokumentMottattPersonHendelse(
                journalpost = JournalpostId("1021234"),
                mottattTidspunkt = LocalDateTime.now(),
                strukturertDokument = StrukturertDokument(
                    SøknadV0(
                        student = SøknadStudentDto("NEI"), yrkesskade = "NEI", oppgitteBarn = null,
                        medlemskap = SøknadMedlemskapDto(
                            "JA", null, "JA", null,
                            listOf(
                                UtenlandsPeriodeDto(
                                    "SWE",
                                    LocalDate.now().plusMonths(1),
                                    LocalDate.now().minusMonths(1),
                                    "JA",
                                    null,
                                    LocalDate.now().plusMonths(1),
                                    LocalDate.now().minusMonths(1),
                                )
                            )
                        ),
                    ),
                ),
                periode = periode
            )
        )

        util.ventPåSvar()

        val sak = hentSak(ident, periode)
        val behandling = hentBehandling(sak.id)
        val behandlingId = behandling.id

        // Validér avklaring
        var åpneAvklaringsbehov = hentÅpneAvklaringsbehov(behandlingId)
        assertTrue(åpneAvklaringsbehov.all { it.definisjon == Definisjon.AVKLAR_LOVVALG_MEDLEMSKAP })

        // Trigger manuell vurdering
        løsAvklaringsBehov(
            behandling, AvklarLovvalgMedlemskapLøsning(
                manuellVurderingForLovvalgMedlemskap = ManuellVurderingForLovvalgMedlemskapDto(
                    LovvalgVedSøknadsTidspunktDto("crazy lovvalgsland vurdering", EØSLand.NOR),
                    MedlemskapVedSøknadsTidspunktDto(null, true)
                )
            )
        )

        // Validér avklaring
        åpneAvklaringsbehov = hentÅpneAvklaringsbehov(behandlingId)
        assertTrue(åpneAvklaringsbehov.all { it.definisjon == Definisjon.AVKLAR_SYKDOM })

        dataSource.transaction { connection ->
            val behandlingRepo = BehandlingRepositoryImpl(connection)
            assertThat(behandlingRepo.hent(behandlingId).aktivtSteg()).isEqualTo(StegType.AVKLAR_SYKDOM)

            // Tilbakefør med hjelpefunksjon
            Driftfunksjoner(postgresRepositoryRegistry).flyttBehandlingTilStart(behandlingId, connection)

            // Validér avklaring
            assertThat(behandlingRepo.hent(behandlingId).aktivtSteg()).isEqualTo(StegType.START_BEHANDLING)
        }

        util.ventPåSvar()
        val b = hentBehandling(sak.id)
        assertThat(b.aktivtSteg()).isEqualTo(StegType.AVKLAR_SYKDOM)
    }

    @Test
    fun `Skal sette behandling på vent hvis man mottar klage i prod`() {
        System.setProperty("NAIS_CLUSTER_NAME", "prod-test")
        val ident = ident()
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))
        val sak = hentSak(ident, periode)

        opprettBehandling(
            sak.id,
            årsaker = listOf(Årsak(ÅrsakTilBehandling.MOTTATT_SØKNAD)),
            forrigeBehandlingId = null,
            typeBehandling = TypeBehandling.Førstegangsbehandling
        )

        val kravMottatt = LocalDate.now().minusDays(10)
        val nyBehandling = sendInnDokument(
            ident, DokumentMottattPersonHendelse(
                journalpost = JournalpostId("21"),
                mottattTidspunkt = LocalDateTime.now().minusMonths(3),
                InnsendingType.KLAGE,
                strukturertDokument = StrukturertDokument(KlageV0(kravMottatt = kravMottatt)),
                periode
            )

        )

        assertThat(nyBehandling.typeBehandling() == TypeBehandling.Klage)

        val åpneAvklaringsbehov = hentÅpneAvklaringsbehov(nyBehandling.id).first()

        assertTrue(åpneAvklaringsbehov.erÅpent())
        assertTrue(åpneAvklaringsbehov.erVentepunkt())
        assertThat(åpneAvklaringsbehov.definisjon).isEqualTo(Definisjon.VENTE_PÅ_KLAGE_IMPLEMENTASJON)

        System.setProperty("NAIS_CLUSTER_NAME", "LOCAL")
    }

    @Test
    fun `Teste Klageflyt`() {
        val person = TestPerson(
            fødselsdato = Fødselsdato(LocalDate.now().minusYears(14)),
            yrkesskade = listOf(TestYrkesskade()),
        )
        FakePersoner.leggTil(person)

        val ident = person.aktivIdent()

        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))

        // Avslås pga. alder
        val avslåttFørstegang = sendInnDokument(
            ident, DokumentMottattPersonHendelse(
                journalpost = JournalpostId("19"),
                mottattTidspunkt = LocalDateTime.now().minusMonths(4),
                InnsendingType.SØKNAD,
                strukturertDokument = StrukturertDokument(
                    SøknadV0(
                        student = SøknadStudentDto("NEI"),
                        yrkesskade = "NEI",
                        oppgitteBarn = null,
                        medlemskap = SøknadMedlemskapDto("JA", "NEI", "NEI", "NEI", null)
                    ),
                ),
                periode
            )
        )
        assertThat(avslåttFørstegang)
            .describedAs("Førstegangsbehandlingen skal være satt som avsluttet")
            .extracting { b -> b.status().erAvsluttet() }.isEqualTo(true)
        val kravMottatt = LocalDate.now().minusMonths(1)
        val klagebehandling = sendInnDokument(
            ident, DokumentMottattPersonHendelse(
                journalpost = JournalpostId("21"),
                mottattTidspunkt = LocalDateTime.now().minusMonths(3),
                InnsendingType.KLAGE,
                strukturertDokument = StrukturertDokument(KlageV0(kravMottatt = kravMottatt)),
                periode
            )
        )
        assertThat(klagebehandling.referanse).isNotEqualTo(avslåttFørstegang.referanse)
        assertThat(klagebehandling.typeBehandling()).isEqualTo(TypeBehandling.Klage)

        dataSource.transaction { connection ->
            val mottattDokumentRepository = MottattDokumentRepositoryImpl(connection)
            val klageDokumenter =
                mottattDokumentRepository.hentDokumenterAvType(klagebehandling.sakId, InnsendingType.KLAGE)
            assertThat(klageDokumenter).hasSize(1)
            assertThat(klageDokumenter.first().strukturertDokument).isNotNull
            assertThat(klageDokumenter.first().strukturerteData<KlageV0>()?.data?.kravMottatt).isEqualTo(kravMottatt)
        }

        var åpneAvklaringsbehov = hentÅpneAvklaringsbehov(klagebehandling.id)
        assertThat(åpneAvklaringsbehov).hasSize(1).first().extracting(Avklaringsbehov::definisjon)
            .isEqualTo(Definisjon.FASTSETT_PÅKLAGET_BEHANDLING)

        løsAvklaringsBehov(
            klagebehandling,
            avklaringsBehovLøsning = FastsettPåklagetBehandlingLøsning(
                påklagetBehandlingVurdering = PåklagetBehandlingVurderingLøsningDto(
                    påklagetVedtakType = PåklagetVedtakType.KELVIN_BEHANDLING,
                    påklagetBehandling = avslåttFørstegang.referanse.referanse,
                )
            )
        )

        åpneAvklaringsbehov = hentÅpneAvklaringsbehov(klagebehandling.id)
        assertThat(åpneAvklaringsbehov).hasSize(1)
        assertThat(åpneAvklaringsbehov.first().definisjon).isEqualTo(Definisjon.VURDER_FORMKRAV)

        løsAvklaringsBehov(
            klagebehandling,
            avklaringsBehovLøsning = VurderFormkravLøsning(
                formkravVurdering = FormkravVurderingLøsningDto(
                    begrunnelse = "Begrunnelse",
                    erBrukerPart = true,
                    erFristOverholdt = false,
                    likevelBehandles = true,
                    erKonkret = true,
                    erSignert = true
                )
            )
        )

        åpneAvklaringsbehov = hentÅpneAvklaringsbehov(klagebehandling.id)
        assertThat(åpneAvklaringsbehov).hasSize(1).first().extracting(Avklaringsbehov::definisjon)
            .isEqualTo(Definisjon.FASTSETT_BEHANDLENDE_ENHET)

        løsAvklaringsBehov(
            klagebehandling,
            avklaringsBehovLøsning = FastsettBehandlendeEnhetLøsning(
                behandlendeEnhetVurdering = BehandlendeEnhetLøsningDto(
                    skalBehandlesAvNay = true,
                    skalBehandlesAvKontor = true
                )
            )
        )

        åpneAvklaringsbehov = hentÅpneAvklaringsbehov(klagebehandling.id)
        assertThat(åpneAvklaringsbehov).hasSize(1)
        assertThat(åpneAvklaringsbehov.first().definisjon).isEqualTo(Definisjon.VURDER_KLAGE_KONTOR)

        løsAvklaringsBehov(
            klagebehandling,
            avklaringsBehovLøsning = VurderKlageKontorLøsning(
                klagevurderingKontor = KlagevurderingKontorLøsningDto(
                    begrunnelse = "Begrunnelse",
                    notat = null,
                    innstilling = KlageInnstilling.OPPRETTHOLD,
                    vilkårSomOmgjøres = emptyList(),
                    vilkårSomOpprettholdes = listOf(Hjemmel.FOLKETRYGDLOVEN_11_5)
                )
            )
        )
        åpneAvklaringsbehov = hentÅpneAvklaringsbehov(klagebehandling.id)
        assertThat(åpneAvklaringsbehov).hasSize(1)
        assertThat(åpneAvklaringsbehov.first().definisjon).isEqualTo(Definisjon.KVALITETSSIKRING)

        løsAvklaringsBehov(
            klagebehandling,
            avklaringsBehovLøsning = KvalitetssikringLøsning(
                vurderinger = listOf(
                    TotrinnsVurdering(
                        begrunnelse = "Begrunnelse",
                        godkjent = true,
                        definisjon = Definisjon.VURDER_KLAGE_KONTOR.kode,
                        grunner = emptyList(),
                    )
                )
            )
        )
        åpneAvklaringsbehov = hentÅpneAvklaringsbehov(klagebehandling.id)
        assertThat(åpneAvklaringsbehov).hasSize(1)
        assertThat(åpneAvklaringsbehov.first().definisjon).isEqualTo(Definisjon.VURDER_KLAGE_NAY)

        løsAvklaringsBehov(
            klagebehandling,
            avklaringsBehovLøsning = VurderKlageNayLøsning(
                klagevurderingNay = KlagevurderingNayLøsningDto(
                    begrunnelse = "Begrunnelse",
                    notat = null,
                    innstilling = KlageInnstilling.OPPRETTHOLD,
                    vilkårSomOmgjøres = emptyList(),
                    vilkårSomOpprettholdes = listOf(Hjemmel.FOLKETRYGDLOVEN_11_5)
                )
            )
        )
        åpneAvklaringsbehov = hentÅpneAvklaringsbehov(klagebehandling.id)
        assertThat(åpneAvklaringsbehov).hasSize(1)
        assertThat(åpneAvklaringsbehov.first().definisjon).isEqualTo(Definisjon.FATTE_VEDTAK)

        løsAvklaringsBehov(
            klagebehandling,
            avklaringsBehovLøsning = FatteVedtakLøsning(
                vurderinger = listOf(
                    TotrinnsVurdering(
                        begrunnelse = "Begrunnelse",
                        godkjent = true,
                        definisjon = Definisjon.VURDER_KLAGE_NAY.kode,
                        grunner = emptyList(),
                    )
                )
            )
        )
        åpneAvklaringsbehov = hentÅpneAvklaringsbehov(klagebehandling.id)
        assertThat(åpneAvklaringsbehov).hasSize(0)
        // TODO: Lukk avklaringsbehovet og gå til neste steg når neste steg er implementert
    }

    @Test
    fun `Skal kunne overstyre rettighetsperioden`() {
        val ident = ident()
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(1))
        val nyStartDato = periode.fom.minusDays(7)


        // Oppretter vanlig søknad
        hendelsesMottak.håndtere(
            ident, DokumentMottattPersonHendelse(
                journalpost = JournalpostId("10212345"),
                mottattTidspunkt = LocalDateTime.now(),
                strukturertDokument = StrukturertDokument(
                    SøknadV0(
                        student = SøknadStudentDto("NEI"), yrkesskade = "NEI", oppgitteBarn = null,
                        medlemskap = SøknadMedlemskapDto(
                            "JA", "JA", "NEI", null, emptyList()
                        ),
                    ),
                ),
                periode = periode
            )
        )

        util.ventPåSvar()

        val sak = hentSak(ident, periode)
        val behandling = hentBehandling(sak.id)

        løsAvklaringsBehov(
            behandling = behandling,
            avklaringsBehovLøsning = VurderRettighetsperiodeLøsning(
                rettighetsperiodeVurdering = RettighetsperiodeVurderingDTO(
                    startDato = nyStartDato,
                    begrunnelse = "En begrunnelse",
                    harRettUtoverSøknadsdato = true,
                    harKravPåRenter = false,
                )
            )
        )

        val oppdatertSak = hentSak(ident, periode)

        assertThat(oppdatertSak.rettighetsperiode).isNotEqualTo(periode)
        assertThat(oppdatertSak.rettighetsperiode).isEqualTo(
            Periode(
                nyStartDato,
                nyStartDato.plusYears(1).minusDays(1)
            )
        )
    }

    /**
     * Løser avklaringsbehov og venter på svar vha [util].
     */
    private fun løsAvklaringsBehov(
        behandling: Behandling,
        avklaringsBehovLøsning: AvklaringsbehovLøsning,
        bruker: Bruker = Bruker("SAKSBEHANDLER"),
        ingenEndringIGruppe: Boolean = false
    ): Behandling {
        dataSource.transaction {
            AvklaringsbehovHendelseHåndterer(
                AvklaringsbehovOrkestrator(postgresRepositoryRegistry.provider(it)),
                AvklaringsbehovRepositoryImpl(it),
                BehandlingRepositoryImpl(it),
            ).håndtere(
                behandling.id, LøsAvklaringsbehovHendelse(
                    løsning = avklaringsBehovLøsning,
                    behandlingVersjon = behandling.versjon,
                    bruker = bruker,
                    ingenEndringIGruppe = ingenEndringIGruppe
                )
            )
        }
        util.ventPåSvar(behandling.sakId.id, behandling.id.id)
        return hentBehandling(behandling.sakId)
    }

    private fun løsSykdom(behandling: Behandling): Behandling {
        return løsAvklaringsBehov(
            behandling,
            AvklarSykdomLøsning(
                sykdomsvurdering = SykdomsvurderingLøsningDto(
                    begrunnelse = "Er syk nok",
                    dokumenterBruktIVurdering = listOf(JournalpostId("123123")),
                    harSkadeSykdomEllerLyte = true,
                    erSkadeSykdomEllerLyteVesentligdel = true,
                    erNedsettelseIArbeidsevneMerEnnHalvparten = true,
                    erNedsettelseIArbeidsevneAvEnVissVarighet = true,
                    erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = null,
                    erArbeidsevnenNedsatt = true,
                    yrkesskadeBegrunnelse = null,
                    vurderingenGjelderFra = null,
                )
            ),
        )
    }

    private fun hentPerson(ident: Ident): Person {
        return dataSource.transaction {
            PersonRepositoryImpl(it).finnEllerOpprett(listOf(ident))
        }
    }

    private fun hentSak(ident: Ident, periode: Periode): Sak {
        return dataSource.transaction { connection ->
            SakRepositoryImpl(connection).finnEllerOpprett(
                PersonRepositoryImpl(connection).finnEllerOpprett(listOf(ident)),
                periode
            )
        }
    }

    private fun opprettBehandling(
        sakId: SakId,
        årsaker: List<Årsak>,
        typeBehandling: TypeBehandling,
        forrigeBehandlingId: BehandlingId?
    ): Behandling {
        return dataSource.transaction { connection ->
            BehandlingRepositoryImpl(connection).opprettBehandling(
                forrigeBehandlingId = forrigeBehandlingId,
                sakId = sakId,
                typeBehandling = typeBehandling,
                årsaker = årsaker
            )
        }
    }

    private fun hentVilkårsresultat(behandlingId: BehandlingId): Vilkårsresultat {
        return dataSource.transaction(readOnly = true) { connection ->
            VilkårsresultatRepositoryImpl(connection).hent(behandlingId)
        }
    }

    private fun hentBehandling(sakId: SakId): Behandling {
        return dataSource.transaction(readOnly = true) { connection ->
            val finnSisteBehandlingFor = BehandlingRepositoryImpl(connection).finnSisteBehandlingFor(
                sakId,
                listOf(TypeBehandling.Førstegangsbehandling, TypeBehandling.Revurdering, TypeBehandling.Klage)
            )
            requireNotNull(finnSisteBehandlingFor)
        }
    }

    private fun hentVedtak(behandlingId: BehandlingId): Vedtak {
        return dataSource.transaction(readOnly = true) { connection ->
            val vedtak = VedtakRepositoryImpl(connection).hent(behandlingId)
            requireNotNull(vedtak)
        }
    }

    private fun hentStegHistorikk(behandlingId: BehandlingId): List<StegTilstand> {
        return dataSource.transaction(readOnly = true) { connection ->
            BehandlingRepositoryImpl(connection).hentStegHistorikk(behandlingId)
        }
    }

    private fun hentÅpneAvklaringsbehov(behandlingId: BehandlingId): List<Avklaringsbehov> {
        return dataSource.transaction(readOnly = true) {
            AvklaringsbehovRepositoryImpl(it).hentAvklaringsbehovene(
                behandlingId
            ).åpne()
        }
    }

    private fun hentAvklaringsbehov(behandlingId: BehandlingId, connection: DBConnection): Avklaringsbehovene {
        return AvklaringsbehovRepositoryImpl(connection).hentAvklaringsbehovene(behandlingId)
    }

    private fun hentAlleAvklaringsbehov(behandling: Behandling): List<Avklaringsbehov> {
        return dataSource.transaction(readOnly = true) {
            AvklaringsbehovRepositoryImpl(it).hentAvklaringsbehovene(
                behandling.id
            ).alle()
        }
    }

    private fun sendInnDokument(
        ident: Ident,
        dokumentMottattPersonHendelse: DokumentMottattPersonHendelse
    ): Behandling {
        hendelsesMottak.håndtere(ident, dokumentMottattPersonHendelse)
        util.ventPåSvar()
        val sak = hentSak(ident, dokumentMottattPersonHendelse.periode)
        val behandling = hentBehandling(sak.id)
        return behandling
    }

    private fun hentBrevAvType(behandling: Behandling, typeBrev: TypeBrev) =
        dataSource.transaction(readOnly = true) {
            BrevbestillingRepositoryImpl(it).hent(behandling.id)
                .first { it.typeBrev == typeBrev }
        }

    private fun løsFramTilForutgåendeMedlemskap(
        behandling: Behandling,
        harYrkesskade: Boolean = false,
    ) {
        var behandling = behandling
        behandling = løsSykdom(behandling)
        behandling = løsAvklaringsBehov(
            behandling,
            AvklarBistandsbehovLøsning(
                bistandsVurdering = BistandVurderingLøsningDto(
                    begrunnelse = "Trenger hjelp fra nav",
                    erBehovForAktivBehandling = true,
                    erBehovForArbeidsrettetTiltak = false,
                    erBehovForAnnenOppfølging = null,
                    skalVurdereAapIOvergangTilUføre = null,
                    skalVurdereAapIOvergangTilArbeid = null,
                    overgangBegrunnelse = null
                ),
            ),
        )

        behandling = løsAvklaringsBehov(
            behandling,
            RefusjonkravLøsning(
                RefusjonkravVurdering(
                    harKrav = true,
                    fom = LocalDate.now(),
                    tom = null
                )
            )
        )

        behandling = kvalitetssikre(behandling)

        if (harYrkesskade) {
            behandling = løsAvklaringsBehov(
                behandling,
                AvklarYrkesskadeLøsning(
                    yrkesskadesvurdering = YrkesskadevurderingDto(
                        begrunnelse = "",
                        relevanteSaker = listOf(),
                        andelAvNedsettelsen = null,
                        erÅrsakssammenheng = true
                    )
                ),
            )
        }

        løsAvklaringsBehov(
            behandling,
            FastsettBeregningstidspunktLøsning(
                beregningVurdering = BeregningstidspunktVurdering(
                    begrunnelse = "Trenger hjelp fra Nav",
                    nedsattArbeidsevneDato = LocalDate.now(),
                    ytterligereNedsattArbeidsevneDato = null,
                    ytterligereNedsattBegrunnelse = null
                ),
            ),
        )
    }

    private fun kvalitetssikre(
        behandling: Behandling
    ): Behandling {
        val alleAvklaringsbehov = hentAlleAvklaringsbehov(behandling)
        return løsAvklaringsBehov(
            behandling,
            KvalitetssikringLøsning(alleAvklaringsbehov.filter { behov -> behov.erTotrinn() }.map { behov ->
                TotrinnsVurdering(
                    behov.definisjon.kode, true, "begrunnelse", emptyList()
                )
            }),
        )
    }

    private fun fattVedtak(behandling: Behandling): Behandling = løsAvklaringsBehov(
        behandling,
        FatteVedtakLøsning(hentAlleAvklaringsbehov(behandling).filter { behov -> behov.erTotrinn() }.map { behov ->
            TotrinnsVurdering(
                behov.definisjon.kode, true, "begrunnelse", null
            )
        }),
        Bruker("BESLUTTER")
    )


    private fun nyPerson(
        harYrkesskade: Boolean,
        harUtenlandskOpphold: Boolean,
    ): Ident {
        val ident = ident()
        val person = TestPerson(
            identer = setOf(ident),
            statsborgerskap = if (harUtenlandskOpphold) listOf(
                PdlStatsborgerskap(
                    "MAC",
                    LocalDate.now().minusYears(5),
                    LocalDate.now()
                )
            )
            else listOf(PdlStatsborgerskap("NOR", LocalDate.now().minusYears(5), LocalDate.now())),
            yrkesskade = if (harYrkesskade) listOf(TestYrkesskade()) else emptyList(),
            personStatus = if (!harUtenlandskOpphold) listOf(
                PdlFolkeregisterPersonStatus(
                    PersonStatus.bosatt,
                    PdlFolkeregistermetadata(
                        LocalDateTime.now(),
                        LocalDateTime.now().plusYears(2)
                    )
                )
            ) else listOf(
                PdlFolkeregisterPersonStatus(
                    PersonStatus.bosatt,
                    PdlFolkeregistermetadata(
                        LocalDateTime.now(),
                        LocalDateTime.now().plusYears(2)
                    )
                ),
                PdlFolkeregisterPersonStatus(
                    PersonStatus.ikkeBosatt,
                    PdlFolkeregistermetadata(
                        LocalDateTime.now().minusYears(5),
                        LocalDateTime.now().minusYears(2)
                    )
                )
            )
        )
        FakePersoner.leggTil(person)
        return ident
    }

    private fun vedtaksbrevLøsning(brevbestillingReferanse: UUID): AvklaringsbehovLøsning {
        return SkrivVedtaksbrevLøsning(
            brevbestillingReferanse = brevbestillingReferanse,
            handling = SkrivBrevAvklaringsbehovLøsning.Handling.FERDIGSTILL
        )
    }

    private fun leggTilÅrsakForBehandling(behandling: Behandling, årsaker: List<Årsak>) {
        dataSource.transaction { connection ->
            SakOgBehandlingService(postgresRepositoryRegistry.provider(connection))
                .finnEllerOpprettBehandling(behandling.sakId, årsaker)
        }
        prosesserBehandling(behandling)
    }

    private fun prosesserBehandling(behandling: Behandling) {
        dataSource.transaction { connection ->
            FlytOrkestrator(postgresRepositoryRegistry.provider(connection)).forberedOgProsesserBehandling(
                FlytKontekst(
                    sakId = behandling.sakId,
                    behandlingId = behandling.id,
                    forrigeBehandlingId = behandling.forrigeBehandlingId,
                    behandlingType = behandling.typeBehandling(),
                )
            )
        }
    }
}
