package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.SYSTEMBRUKER
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehov
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovHendelseHåndterer
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovOrkestrator
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.LøsAvklaringsbehovHendelse
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.vedtak.TotrinnsVurdering
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarBistandsbehovLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarForutgåendeMedlemskapLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarLovvalgMedlemskapLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarManuellInntektVurderingLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarOppholdskravLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarOvergangArbeidLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarOvergangUføreLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarSamordningGraderingLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarSykdomLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarYrkesskadeLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklaringsbehovLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.FastsettBeregningstidspunktLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.FatteVedtakLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.ForeslåVedtakLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.KvalitetssikringLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.RefusjonkravLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.SkrivBrevAvklaringsbehovLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.SkrivVedtaksbrevLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.SykdomsvurderingForBrevLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.VurderRettighetsperiodeLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.YrkesskadevurderingDto
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.TypeBrev
import no.nav.aap.behandlingsflyt.behandling.mellomlagring.MellomlagretVurdering
import no.nav.aap.behandlingsflyt.behandling.oppholdskrav.AvklarOppholdkravLøsningPeriodeDto
import no.nav.aap.behandlingsflyt.behandling.oppholdskrav.AvklarOppholdskravLøsningDto

import no.nav.aap.behandlingsflyt.behandling.vedtak.Vedtak
import no.nav.aap.behandlingsflyt.behandling.vilkår.medlemskap.EØSLand
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravNavn
import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.LovvalgVedSøknadsTidspunktDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.ManuellVurderingForForutgåendeMedlemskapDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.ManuellVurderingForLovvalgMedlemskapDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.MedlemskapVedSøknadsTidspunktDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektPerÅr
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningstidspunktVurderingDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.ManuellInntektVurderingDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.flate.BistandVurderingLøsningDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangarbeid.flate.OvergangArbeidVurderingLøsningDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangufore.flate.OvergangUføreVurderingLøsningDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.refusjonkrav.RefusjonkravVurderingDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.rettighetsperiode.RettighetsperiodeVurderingDTO
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.samordning.VurderingerForSamordning
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.flate.SykdomsvurderingLøsningDto
import no.nav.aap.behandlingsflyt.hendelse.mottak.BehandlingSattPåVent
import no.nav.aap.behandlingsflyt.hendelse.mottak.MottattHendelseService
import no.nav.aap.behandlingsflyt.integrasjon.aordning.InntektkomponentenGatewayImpl
import no.nav.aap.behandlingsflyt.integrasjon.arbeidsforhold.AARegisterGateway
import no.nav.aap.behandlingsflyt.integrasjon.arbeidsforhold.EREGGateway
import no.nav.aap.behandlingsflyt.integrasjon.brev.BrevGateway
import no.nav.aap.behandlingsflyt.integrasjon.createGatewayProvider
import no.nav.aap.behandlingsflyt.integrasjon.datadeling.SamGatewayImpl
import no.nav.aap.behandlingsflyt.integrasjon.dokumentinnhenting.DokumentinnhentingGatewayImpl
import no.nav.aap.behandlingsflyt.integrasjon.ident.PdlIdentGateway
import no.nav.aap.behandlingsflyt.integrasjon.ident.PdlPersoninfoBulkGateway
import no.nav.aap.behandlingsflyt.integrasjon.ident.PdlPersoninfoGateway
import no.nav.aap.behandlingsflyt.integrasjon.inntekt.InntektGatewayImpl
import no.nav.aap.behandlingsflyt.integrasjon.institusjonsopphold.InstitusjonsoppholdGatewayImpl
import no.nav.aap.behandlingsflyt.integrasjon.kabal.KabalGateway
import no.nav.aap.behandlingsflyt.integrasjon.medlemsskap.MedlemskapGateway
import no.nav.aap.behandlingsflyt.integrasjon.meldekort.MeldekortGatewayImpl
import no.nav.aap.behandlingsflyt.integrasjon.oppgave.GosysGateway
import no.nav.aap.behandlingsflyt.integrasjon.oppgave.OppgavestyringGatewayImpl
import no.nav.aap.behandlingsflyt.integrasjon.organisasjon.NomInfoGateway
import no.nav.aap.behandlingsflyt.integrasjon.organisasjon.NorgGateway
import no.nav.aap.behandlingsflyt.integrasjon.pdl.PdlBarnGateway
import no.nav.aap.behandlingsflyt.integrasjon.pdl.PdlFolkeregisterPersonStatus
import no.nav.aap.behandlingsflyt.integrasjon.pdl.PdlFolkeregistermetadata
import no.nav.aap.behandlingsflyt.integrasjon.pdl.PdlPersonopplysningGateway
import no.nav.aap.behandlingsflyt.integrasjon.pdl.PdlStatsborgerskap
import no.nav.aap.behandlingsflyt.integrasjon.pdl.PersonStatus
import no.nav.aap.behandlingsflyt.integrasjon.samordning.AbakusForeldrepengerGateway
import no.nav.aap.behandlingsflyt.integrasjon.samordning.AbakusSykepengerGateway
import no.nav.aap.behandlingsflyt.integrasjon.samordning.TjenestePensjonGatewayImpl
import no.nav.aap.behandlingsflyt.integrasjon.statistikk.StatistikkGatewayImpl
import no.nav.aap.behandlingsflyt.integrasjon.ufore.UføreGateway
import no.nav.aap.behandlingsflyt.integrasjon.utbetaling.UtbetalingGatewayImpl
import no.nav.aap.behandlingsflyt.integrasjon.yrkesskade.YrkesskadeRegisterGatewayImpl
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.KabalHendelseId
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.ArbeidIPeriodeV0
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Innsending
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.KabalHendelseV0
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Klage
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.ManuellRevurderingV0
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.ManueltOppgittBarn
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.MeldekortV0
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Melding
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.NyÅrsakTilBehandlingV0
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.OppfølgingsoppgaveV0
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.OppgitteBarn
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Søknad
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.SøknadMedlemskapDto
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.SøknadStudentDto
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.SøknadV0
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov
import no.nav.aap.behandlingsflyt.prosessering.ProsesseringsJobber
import no.nav.aap.behandlingsflyt.repository.avklaringsbehov.AvklaringsbehovRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.behandling.brev.bestilling.BrevbestillingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.behandling.mellomlagring.MellomlagretVurderingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.behandling.vedtak.VedtakRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.underveis.UnderveisRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.postgresRepositoryRegistry
import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.StegTilstand
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekst
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.test.FakeApiInternGateway
import no.nav.aap.behandlingsflyt.test.FakePersoner
import no.nav.aap.behandlingsflyt.test.Fakes
import no.nav.aap.behandlingsflyt.test.ident
import no.nav.aap.behandlingsflyt.test.modell.TestPerson
import no.nav.aap.behandlingsflyt.test.modell.TestYrkesskade
import no.nav.aap.behandlingsflyt.test.modell.defaultInntekt
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDatabase
import no.nav.aap.komponenter.dbtest.TestDatabaseExtension
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.motor.testutil.ManuellMotorImpl
import no.nav.aap.verdityper.dokument.JournalpostId
import no.nav.aap.verdityper.dokument.Kanal
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import javax.sql.DataSource
import kotlin.reflect.KClass


@Fakes
@ExtendWith(TestDatabaseExtension::class)
open class AbstraktFlytOrkestratorTest(unleashGateway: KClass<out UnleashGateway>) {
    @TestDatabase
    lateinit var dataSource: DataSource

    protected val motor by lazy {
        ManuellMotorImpl(
            dataSource,
            jobber = ProsesseringsJobber.alle(),
            repositoryRegistry = postgresRepositoryRegistry,
            gatewayProvider = gatewayProvider
        )
    }

    protected val gatewayProvider = createGatewayProvider {
        register<PdlBarnGateway>()
        register<PdlIdentGateway>()
        register<PdlPersoninfoBulkGateway>()
        register<PdlPersoninfoGateway>()
        register<PdlPersonopplysningGateway>()
        register<AbakusSykepengerGateway>()
        register<AbakusForeldrepengerGateway>()
        register<DokumentinnhentingGatewayImpl>()
        register<MedlemskapGateway>()
        register<FakeApiInternGateway>()
        register<UtbetalingGatewayImpl>()
        register<AARegisterGateway>()
        register<EREGGateway>()
        register<StatistikkGatewayImpl>()
        register<InntektGatewayImpl>()
        register<InstitusjonsoppholdGatewayImpl>()
        register<InntektkomponentenGatewayImpl>()
        register<BrevGateway>()
        register<OppgavestyringGatewayImpl>()
        register<UføreGateway>()
        register<YrkesskadeRegisterGatewayImpl>()
        register<MeldekortGatewayImpl>()
        register<TjenestePensjonGatewayImpl>()
        register(unleashGateway)
        register<SamGatewayImpl>()
        register<NomInfoGateway>()
        register<KabalGateway>()
        register<NorgGateway>()
        register<GosysGateway>()
    }

    companion object {
        @BeforeAll
        @JvmStatic
        internal fun beforeAll() {
            System.setProperty("NAIS_CLUSTER_NAME", "LOCAL")
        }
    }

    private var nesteJournalpostId = (300..1000000)
        .asSequence()
        .map { JournalpostId(it.toString()) }
        .iterator()

    protected fun journalpostId() = nesteJournalpostId.next()

    @BeforeEach
    fun beforeEachClearDatabase() {
        dataSource.connection.use { conn ->
            conn.prepareStatement(
                """
                DO $$
                DECLARE
                    r RECORD;
                BEGIN
                    FOR r IN (SELECT tablename FROM pg_tables WHERE schemaname = 'public' AND tablename NOT LIKE 'flyway_%') LOOP
                        EXECUTE 'TRUNCATE TABLE public.' || quote_ident(r.tablename) || ' RESTART IDENTITY CASCADE';
                    END LOOP;
                END;
                $$;
            """.trimIndent()
            ).use { it.execute() }
        }
    }

    object TestPersoner {
        val STANDARD_PERSON = {
            FakePersoner.leggTil(
                TestPerson(
                    fødselsdato = Fødselsdato(LocalDate.now().minusYears(20)),
                    yrkesskade = emptyList(),
                    sykepenger = emptyList()
                )
            )
        }

        val PERSON_MED_YRKESSKADE = {
            FakePersoner.leggTil(
                TestPerson(
                    fødselsdato = Fødselsdato(LocalDate.now().minusYears(25)),
                    yrkesskade = listOf(TestYrkesskade()),
                )
            )
        }

        val PERSON_FOR_UNG = {
            FakePersoner.leggTil(
                TestPerson(
                    fødselsdato = Fødselsdato(LocalDate.now().minusYears(17))
                )
            )
        }
    }

    object TestSøknader {
        val STANDARD_SØKNAD = SøknadV0(
            student = SøknadStudentDto("NEI"), yrkesskade = "NEI", oppgitteBarn = null,
            medlemskap = SøknadMedlemskapDto("JA", "JA", "NEI", "NEI", null)
        )

        val SØKNAD_STUDENT = SøknadV0(
            student = SøknadStudentDto("JA", "JA"),
            yrkesskade = "JA",
            oppgitteBarn = null,
            medlemskap = SøknadMedlemskapDto("JA", "NEI", "NEI", "NEI", null)
        )

        val SØKNAD_MED_BARN: (List<Pair<String, LocalDate>>) -> SøknadV0 = { barn ->
            SøknadV0(
                student = null,
                yrkesskade = "NEI",
                oppgitteBarn = OppgitteBarn(
                    identer = emptySet(),
                    barn = barn.map { (navn, fødseldato) ->
                        ManueltOppgittBarn(
                            navn = navn,
                            fødselsdato = fødseldato,
                            ident = null,
                            relasjon = ManueltOppgittBarn.Relasjon.FOSTERFORELDER
                        )
                    }
                ),
                medlemskap = SøknadMedlemskapDto("JA", "NEI", "NEI", "NEI", null)
            )
        }
    }


    fun happyCaseFørstegangsbehandling(
        fom: LocalDate = LocalDate.now().minusMonths(3),
        person: TestPerson = TestPersoner.STANDARD_PERSON(),
        periode: Periode = Periode(fom, fom.plusYears(3))
    ): Sak {
        // Sender inn en søknad
        var (sak, behandling) = sendInnFørsteSøknad(
            person = person,
            mottattTidspunkt = fom.atStartOfDay(),
            periode = periode,
        )

        assertThat(behandling.typeBehandling()).isEqualTo(TypeBehandling.Førstegangsbehandling)
        behandling = behandling.medKontekst {
            assertThat(åpneAvklaringsbehov).isNotEmpty()
            assertThat(behandling.status()).isEqualTo(Status.UTREDES)
        }
            .løsSykdom()
            .løsAvklaringsBehov(
                AvklarBistandsbehovLøsning(
                    bistandsVurdering = BistandVurderingLøsningDto(
                        begrunnelse = "Trenger hjelp fra nav",
                        erBehovForAktivBehandling = true,
                        erBehovForArbeidsrettetTiltak = false,
                        erBehovForAnnenOppfølging = null,
                        skalVurdereAapIOvergangTilArbeid = null,
                        overgangBegrunnelse = null,
                        skalVurdereAapIOvergangTilUføre = null,
                    ),
                )
            ).løsAvklaringsBehov(
                RefusjonkravLøsning(
                    listOf(
                        RefusjonkravVurderingDto(
                            harKrav = true,
                            fom = LocalDate.now(),
                            tom = null,
                            navKontor = "",
                        )
                    )
                )
            )
            .løsSykdomsvurderingBrev()

        sak.sendInnMeldekort(
            journalpostId = JournalpostId("220"),
            mottattTidspunkt = fom.atStartOfDay(),
            timerArbeidet = sak.rettighetsperiode.dager()
                .associateWith { 0.0 }
        )

        behandling = behandling.kvalitetssikreOk()
            .løsAvklaringsBehov(
                FastsettBeregningstidspunktLøsning(
                    beregningVurdering = BeregningstidspunktVurderingDto(
                        begrunnelse = "Trenger hjelp fra Nav",
                        nedsattArbeidsevneDato = LocalDate.now(),
                        ytterligereNedsattArbeidsevneDato = null,
                        ytterligereNedsattBegrunnelse = null
                    ),
                ),
            )
            .løsForutgåendeMedlemskap()
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .fattVedtakEllerSendRetur()

        val vedtak = hentVedtak(behandling.id)
        assertThat(vedtak.vedtakstidspunkt.toLocalDate()).isToday

        behandling = behandling.løsVedtaksbrev()

        // Henter vurder alder-vilkår
        // Assert utfall
        val vilkårsresultat = hentVilkårsresultat(behandlingId = behandling.id)
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

        return hentSak(behandling)
    }

    fun revurdereFramTilOgMedSykdom(sak: Sak, gjelderFra: LocalDate, vissVarighet: Boolean? = null): Behandling {
        return sak.sendInnSøknad(
            journalpostId = JournalpostId("29"),
            mottattTidspunkt = gjelderFra.atStartOfDay(),
            søknad = SøknadV0(
                student = SøknadStudentDto("NEI"),
                yrkesskade = "NEI",
                oppgitteBarn = null,
                medlemskap = SøknadMedlemskapDto("JA", "NEI", "NEI", "NEI", null)
            ),
        )
            .medKontekst {
                assertThat(this.behandling.typeBehandling()).isEqualTo(TypeBehandling.Revurdering)
                assertThat(this.behandling.status()).isEqualTo(Status.UTREDES)
            }
            .løsAvklaringsBehov(
                AvklarSykdomLøsning(
                    sykdomsvurderinger = listOf(
                        SykdomsvurderingLøsningDto(
                            begrunnelse = "Er syk nok",
                            dokumenterBruktIVurdering = listOf(JournalpostId("1349532")),
                            harSkadeSykdomEllerLyte = true,
                            erSkadeSykdomEllerLyteVesentligdel = true,
                            erNedsettelseIArbeidsevneMerEnnHalvparten = true,
                            erNedsettelseIArbeidsevneAvEnVissVarighet = vissVarighet,
                            erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = null,
                            erArbeidsevnenNedsatt = true,
                            yrkesskadeBegrunnelse = null,
                            // Ny revurdering
                            vurderingenGjelderFra = gjelderFra
                        )
                    )
                ),
            )
            .medKontekst {
                assertThat(this.behandling.status()).isEqualTo(Status.UTREDES)
            }
    }


    /**
     * Løser avklaringsbehov og venter på svar.
     */
    protected fun løsAvklaringsBehov(
        behandling: Behandling,
        avklaringsBehovLøsning: AvklaringsbehovLøsning,
        bruker: Bruker = Bruker("SAKSBEHANDLER"),
        ingenEndringIGruppe: Boolean = false
    ): Behandling {
        dataSource.transaction {
            AvklaringsbehovHendelseHåndterer(
                AvklaringsbehovOrkestrator(postgresRepositoryRegistry.provider(it), gatewayProvider),
                AvklaringsbehovRepositoryImpl(it),
                BehandlingRepositoryImpl(it),
                MellomlagretVurderingRepositoryImpl(it),
            ).håndtere(
                behandling.id, LøsAvklaringsbehovHendelse(
                    løsning = avklaringsBehovLøsning,
                    behandlingVersjon = behandling.versjon,
                    bruker = bruker,
                    ingenEndringIGruppe = ingenEndringIGruppe
                )
            )
        }
        motor.kjørJobber()
        return hentBehandling(behandling.referanse)
    }

    @JvmName("løsAvklaringsBehovExt")
    protected fun Behandling.løsAvklaringsBehov(
        avklaringsBehovLøsning: AvklaringsbehovLøsning,
        bruker: Bruker = Bruker("SAKSBEHANDLER"),
        ingenEndringIGruppe: Boolean = false
    ): Behandling {
        return løsAvklaringsBehov(
            this,
            avklaringsBehovLøsning = avklaringsBehovLøsning,
            bruker = bruker,
            ingenEndringIGruppe = ingenEndringIGruppe
        )
    }

    protected fun løsFramTilGrunnlag(behandling: Behandling) {
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
                    skalVurdereAapIOvergangTilArbeid = null,
                    overgangBegrunnelse = null,
                    skalVurdereAapIOvergangTilUføre = null,

                    ),
            ),
        )

        behandling = løsAvklaringsBehov(
            behandling,
            RefusjonkravLøsning(
                listOf(
                    RefusjonkravVurderingDto(
                        harKrav = true,
                        fom = LocalDate.now(),
                        tom = null,
                        navKontor = "",
                    )
                )
            )
        )

        behandling = løsSykdomsvurderingBrev(behandling)

        kvalitetssikreOk(behandling)
    }

    protected fun mellomlagreSykdom(behandling: Behandling): Behandling {
        dataSource.transaction { connection ->
            MellomlagretVurderingRepositoryImpl(connection).lagre(
                MellomlagretVurdering(
                    behandlingId = behandling.id,
                    avklaringsbehovKode = AvklaringsbehovKode.`5003`,
                    data = """{"test": "testverdi"}""",
                    vurdertAv = "A123456",
                    vurdertDato = LocalDateTime.now().withNano(0),
                )
            )
        }
        return behandling
    }

    protected fun løsSykdom(behandling: Behandling): Behandling {
        return løsAvklaringsBehov(
            behandling,
            AvklarSykdomLøsning(
                sykdomsvurderinger = listOf(
                    SykdomsvurderingLøsningDto(
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
                )
            ),
        )
    }

    protected fun Behandling.løsBistand(): Behandling {
        return this.løsAvklaringsBehov(
            AvklarBistandsbehovLøsning(
                BistandVurderingLøsningDto(
                    begrunnelse = "Trenger hjelp fra nav",
                    erBehovForAktivBehandling = true,
                    erBehovForArbeidsrettetTiltak = false,
                    erBehovForAnnenOppfølging = null,
                    skalVurdereAapIOvergangTilArbeid = null,
                    overgangBegrunnelse = null,
                    skalVurdereAapIOvergangTilUføre = null,
                )
            )
        )
    }


    @JvmName("løsOvergangUføreExt")
    protected fun Behandling.løsOvergangUføre(): Behandling {
        return løsOvergangUføre(this)
    }

    protected fun løsOvergangUføre(behandling: Behandling): Behandling {
        return løsAvklaringsBehov(
            behandling = behandling,
            avklaringsBehovLøsning =
                AvklarOvergangUføreLøsning(
                    OvergangUføreVurderingLøsningDto(
                        begrunnelse = "Løsning",
                        brukerHarSøktOmUføretrygd = false,
                        brukerHarFåttVedtakOmUføretrygd = null,
                        brukerRettPåAAP = false,
                        virkningsdato = null,
                        overgangBegrunnelse = null
                    )
                )
        )
    }

    protected fun Behandling.løsOppholdskrav(): Behandling {
        return løsAvklaringsBehov(
            behandling = this,
            avklaringsBehovLøsning =
                AvklarOppholdskravLøsning(
                    vurdering = AvklarOppholdskravLøsningDto(
                        perioder = listOf(
                            AvklarOppholdkravLøsningPeriodeDto(
                                oppfylt = true,
                                land = "SE:Sverige",
                                fom = LocalDate.now(),
                                begrunnelse = "Fiske"

                            )
                        )
                    )
                )
        )
    }

    protected fun Behandling.løsRettighetsperiode(dato: LocalDate): Behandling {
        return this.løsAvklaringsBehov(
            avklaringsBehovLøsning = VurderRettighetsperiodeLøsning(
                rettighetsperiodeVurdering = RettighetsperiodeVurderingDTO(
                    startDato = dato,
                    begrunnelse = "En begrunnelse",
                    harRettUtoverSøknadsdato = true,
                    harKravPåRenter = false,
                )
            )
        )
    }

    protected fun Behandling.løsFastsettManuellInntekt(): Behandling {
        return this.løsAvklaringsBehov(
            AvklarManuellInntektVurderingLøsning(
                manuellVurderingForManglendeInntekt = ManuellInntektVurderingDto(
                    begrunnelse = "Mangler ligning",
                    belop = BigDecimal(300000),
                )
            )
        )
    }

    protected fun Behandling.løsUtenSamordning(): Behandling {
        return this.løsAvklaringsBehov(
            AvklarSamordningGraderingLøsning(
                vurderingerForSamordning = VurderingerForSamordning("", true, null, emptyList())
            )
        )
    }

    protected fun løsSykdomsvurderingBrev(behandling: Behandling): Behandling {
        return løsAvklaringsBehov(
            behandling = behandling,
            avklaringsBehovLøsning = SykdomsvurderingForBrevLøsning(
                vurdering = "Denne vurderingen skal vises i brev"
            )
        )
    }

    @JvmName("løsOvergangArbeidExt")
    protected fun Behandling.løsOvergangArbeid(): Behandling {
        return løsOvergangArbeid(this)
    }

    protected fun løsOvergangArbeid(behandling: Behandling): Behandling {
        return løsAvklaringsBehov(
            behandling = behandling,
            avklaringsBehovLøsning =
                AvklarOvergangArbeidLøsning(
                    OvergangArbeidVurderingLøsningDto(
                        begrunnelse = "Løsning",
                        brukerRettPåAAP = true,
                        virkningsdato = null,
                        overgangBegrunnelse = null
                    )
                )
        )
    }

    @JvmName("løsSykdomsvurderingBrevExt")
    protected fun Behandling.løsSykdomsvurderingBrev(): Behandling {
        return løsSykdomsvurderingBrev(this)
    }

    @JvmName("løsSykdomExt")
    protected fun Behandling.løsSykdom(): Behandling {
        return løsSykdom(this)
    }

    @JvmName("mellomlagreSykdomExt")
    protected fun Behandling.mellomlagreSykdom(): Behandling {
        return mellomlagreSykdom(this)
    }

    protected fun hentSak(ident: Ident, periode: Periode): Sak {
        return dataSource.transaction { connection ->
            SakRepositoryImpl(connection).finnEllerOpprett(
                PersonRepositoryImpl(connection).finnEllerOpprett(listOf(ident)),
                periode
            )
        }
    }

    protected fun hentSak(behandling: Behandling): Sak {
        return dataSource.transaction { connection ->
            SakRepositoryImpl(connection).hent(behandling.sakId)
        }
    }

    protected fun hentSak(saksnummer: Saksnummer): Sak {
        return dataSource.transaction { connection ->
            SakRepositoryImpl(connection).hent(saksnummer)
        }
    }

    protected fun opprettBehandling(
        sakId: SakId,
        vurderingsbehov: List<VurderingsbehovMedPeriode>,
        typeBehandling: TypeBehandling,
        forrigeBehandlingId: BehandlingId?
    ): Behandling {
        return dataSource.transaction { connection ->
            BehandlingRepositoryImpl(connection).opprettBehandling(
                forrigeBehandlingId = forrigeBehandlingId,
                sakId = sakId,
                typeBehandling = typeBehandling,
                vurderingsbehovOgÅrsak = VurderingsbehovOgÅrsak(
                    vurderingsbehov = vurderingsbehov,
                    årsak = ÅrsakTilOpprettelse.SØKNAD
                )
            )
        }
    }

    protected fun hentVilkårsresultat(behandlingId: BehandlingId): Vilkårsresultat {
        return dataSource.transaction(readOnly = true) { connection ->
            VilkårsresultatRepositoryImpl(connection).hent(behandlingId)
        }
    }

    /**
     * Denne må brukes med omhu, da siste opprettede behandling ikke nødvendigvis er siste behandling
     * i den lenkede listen av behandlinger. Ref. fasttrack/atomære behandlinger
     *
     * Bruk i stedet: [SakOgBehandlingService.finnSisteYtelsesbehandlingFor]
     */
    protected fun hentSisteOpprettedeBehandlingForSak(
        sakId: SakId,
        typeBehandling: List<TypeBehandling> = TypeBehandling.entries
    ): Behandling {
        return dataSource.transaction(readOnly = true) { connection ->
            val finnSisteBehandlingFor = BehandlingRepositoryImpl(connection).finnSisteOpprettedeBehandlingFor(
                sakId,
                typeBehandling
            )
            requireNotNull(finnSisteBehandlingFor)
        }
    }

    /**
     * Denne må brukes med omhu, da siste opprettede behandling ikke nødvendigvis er siste behandling
     * i den lenkede listen av behandlinger. Ref. fasttrack/atomære behandlinger
     *
     * Bruk i stedet: [SakOgBehandlingService.finnSisteYtelsesbehandlingFor]
     */
    protected fun hentSisteOpprettedeBehandlingForSak(
        saksnummer: Saksnummer,
        typeBehandling: List<TypeBehandling> = TypeBehandling.entries
    ): Behandling {
        return hentSisteOpprettedeBehandlingForSak(hentSak(saksnummer).id, typeBehandling)
    }

    protected fun hentBehandling(behandlingReferanse: BehandlingReferanse): Behandling {
        return dataSource.transaction(readOnly = true) { connection ->
            val behandling = BehandlingRepositoryImpl(connection).hent(behandlingReferanse)
            requireNotNull(behandling)
        }
    }

    protected fun hentVedtak(behandlingId: BehandlingId): Vedtak {
        return dataSource.transaction(readOnly = true) { connection ->
            val vedtak = VedtakRepositoryImpl(connection).hent(behandlingId)
            requireNotNull(vedtak)
        }
    }

    protected fun hentStegHistorikk(behandlingId: BehandlingId): List<StegTilstand> {
        return dataSource.transaction(readOnly = true) { connection ->
            BehandlingRepositoryImpl(connection).hentStegHistorikk(behandlingId)
        }
    }

    protected fun hentÅpneAvklaringsbehov(behandling: Behandling): List<Avklaringsbehov> {
        return hentÅpneAvklaringsbehov(behandling.id)
    }

    protected fun hentÅpneAvklaringsbehov(behandlingId: BehandlingId): List<Avklaringsbehov> {
        return dataSource.transaction(readOnly = true) {
            AvklaringsbehovRepositoryImpl(it).hentAvklaringsbehovene(
                behandlingId
            ).åpne()
        }
    }

    protected fun hentAlleAvklaringsbehov(behandling: Behandling): List<Avklaringsbehov> {
        return hentAlleAvklaringsbehov(behandling.id)
    }

    protected fun hentAlleAvklaringsbehov(behandlingId: BehandlingId): List<Avklaringsbehov> {
        return dataSource.transaction(readOnly = true) {
            AvklaringsbehovRepositoryImpl(it).hentAvklaringsbehovene(
                behandlingId
            ).alle()
        }
    }

    protected fun sendInnFørsteSøknad(
        søknad: Søknad = TestSøknader.STANDARD_SØKNAD,
        person: TestPerson = TestPersoner.STANDARD_PERSON(),
        mottattTidspunkt: LocalDateTime? = null,
        periode: Periode? = null,
        journalpostId: JournalpostId = journalpostId(),
    ): Pair<Sak, Behandling> {
        val mottattTidspunkt = mottattTidspunkt ?: periode?.fom?.atStartOfDay() ?: LocalDateTime.now().minusMonths(3)
        val periode = periode ?: Periode(mottattTidspunkt.toLocalDate(), mottattTidspunkt.toLocalDate().plusYears(1))
        val sak = dataSource.transaction { connection ->
            SakRepositoryImpl(connection).finnEllerOpprett(
                PersonRepositoryImpl(connection).finnEllerOpprett(listOf(person.aktivIdent())),
                periode
            )
        }

        val behandling = sak.sendInnSøknad(søknad, mottattTidspunkt, journalpostId)

        return Pair(sak, behandling)
    }


    @Deprecated("Bruk opprettSak for førstegangsbehandling eller Sak.sendInnSøknad hvis saken allerede eksisterer")
    protected fun sendInnSøknad(
        ident: Ident,
        periode: Periode,
        søknad: Søknad,
        mottattTidspunkt: LocalDateTime = LocalDateTime.now(),
        journalpostId: JournalpostId = journalpostId(),
    ): Behandling {
        return hentSak(ident, periode).sendInnSøknad(
            søknad,
            mottattTidspunkt,
            journalpostId
        )
    }

    protected fun Sak.sendInnSøknad(
        søknad: Søknad,
        mottattTidspunkt: LocalDateTime = LocalDateTime.now(),
        journalpostId: JournalpostId = journalpostId(),
    ): Behandling {
        sendInn(
            referanse = InnsendingReferanse(journalpostId),
            type = InnsendingType.SØKNAD,
            kanal = Kanal.DIGITAL,
            mottattTidspunkt = mottattTidspunkt,
            melding = søknad,
        )
        return hentSisteOpprettedeBehandlingForSak(id)
    }

    protected fun Sak.opprettManuellRevurdering(
        vurderingsbehov: List<Vurderingsbehov>,
    ): Behandling {
        sendInn(
            referanse = InnsendingReferanse(
                InnsendingReferanse.Type.REVURDERING_ID,
                /* Dette kan da neppe være riktig. Hva slags referanse genereres i disse tilfellene? */
                journalpostId().identifikator,
            ),
            type = InnsendingType.MANUELL_REVURDERING,
            kanal = Kanal.DIGITAL,
            mottattTidspunkt = LocalDateTime.now(),
            melding = ManuellRevurderingV0(
                årsakerTilBehandling = vurderingsbehov, ""
            ),
        )
        return hentSisteOpprettedeBehandlingForSak(id)
    }

    protected fun Sak.sendInnKabalHendelse(
        mottattTidspunkt: LocalDateTime = LocalDateTime.now(),
        kabalHendelse: KabalHendelseV0,
    ): Behandling {
        sendInn(
            referanse = InnsendingReferanse(KabalHendelseId(kabalHendelse.eventId)),
            type = InnsendingType.KABAL_HENDELSE,
            kanal = Kanal.DIGITAL,
            mottattTidspunkt = mottattTidspunkt,
            melding = kabalHendelse,
        )
        return hentSisteOpprettedeBehandlingForSak(saksnummer)
    }

    protected fun Sak.sendInnOppfølgingsoppgave(
        oppfølgingsoppgave: OppfølgingsoppgaveV0,
        mottattTidspunkt: LocalDateTime = LocalDateTime.now(),
    ): Behandling {
        sendInn(
            referanse = InnsendingReferanse(
                InnsendingReferanse.Type.BEHANDLING_REFERANSE,
                UUID.randomUUID().toString(),
            ),
            type = InnsendingType.OPPFØLGINGSOPPGAVE,
            kanal = Kanal.DIGITAL,
            mottattTidspunkt = mottattTidspunkt,
            melding = oppfølgingsoppgave,
        )
        return hentSisteOpprettedeBehandlingForSak(saksnummer)
    }

    protected fun Sak.sendInnKlage(
        journalpostId: JournalpostId = journalpostId(),
        mottattTidspunkt: LocalDateTime,
        klage: Klage,
    ): Behandling {
        sendInn(
            referanse = InnsendingReferanse(journalpostId),
            type = InnsendingType.KLAGE,
            kanal = Kanal.DIGITAL,
            mottattTidspunkt = mottattTidspunkt,
            melding = klage,
        )
        val behandling = hentSisteOpprettedeBehandlingForSak(saksnummer)
        return behandling
    }


    protected fun Sak.sendInnMeldekort(
        journalpostId: JournalpostId,
        timerArbeidet: Map<LocalDate, Double>,
        mottattTidspunkt: LocalDateTime = LocalDateTime.now(),
    ) {
        sendInn(
            referanse = InnsendingReferanse(journalpostId),
            type = InnsendingType.MELDEKORT,
            kanal = Kanal.DIGITAL,
            mottattTidspunkt = mottattTidspunkt,
            melding = MeldekortV0(
                harDuArbeidet = timerArbeidet.any { (_, timer) -> timer > 0.0 },
                timerArbeidPerPeriode = timerArbeidet.map { (dato, timer) ->
                    ArbeidIPeriodeV0(
                        fraOgMedDato = dato,
                        tilOgMedDato = dato,
                        timerArbeid = timer,
                    )
                }
            ),
        )
    }

    protected fun Behandling.leggTilVurderingsbehov(vurderingsbehov: Vurderingsbehov): Behandling {
        return leggTilVurderingsbehov(listOf(vurderingsbehov))
    }

    protected fun Behandling.leggTilVurderingsbehov(vurderingsbehov: List<Vurderingsbehov>): Behandling {
        hentSak(this).leggTilVurderingsbehov(this.referanse, vurderingsbehov)
        return hentBehandling(this.referanse)
    }

    protected fun Sak.leggTilVurderingsbehov(
        behandlingReferanse: BehandlingReferanse,
        vurderingsbehov: List<Vurderingsbehov>
    ) {
        sendInn(
            referanse = InnsendingReferanse(
                type = InnsendingReferanse.Type.SAKSBEHANDLER_KELVIN_REFERANSE,
                verdi = UUID.randomUUID().toString(),
            ),
            mottattTidspunkt = LocalDateTime.now().minusMonths(4),
            type = InnsendingType.NY_ÅRSAK_TIL_BEHANDLING,
            kanal = Kanal.DIGITAL,
            melding = NyÅrsakTilBehandlingV0(
                årsakerTilBehandling = vurderingsbehov,
                behandlingReferanse = behandlingReferanse.toString(),
                null
            ),
        )
    }

    protected fun Sak.sendInn(
        referanse: InnsendingReferanse,
        type: InnsendingType,
        kanal: Kanal,
        mottattTidspunkt: LocalDateTime,
        melding: Melding?,
    ) {
        dataSource.transaction { connection ->
            MottattHendelseService(postgresRepositoryRegistry.provider(connection)).registrerMottattHendelse(
                Innsending(
                    saksnummer = saksnummer,
                    referanse = referanse,
                    type = type,
                    kanal = kanal,
                    mottattTidspunkt = mottattTidspunkt,
                    melding = melding,
                )
            )
        }
        motor.kjørJobber()
    }

    fun settBehandlingPåVent(key: BehandlingId, hendelse: BehandlingSattPåVent) {
        dataSource.transaction { connection ->
            AvklaringsbehovOrkestrator(postgresRepositoryRegistry.provider(connection), gatewayProvider)
                .settBehandlingPåVent(key, hendelse)
        }
    }

    fun bestillLegeerklæring(key: BehandlingId) {
        dataSource.transaction { connection ->
            AvklaringsbehovOrkestrator(postgresRepositoryRegistry.provider(connection), gatewayProvider)
                .settPåVentMensVentePåMedisinskeOpplysninger(key, SYSTEMBRUKER)
        }
    }

    protected fun hentBrevAvType(behandling: Behandling, typeBrev: TypeBrev) =
        dataSource.transaction(readOnly = true) {
            val brev = BrevbestillingRepositoryImpl(it).hent(behandling.id)
            brev.firstOrNull { it.typeBrev == typeBrev }
                ?: error("Ingen brev av type $typeBrev. Følgende finnes: ${brev.joinToString { it.typeBrev.toString() }}")
        }

    protected fun løsForutgåendeMedlemskap(
        behandling: Behandling
    ): Behandling {
        return løsAvklaringsBehov(
            behandling,
            AvklarForutgåendeMedlemskapLøsning(
                ManuellVurderingForForutgåendeMedlemskapDto(
                    begrunnelse = "",
                    harForutgåendeMedlemskap = true,
                    varMedlemMedNedsattArbeidsevne = true,
                    medlemMedUnntakAvMaksFemAar = null
                )
            )
        )
    }

    @JvmName("losForutgaaendeMedlemskapExt")
    protected fun Behandling.løsForutgåendeMedlemskap(): Behandling {
        return løsForutgåendeMedlemskap(this)
    }

    @JvmName("løsFramTilForutgåendeMedlemskapExt")
    protected fun Behandling.løsFramTilForutgåendeMedlemskap(harYrkesskade: Boolean = false): Behandling {
        return løsFramTilForutgåendeMedlemskap(this, harYrkesskade)
    }

    protected fun løsFramTilForutgåendeMedlemskap(
        behandling: Behandling,
        harYrkesskade: Boolean = false,
    ): Behandling {
        var behandling = behandling
        behandling = løsSykdom(behandling)
            .løsAvklaringsBehov(
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
            .løsAvklaringsBehov(
                RefusjonkravLøsning(
                    listOf(
                        RefusjonkravVurderingDto(
                            harKrav = true,
                            fom = LocalDate.now(),
                            tom = null,
                            navKontor = "",
                        )
                    )
                )
            )
            .løsSykdomsvurderingBrev()
            .kvalitetssikreOk()

        if (harYrkesskade) {
            behandling = løsAvklaringsBehov(
                behandling,
                AvklarYrkesskadeLøsning(
                    yrkesskadesvurdering = YrkesskadevurderingDto(
                        begrunnelse = "",
                        relevanteSaker = emptyList(),
                        relevanteYrkesskadeSaker = emptyList(),
                        andelAvNedsettelsen = null,
                        erÅrsakssammenheng = true
                    )
                ),
            )
        }

        return løsAvklaringsBehov(
            behandling,
            FastsettBeregningstidspunktLøsning(
                beregningVurdering = BeregningstidspunktVurderingDto(
                    begrunnelse = "Trenger hjelp fra Nav",
                    nedsattArbeidsevneDato = LocalDate.now(),
                    ytterligereNedsattArbeidsevneDato = null,
                    ytterligereNedsattBegrunnelse = null
                ),
            ),
        )
    }

    protected fun Behandling.løsBeregningstidspunkt(dato: LocalDate = LocalDate.now()): Behandling {
        return løsAvklaringsBehov(
            this,
            FastsettBeregningstidspunktLøsning(
                beregningVurdering = BeregningstidspunktVurderingDto(
                    begrunnelse = "Trenger hjelp fra Nav",
                    nedsattArbeidsevneDato = dato,
                    ytterligereNedsattArbeidsevneDato = null,
                    ytterligereNedsattBegrunnelse = null
                ),
            ),
        )
    }

    protected fun Behandling.løsLovvalg(): Behandling {
        return løsAvklaringsBehov(
            this,
            AvklarLovvalgMedlemskapLøsning(
                ManuellVurderingForLovvalgMedlemskapDto(
                    lovvalgVedSøknadsTidspunkt = LovvalgVedSøknadsTidspunktDto("", EØSLand.NOR),
                    medlemskapVedSøknadsTidspunkt = MedlemskapVedSøknadsTidspunktDto("", true)
                )
            )
        )
    }

    protected fun kvalitetssikreOk(
        behandling: Behandling,
        bruker: Bruker = Bruker("KVALITETSSIKRER")
    ): Behandling {
        val alleAvklaringsbehov = hentAlleAvklaringsbehov(behandling)
        return løsAvklaringsBehov(
            behandling,
            KvalitetssikringLøsning(alleAvklaringsbehov.filter { behov -> behov.erTotrinn() || behov.kreverKvalitetssikring() }
                .map { behov ->
                    TotrinnsVurdering(
                        behov.definisjon.kode, true, "begrunnelse", emptyList()
                    )
                }),
            bruker,
        )
    }

    @JvmName("kvalitetssikreOkExt")
    protected fun Behandling.kvalitetssikreOk(bruker: Bruker = Bruker("KVALITETSSIKRER")): Behandling {
        return kvalitetssikreOk(this, bruker)
    }

    protected fun fattVedtakEllerSendRetur(behandling: Behandling, returVed: Definisjon? = null): Behandling =
        løsAvklaringsBehov(
            behandling,
            FatteVedtakLøsning(
                hentAlleAvklaringsbehov(behandling)
                    .filter { behov -> behov.erTotrinn() }
                    .map { behov ->
                        TotrinnsVurdering(
                            behov.definisjon.kode, behov.definisjon != returVed, "begrunnelse", emptyList()
                        )
                    }),
            Bruker("BESLUTTER")
        )

    protected fun Behandling.løsRefusjonskrav(): Behandling {
        return løsAvklaringsBehov(
            this,
            RefusjonkravLøsning(
                listOf(
                    RefusjonkravVurderingDto(
                        harKrav = true,
                        fom = LocalDate.now(),
                        tom = null,
                        navKontor = "",
                    )
                )
            )
        )
    }

    @JvmName("fattVedtakExt")
    protected fun Behandling.fattVedtakEllerSendRetur(returVed: Definisjon? = null): Behandling {
        return fattVedtakEllerSendRetur(this, returVed)
    }

    class BehandlingInfo(
        val åpneAvklaringsbehov: List<Avklaringsbehov>,
        val behandling: Behandling,
        val ventebehov: List<Avklaringsbehov>
    )

    protected fun Behandling.medKontekst(block: BehandlingInfo.() -> Unit): Behandling {
        val åpneAvklaringsbehov = hentÅpneAvklaringsbehov(this)
        block(
            BehandlingInfo(
                åpneAvklaringsbehov = åpneAvklaringsbehov,
                behandling = this,
                ventebehov = åpneAvklaringsbehov.filter { it.erVentepunkt() })
        )
        return this
    }

    protected fun nyPerson(
        harYrkesskade: Boolean,
        harUtenlandskOpphold: Boolean,
        inntekter: MutableList<InntektPerÅr>? = null
    ): Ident {
        val ident = ident()
        val person = TestPerson(
            identer = setOf(ident),
            inntekter = inntekter ?: defaultInntekt(),
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

    protected fun vedtaksbrevLøsning(brevbestillingReferanse: UUID): AvklaringsbehovLøsning {
        return SkrivVedtaksbrevLøsning(
            brevbestillingReferanse = brevbestillingReferanse,
            handling = SkrivBrevAvklaringsbehovLøsning.Handling.FERDIGSTILL
        )
    }

    protected fun Behandling.løsVedtaksbrev(typeBrev: TypeBrev = TypeBrev.VEDTAK_INNVILGELSE): Behandling {
        val brevbestilling = hentBrevAvType(this, typeBrev)

        return this.løsAvklaringsBehov(vedtaksbrevLøsning(brevbestilling.referanse.brevbestillingReferanse))
    }

    protected fun prosesserBehandling(behandling: Behandling): Behandling {
        dataSource.transaction { connection ->
            FlytOrkestrator(
                postgresRepositoryRegistry.provider(connection),
                gatewayProvider
            ).forberedOgProsesserBehandling(
                FlytKontekst(
                    sakId = behandling.sakId,
                    behandlingId = behandling.id,
                    forrigeBehandlingId = behandling.forrigeBehandlingId,
                    behandlingType = behandling.typeBehandling(),
                ),
            )
        }
        motor.kjørJobber()
        return hentBehandling(behandling.referanse)
    }

    // Sletter tidligere informasjonskrav-oppdateringer for å slippe unna at den ikke skal sjekke på nytt før en time har gått
    protected fun nullstillInformasjonskravOppdatert(informasjonskravnavn: InformasjonskravNavn, sakId: SakId) {
        dataSource.transaction { connection ->
            connection.execute(
                """
                    delete from informasjonskrav_oppdatert where informasjonskrav = ? and sak_id = ?
                """
            ) {
                setParams {
                    setEnumName(1, informasjonskravnavn)
                    setLong(2, sakId.id)
                }
            }
        }
    }
}