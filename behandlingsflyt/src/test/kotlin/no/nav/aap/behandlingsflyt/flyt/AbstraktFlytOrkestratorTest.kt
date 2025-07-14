package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehov
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovHendelseHåndterer
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovOrkestrator
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.LøsAvklaringsbehovHendelse
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.vedtak.TotrinnsVurdering
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarBistandsbehovLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarForutgåendeMedlemskapLøsning
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
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.TypeBrev
import no.nav.aap.behandlingsflyt.behandling.vedtak.Vedtak
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravNavn
import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.StrukturertDokument
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.ManuellVurderingForForutgåendeMedlemskapDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektPerÅr
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningstidspunktVurderingDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.flate.BistandVurderingLøsningDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.refusjonkrav.RefusjonkravVurderingDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.YrkesskadevurderingDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.flate.SykdomsvurderingLøsningDto
import no.nav.aap.behandlingsflyt.flyt.internals.DokumentMottattPersonHendelse
import no.nav.aap.behandlingsflyt.flyt.internals.NyÅrsakTilBehandlingHendelse
import no.nav.aap.behandlingsflyt.flyt.internals.TestHendelsesMottak
import no.nav.aap.behandlingsflyt.integrasjon.arbeidsforhold.AARegisterGateway
import no.nav.aap.behandlingsflyt.integrasjon.arbeidsforhold.EREGGateway
import no.nav.aap.behandlingsflyt.integrasjon.barn.PdlBarnGateway
import no.nav.aap.behandlingsflyt.integrasjon.brev.BrevGateway
import no.nav.aap.behandlingsflyt.integrasjon.datadeling.SamGatewayImpl
import no.nav.aap.behandlingsflyt.integrasjon.dokumentinnhenting.DokumentinnhentingGatewayImpl
import no.nav.aap.behandlingsflyt.integrasjon.ident.PdlIdentGateway
import no.nav.aap.behandlingsflyt.integrasjon.ident.PdlPersoninfoBulkGateway
import no.nav.aap.behandlingsflyt.integrasjon.ident.PdlPersoninfoGateway
import no.nav.aap.behandlingsflyt.integrasjon.inntekt.InntektGatewayImpl
import no.nav.aap.behandlingsflyt.integrasjon.kabal.KabalGateway
import no.nav.aap.behandlingsflyt.integrasjon.medlemsskap.MedlemskapGateway
import no.nav.aap.behandlingsflyt.integrasjon.meldekort.MeldekortGatewayImpl
import no.nav.aap.behandlingsflyt.integrasjon.oppgave.OppgavestyringGatewayImpl
import no.nav.aap.behandlingsflyt.integrasjon.organisasjon.NomInfoGateway
import no.nav.aap.behandlingsflyt.integrasjon.organisasjon.NorgGateway
import no.nav.aap.behandlingsflyt.integrasjon.samordning.AbakusForeldrepengerGateway
import no.nav.aap.behandlingsflyt.integrasjon.samordning.AbakusSykepengerGateway
import no.nav.aap.behandlingsflyt.integrasjon.samordning.TjenestePensjonGatewayImpl
import no.nav.aap.behandlingsflyt.integrasjon.statistikk.StatistikkGatewayImpl
import no.nav.aap.behandlingsflyt.integrasjon.ufore.UføreGateway
import no.nav.aap.behandlingsflyt.integrasjon.utbetaling.UtbetalingGatewayImpl
import no.nav.aap.behandlingsflyt.integrasjon.yrkesskade.YrkesskadeRegisterGatewayImpl
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.ArbeidIPeriodeV0
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.MeldekortV0
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Søknad
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.SøknadMedlemskapDto
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.SøknadStudentDto
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.SøknadV0
import no.nav.aap.behandlingsflyt.prosessering.ProsesseringsJobber
import no.nav.aap.behandlingsflyt.repository.avklaringsbehov.AvklaringsbehovRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.behandling.brev.bestilling.BrevbestillingRepositoryImpl
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
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Årsak
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekst
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
import no.nav.aap.behandlingsflyt.test.modell.defaultInntekt
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.komponenter.gateway.GatewayRegistry
import no.nav.aap.komponenter.httpklient.auth.Bruker
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.motor.Motor
import no.nav.aap.motor.testutil.TestUtil
import no.nav.aap.verdityper.dokument.JournalpostId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import javax.sql.DataSource


@Fakes
abstract class AbstraktFlytOrkestratorTest {
    companion object {
        @JvmStatic
        protected val dataSource = InitTestDatabase.freshDatabase()

        protected val motor =
            Motor(dataSource, 8, jobber = ProsesseringsJobber.alle(), repositoryRegistry = postgresRepositoryRegistry)

        @JvmStatic
        protected val hendelsesMottak = TestHendelsesMottak(dataSource)

        @JvmStatic
        protected val util =
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
                .register<EREGGateway>()
                .register<StatistikkGatewayImpl>()
                .register<InntektGatewayImpl>()
                .register<BrevGateway>()
                .register<OppgavestyringGatewayImpl>()
                .register<UføreGateway>()
                .register<YrkesskadeRegisterGatewayImpl>()
                .register<MeldekortGatewayImpl>()
                .register<TjenestePensjonGatewayImpl>()
                .register<FakeUnleash>()
                .register<SamGatewayImpl>()
                .register<NomInfoGateway>()
                .register<KabalGateway>()
                .register<NorgGateway>()
            motor.start()


        }

        @AfterAll
        @JvmStatic
        internal fun afterAll() {
            motor.stop()
        }
    }

    object TestPersoner {
        val STANDARD_PERSON = {
            FakePersoner.leggTil(
                TestPerson(
                    fødselsdato = Fødselsdato(LocalDate.now().minusYears(20)),
                    yrkesskade = listOf(),
                    sykepenger = listOf()
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
    }


    fun happyCaseFørstegangsbehandling(): Sak {
        val fom = LocalDate.now().minusMonths(3)
        val periode = Periode(fom, fom.plusYears(3))

        // Simulerer et svar fra YS-løsning om at det finnes en yrkesskade
        val person = TestPersoner.STANDARD_PERSON()
        val ident = person.aktivIdent()

        // Sender inn en søknad
        var behandling = sendInnSøknad(ident, periode, TestSøknader.STANDARD_SØKNAD)

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
                        skalVurdereAapIOvergangTilUføre = null,
                        skalVurdereAapIOvergangTilArbeid = null,
                        overgangBegrunnelse = null
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
            // Sender inn en søknad
            .sendInnDokument(
                DokumentMottattPersonHendelse(
                    journalpost = JournalpostId("220"),
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
            .kvalitetssikreOk()
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


    /**
     * Løser avklaringsbehov og venter på svar vha [util].
     */
    protected fun løsAvklaringsBehov(
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
        return hentBehandling(behandling.referanse)
    }

    protected fun TestUtil.ventPåSvar(behandling: Behandling) {
        util.ventPåSvar(behandling.sakId.id, behandling.id.id)
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
                    skalVurdereAapIOvergangTilUføre = null,
                    skalVurdereAapIOvergangTilArbeid = null,
                    overgangBegrunnelse = null
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
        kvalitetssikreOk(behandling)
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
                    skalVurdereAapIOvergangTilUføre = null,
                    skalVurdereAapIOvergangTilArbeid = null,
                    overgangBegrunnelse = null
                )
            )
        )
    }


    @JvmName("løsSykdomExt")
    protected fun Behandling.løsSykdom(): Behandling {
        return løsSykdom(this)
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

    protected fun opprettBehandling(
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

    protected fun hentVilkårsresultat(behandlingId: BehandlingId): Vilkårsresultat {
        return dataSource.transaction(readOnly = true) { connection ->
            VilkårsresultatRepositoryImpl(connection).hent(behandlingId)
        }
    }

    protected fun hentNyesteBehandlingForSak(
        sakId: SakId,
        typeBehandling: List<TypeBehandling> = TypeBehandling.entries
    ): Behandling {
        return dataSource.transaction(readOnly = true) { connection ->
            val finnSisteBehandlingFor = BehandlingRepositoryImpl(connection).finnSisteBehandlingFor(
                sakId,
                typeBehandling
            )
            requireNotNull(finnSisteBehandlingFor)
        }
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

    protected fun sendInnSøknad(ident: Ident, periode: Periode, søknad: Søknad): Behandling {
        return sendInnDokument(
            ident, DokumentMottattPersonHendelse(
                journalpost = JournalpostId(Random().nextInt(1000000).toString()),
                mottattTidspunkt = LocalDateTime.now(),
                strukturertDokument = StrukturertDokument(søknad),
                periode = periode
            )
        )
    }

    protected fun sendInnDokument(
        ident: Ident,
        dokumentMottattPersonHendelse: DokumentMottattPersonHendelse
    ): Behandling {
        hendelsesMottak.håndtere(ident, dokumentMottattPersonHendelse)
        util.ventPåSvar()
        val sak = hentSak(ident, dokumentMottattPersonHendelse.periode)
        val behandling = hentNyesteBehandlingForSak(sak.id)
        return behandling
    }

    protected fun Behandling.sendInnDokument(
        dokumentMottattPersonHendelse: DokumentMottattPersonHendelse
    ): Behandling {
        val aktivIdent = hentSak(this).person.aktivIdent()
        return sendInnDokument(aktivIdent, dokumentMottattPersonHendelse)
    }

    protected fun sendInnDokument(
        ident: Ident,
        hendelse: NyÅrsakTilBehandlingHendelse
    ): Behandling {
        hendelsesMottak.håndtere(ident, hendelse)
        util.ventPåSvar()
        return hentBehandling(hendelse.referanse.asBehandlingReferanse)
    }

    protected fun hentBrevAvType(behandling: Behandling, typeBrev: TypeBrev) =
        dataSource.transaction(readOnly = true) {
            BrevbestillingRepositoryImpl(it).hent(behandling.id)
                .first { it.typeBrev == typeBrev }
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
            .kvalitetssikreOk()

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

    protected fun Behandling.løsBeregningstidspunkt(): Behandling {
        return løsAvklaringsBehov(
            this,
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


    protected fun kvalitetssikreOk(
        behandling: Behandling,
        bruker: Bruker = Bruker("KVALITETSSIKRER")
    ): Behandling {
        val alleAvklaringsbehov = hentAlleAvklaringsbehov(behandling)
        return løsAvklaringsBehov(
            behandling,
            KvalitetssikringLøsning(alleAvklaringsbehov.filter { behov -> behov.erTotrinn() }.map { behov ->
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

    class BehandlingInfo(val åpneAvklaringsbehov: List<Avklaringsbehov>, val behandling: Behandling)

    protected fun Behandling.medKontekst(block: BehandlingInfo.() -> Unit): Behandling {
        val åpneAvklaringsbehov = hentÅpneAvklaringsbehov(this)
        block(BehandlingInfo(åpneAvklaringsbehov = åpneAvklaringsbehov, behandling = this))
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

    protected fun Behandling.løsVedtaksbrev(): Behandling {
        val brevbestilling = hentBrevAvType(this, TypeBrev.VEDTAK_INNVILGELSE)

        return this.løsAvklaringsBehov(vedtaksbrevLøsning(brevbestilling.referanse.brevbestillingReferanse))
    }

    protected fun leggTilÅrsakForBehandling(behandling: Behandling, årsaker: List<Årsak>) {
        dataSource.transaction { connection ->
            SakOgBehandlingService(postgresRepositoryRegistry.provider(connection))
                .finnEllerOpprettBehandling(behandling.sakId, årsaker)
        }
        prosesserBehandling(behandling)
    }

    protected fun prosesserBehandling(behandling: Behandling): Behandling {
        dataSource.transaction { connection ->
            FlytOrkestrator(postgresRepositoryRegistry.provider(connection)).forberedOgProsesserBehandling(
                FlytKontekst(
                    sakId = behandling.sakId,
                    behandlingId = behandling.id,
                    forrigeBehandlingId = behandling.forrigeBehandlingId,
                    behandlingType = behandling.typeBehandling(),
                ),
            )
        }
        util.ventPåSvar(behandling.sakId.id, behandling.id.id)
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