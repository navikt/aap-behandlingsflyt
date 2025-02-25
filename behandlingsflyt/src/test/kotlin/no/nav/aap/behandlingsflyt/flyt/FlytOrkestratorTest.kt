package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.SYSTEMBRUKER
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
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarSamordningGraderingLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarStudentLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarSykdomLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarYrkesskadeLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklaringsbehovLøsning
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
import no.nav.aap.behandlingsflyt.behandling.samordning.Ytelse
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.TilkjentYtelseRepositoryImpl
import no.nav.aap.behandlingsflyt.behandling.vilkår.medlemskap.EØSLand
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.BeregningsgrunnlagRepositoryImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.GrunnlagYrkesskade
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningVurderingPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepositoryImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.StrukturertDokument
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.AktivitetspliktRepositoryImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.LovvalgVedSøknadsTidspunkt
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.ManuellVurderingForForutgåendeMedlemskap
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.ManuellVurderingForLovvalgMedlemskap
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.MedlemskapVedSøknadsTidspunkt
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.register.inntekt.InntektGrunnlagRepositoryImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektPerÅr
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.adapter.MedlemskapGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningYrkeskaderBeløpVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningstidspunktVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.YrkesskadeBeløpVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.flate.BistandVurderingDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.samordning.VurderingerForSamordning
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.flate.SykdomsvurderingLøsningDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.flate.YrkesskadevurderingDto
import no.nav.aap.behandlingsflyt.flyt.internals.DokumentMottattPersonHendelse
import no.nav.aap.behandlingsflyt.flyt.internals.TestHendelsesMottak
import no.nav.aap.behandlingsflyt.hendelse.avløp.BehandlingHendelseServiceImpl
import no.nav.aap.behandlingsflyt.hendelse.mottak.BehandlingSattPåVent
import no.nav.aap.behandlingsflyt.integrasjon.barn.PdlBarnGateway
import no.nav.aap.behandlingsflyt.integrasjon.dokumentinnhenting.DokumentinnhentingGatewayImpl
import no.nav.aap.behandlingsflyt.integrasjon.ident.PdlIdentGateway
import no.nav.aap.behandlingsflyt.integrasjon.ident.PdlPersoninfoBulkGateway
import no.nav.aap.behandlingsflyt.integrasjon.ident.PdlPersoninfoGateway
import no.nav.aap.behandlingsflyt.integrasjon.samordning.AbakusForeldrepengerGateway
import no.nav.aap.behandlingsflyt.integrasjon.samordning.AbakusSykepengerGateway
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
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.UtenlandsPeriodeDto
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.StoppetBehandling
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.prosessering.HendelseMottattHåndteringJobbUtfører
import no.nav.aap.behandlingsflyt.prosessering.ProsesseringsJobber
import no.nav.aap.behandlingsflyt.repository.avklaringsbehov.AvklaringsbehovRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.barnetillegg.BarnetilleggRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.effektuer11_7.Effektuer11_7RepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.samordning.SamordningRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.samordning.ytelsesvurdering.SamordningYtelseVurderingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.underveis.UnderveisRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.dokument.arbeid.PliktkortRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.medlemskaplovvalg.MedlemskapArbeidInntektForutgåendeRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.medlemskaplovvalg.MedlemskapArbeidInntektRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.personopplysning.PersonopplysningForutgåendeRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.personopplysning.PersonopplysningRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.register.barn.BarnRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.register.institusjonsopphold.InstitusjonsoppholdRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.register.uføre.UføreRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.register.yrkesskade.YrkesskadeRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.arbeidsevne.ArbeidsevneRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.beregning.BeregningVurderingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.bistand.BistandRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.meldeplikt.MeldepliktRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.student.StudentRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.sykdom.SykdomRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.sykdom.SykepengerErstatningRepositoryImpl
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
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.PdlFolkeregisterPersonStatus
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.PdlFolkeregistermetadata
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.PdlStatsborgerskap
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.PersonStatus
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
import no.nav.aap.komponenter.verdityper.Prosent
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
                .register<MedlemskapArbeidInntektRepositoryImpl>()
                .register<SykepengerErstatningRepositoryImpl>()
                .register<SamordningYtelseVurderingRepositoryImpl>()
                .register<StudentRepositoryImpl>()
                .register<MeldepliktRepositoryImpl>()
                .register<MedlemskapArbeidInntektForutgåendeRepositoryImpl>()
                .register<PersonopplysningForutgåendeRepositoryImpl>()
                .register<BarnRepositoryImpl>()
                .register<InstitusjonsoppholdRepositoryImpl>()
                .register<InntektGrunnlagRepositoryImpl>()
                .status()
            GatewayRegistry
                .register<PdlBarnGateway>()
                .register<PdlIdentGateway>()
                .register<PdlPersoninfoBulkGateway>()
                .register<PdlPersoninfoGateway>()
                .register<AbakusSykepengerGateway>()
                .register<AbakusForeldrepengerGateway>()
                .register<DokumentinnhentingGatewayImpl>()
                .register<MedlemskapGateway>()
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
        sendInnDokument(
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
        util.ventPåSvar()

        val sak = hentSak(ident, periode)
        var behandling = hentBehandling(sak.id)
        assertThat(behandling.typeBehandling()).isEqualTo(TypeBehandling.Førstegangsbehandling)

        var alleAvklaringsbehov = hentAlleAvklaringsbehov(behandling.id)

        assertThat(alleAvklaringsbehov).isNotEmpty()
        assertThat(behandling.status()).isEqualTo(Status.UTREDES)

        løsSykdom(behandling)

        util.ventPåSvar(sak.id.toLong(), behandling.id.toLong())
        behandling = hentBehandling(sak.id)

        løsAvklaringsBehov(
            behandling, AvklarBistandsbehovLøsning(
                bistandsVurdering = BistandVurderingDto(
                    begrunnelse = "Trenger hjelp fra nav",
                    erBehovForAktivBehandling = true,
                    erBehovForArbeidsrettetTiltak = false,
                    erBehovForAnnenOppfølging = null
                ),
            )
        )


        util.ventPåSvar(sak.id.toLong(), behandling.id.toLong())
        behandling = hentBehandling(sak.id)

        alleAvklaringsbehov = hentAlleAvklaringsbehov(behandling.id)
        løsAvklaringsBehov(
            behandling,
            KvalitetssikringLøsning(
                alleAvklaringsbehov
                    .filter { behov -> behov.erTotrinn() }
                    .map { behov ->
                        TotrinnsVurdering(
                            behov.definisjon.kode,
                            true,
                            "begrunnelse",
                            emptyList()
                        )
                    }),
        )

        util.ventPåSvar(sak.id.toLong(), behandling.id.toLong())
        behandling = hentBehandling(sak.id)

        løsAvklaringsBehov(
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

        util.ventPåSvar(sak.id.toLong(), behandling.id.toLong())
        behandling = hentBehandling(sak.id)

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

        util.ventPåSvar(sak.id.toLong(), behandling.id.toLong())
        behandling = hentBehandling(sak.id)

        // Saken står til en-trinnskontroll hos saksbehandler klar for å bli sendt til beslutter
        alleAvklaringsbehov = hentAlleAvklaringsbehov(behandling.id)
        assertThat(alleAvklaringsbehov).anySatisfy { assertThat(it.erÅpent() && it.definisjon == Definisjon.FORESLÅ_VEDTAK).isTrue() }
        assertThat(behandling.status()).isEqualTo(Status.UTREDES)

        løsAvklaringsBehov(behandling, ForeslåVedtakLøsning())

        util.ventPåSvar(sak.id.toLong(), behandling.id.toLong())
        behandling = hentBehandling(sak.id)

        løsAvklaringsBehov(
            behandling, FatteVedtakLøsning(
                hentAlleAvklaringsbehov(behandling.id)
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

        util.ventPåSvar(sak.id.toLong(), behandling.id.toLong())
        behandling = hentBehandling(sak.id)

        løsSykdom(behandling)

        util.ventPåSvar(sak.id.toLong(), behandling.id.toLong())
        behandling = hentBehandling(sak.id)

        løsAvklaringsBehov(
            behandling,
            AvklarBistandsbehovLøsning(
                bistandsVurdering = BistandVurderingDto(
                    begrunnelse = "Trenger hjelp fra nav",
                    erBehovForAktivBehandling = true,
                    erBehovForArbeidsrettetTiltak = false,
                    erBehovForAnnenOppfølging = null
                ),
            ),
        )
        util.ventPåSvar(sak.id.toLong(), behandling.id.toLong())
        behandling = hentBehandling(sak.id)

        løsAvklaringsBehov(
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

        util.ventPåSvar(sak.id.toLong(), behandling.id.toLong())
        behandling = hentBehandling(sak.id)

        løsAvklaringsBehov(
            behandling, FastsettBeregningstidspunktLøsning(
                beregningVurdering = BeregningstidspunktVurdering(
                    begrunnelse = "Trenger hjelp fra Nav",
                    nedsattArbeidsevneDato = LocalDate.now(),
                    ytterligereNedsattArbeidsevneDato = null,
                    ytterligereNedsattBegrunnelse = null
                ),
            )
        )

        util.ventPåSvar(sak.id.toLong(), behandling.id.toLong())
        behandling = hentBehandling(sak.id)

        // Saken er tilbake til en-trinnskontroll hos saksbehandler klar for å bli sendt til beslutter
        alleAvklaringsbehov = hentAlleAvklaringsbehov(behandling.id)
        assertThat(alleAvklaringsbehov).anySatisfy { assertTrue(it.erÅpent() && it.definisjon == Definisjon.FORESLÅ_VEDTAK) }
        assertThat(behandling.status()).isEqualTo(Status.UTREDES)

        løsAvklaringsBehov(behandling, ForeslåVedtakLøsning())
        util.ventPåSvar(sak.id.toLong(), behandling.id.toLong())

        // Saken står til To-trinnskontroll hos beslutter
        alleAvklaringsbehov = hentAlleAvklaringsbehov(behandling.id)
        assertThat(alleAvklaringsbehov).anySatisfy { assertTrue(it.erÅpent() && it.definisjon == Definisjon.FATTE_VEDTAK) }
        assertThat(behandling.status()).isEqualTo(Status.UTREDES)

        behandling = hentBehandling(sak.id)

        løsAvklaringsBehov(
            behandling, FatteVedtakLøsning(
                alleAvklaringsbehov
                    .filter { behov -> behov.erTotrinn() }
                    .map { behov ->
                        TotrinnsVurdering(
                            behov.definisjon.kode,
                            true,
                            "begrunnelse",
                            emptyList()
                        )
                    }),
            Bruker("BESLUTTER")
        )
        util.ventPåSvar(sak.id.toLong(), behandling.id.toLong())

        behandling = hentBehandling(sak.id)

        alleAvklaringsbehov = hentAlleAvklaringsbehov(behandling.id)
        // Det er bestilt vedtaksbrev
        assertThat(alleAvklaringsbehov).anySatisfy { assertTrue(it.erÅpent() && it.definisjon == Definisjon.BESTILL_BREV) }
        assertThat(behandling.status()).isEqualTo(Status.IVERKSETTES)

        var brevbestilling = hentBrevAvType(behandling, TypeBrev.VEDTAK_INNVILGELSE)

        løsAvklaringsBehov(
            behandling, BrevbestillingLøsning(
                LøsBrevbestillingDto(
                    behandlingReferanse = behandling.referanse.referanse,
                    bestillingReferanse = brevbestilling.referanse.brevbestillingReferanse,
                    status = BrevbestillingLøsningStatus.KLAR_FOR_EDITERING
                )
            ), BREV_SYSTEMBRUKER
        )
        brevbestilling = hentBrevAvType(behandling, TypeBrev.VEDTAK_INNVILGELSE)
        // Brevet er klar for forhåndsvisning og editering
        assertThat(brevbestilling.status).isEqualTo(
            no.nav.aap.behandlingsflyt.behandling.brev.bestilling.Status.FORHÅNDSVISNING_KLAR
        )

        util.ventPåSvar(sak.id.toLong(), behandling.id.toLong())

        behandling = hentBehandling(sak.id)

        alleAvklaringsbehov = hentAlleAvklaringsbehov(behandling.id)
        // Venter på at brevet skal fullføres
        assertThat(alleAvklaringsbehov).anySatisfy { assertTrue(it.erÅpent() && it.definisjon == Definisjon.SKRIV_BREV) }

        brevbestilling = hentBrevAvType(behandling, TypeBrev.VEDTAK_INNVILGELSE)

        løsAvklaringsBehov(
            behandling,
            SkrivBrevLøsning(brevbestillingReferanse = brevbestilling.referanse.brevbestillingReferanse),
        )


        brevbestilling = hentBrevAvType(behandling, TypeBrev.VEDTAK_INNVILGELSE)
        // Brevet er fullført
        assertThat(brevbestilling.status).isEqualTo(
            no.nav.aap.behandlingsflyt.behandling.brev.bestilling.Status.FULLFØRT
        )


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
        assertThat(underveisGrunnlag.perioder.any { it.arbeidsgradering.gradering.prosentverdi() > 0 }).isTrue()

        // Saken er avsluttet, så det skal ikke være flere åpne avklaringsbehov
        val åpneAvklaringsbehov = hentÅpneAvklaringsbehov(behandling.id)
        assertThat(åpneAvklaringsbehov).isEmpty()

        sendInnDokument(
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
        util.ventPåSvar(sakId = sak.id.toLong())

        behandling = hentBehandling(sak.id)
        assertThat(behandling.typeBehandling()).isEqualTo(TypeBehandling.Revurdering)
        assertThat(behandling.status()).isEqualTo(Status.UTREDES)

        løsAvklaringsBehov(
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
        util.ventPåSvar(sak.id.toLong(), behandling.id.toLong())
        behandling = hentBehandling(sak.id)

        alleAvklaringsbehov = hentAlleAvklaringsbehov(behandling.id)

        assertThat(alleAvklaringsbehov).anySatisfy { assertTrue(it.erÅpent() && it.definisjon == Definisjon.AVKLAR_BISTANDSBEHOV) }
        assertThat(behandling.status()).isEqualTo(Status.UTREDES)
    }

    @Test
    fun `skal avklare yrkesskade hvis det finnes spor av yrkesskade - yrkesskade har årsakssammenheng`() {
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))

        val person = TestPerson(
            fødselsdato = Fødselsdato(LocalDate.now().minusYears(20)),
            yrkesskade = listOf(TestYrkesskade()),
            uføre = null
        )
        FakePersoner.leggTil(person)

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
                        medlemskap = SøknadMedlemskapDto("JA", "NEI", "NEI", "NEI", null)
                    ),
                ),
                periode = periode
            )
        )
        util.ventPåSvar()

        val sak = hentSak(ident, periode)
        var behandling = hentBehandling(sak.id)
        assertThat(behandling.typeBehandling()).isEqualTo(TypeBehandling.Førstegangsbehandling)

        var alleAvklaringsbehov = hentAlleAvklaringsbehov(behandling.id)
        assertThat(alleAvklaringsbehov).isNotEmpty()
        assertThat(behandling.status()).isEqualTo(Status.UTREDES)

        løsSykdom(behandling)

        util.ventPåSvar(sak.id.toLong(), behandling.id.toLong())
        behandling = hentBehandling(sak.id)

        løsAvklaringsBehov(
            behandling,
            AvklarBistandsbehovLøsning(
                bistandsVurdering = BistandVurderingDto(
                    begrunnelse = "Trenger hjelp fra nav",
                    erBehovForAktivBehandling = true,
                    erBehovForArbeidsrettetTiltak = false,
                    erBehovForAnnenOppfølging = null
                ),
            ),
        )

        util.ventPåSvar(sak.id.toLong(), behandling.id.toLong())
        behandling = hentBehandling(sak.id)

        alleAvklaringsbehov = hentAlleAvklaringsbehov(behandling.id)
        løsAvklaringsBehov(
            behandling,
            KvalitetssikringLøsning(
                alleAvklaringsbehov
                    .filter { behov -> behov.erTotrinn() }
                    .map { behov ->
                        TotrinnsVurdering(
                            behov.definisjon.kode,
                            true,
                            "begrunnelse",
                            emptyList()
                        )
                    }),
        )
        util.ventPåSvar(sak.id.toLong(), behandling.id.toLong())
        behandling = hentBehandling(sak.id)

        løsAvklaringsBehov(
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

        util.ventPåSvar(sak.id.toLong(), behandling.id.toLong())
        behandling = hentBehandling(sak.id)

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
        util.ventPåSvar(sak.id.toLong(), behandling.id.toLong())
        behandling = hentBehandling(sak.id)

        løsAvklaringsBehov(
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
        util.ventPåSvar(sak.id.toLong(), behandling.id.toLong())
        behandling = hentBehandling(sak.id)

        // Saken står til en-trinnskontroll hos saksbehandler klar for å bli sendt til beslutter
        alleAvklaringsbehov = hentAlleAvklaringsbehov(behandling.id)
        assertThat(alleAvklaringsbehov).anySatisfy { avklaringsbehov -> assertThat(avklaringsbehov.erÅpent() && avklaringsbehov.definisjon == Definisjon.FORESLÅ_VEDTAK).isTrue() }
        assertThat(behandling.status()).isEqualTo(Status.UTREDES)

        løsAvklaringsBehov(behandling, ForeslåVedtakLøsning())
        util.ventPåSvar(sak.id.toLong(), behandling.id.toLong())

        // Saken står til To-trinnskontroll hos beslutter
        alleAvklaringsbehov = hentAlleAvklaringsbehov(behandling.id)
        assertThat(alleAvklaringsbehov).anySatisfy { assertThat(it.erÅpent() && it.definisjon == Definisjon.FATTE_VEDTAK).isTrue() }
        assertThat(behandling.status()).isEqualTo(Status.UTREDES)

        behandling = hentBehandling(sak.id)

        løsAvklaringsBehov(
            behandling, FatteVedtakLøsning(
                alleAvklaringsbehov
                    .filter { behov -> behov.erTotrinn() }
                    .map { behov ->
                        TotrinnsVurdering(
                            behov.definisjon.kode,
                            true,
                            "begrunnelse",
                            emptyList()
                        )
                    }),
            Bruker("BESLUTTER")
        )

        util.ventPåSvar(sak.id.toLong(), behandling.id.toLong())
        behandling = hentBehandling(sak.id)

        alleAvklaringsbehov = hentAlleAvklaringsbehov(behandling.id)
        // Det er bestilt vedtaksbrev
        assertThat(alleAvklaringsbehov).anySatisfy { assertTrue(it.erÅpent() && it.definisjon == Definisjon.BESTILL_BREV) }
        assertThat(behandling.status()).isEqualTo(Status.IVERKSETTES)

        var brevBestilling = hentBrevAvType(behandling, TypeBrev.VEDTAK_INNVILGELSE)

        løsAvklaringsBehov(
            behandling, BrevbestillingLøsning(
                LøsBrevbestillingDto(
                    behandlingReferanse = behandling.referanse.referanse,
                    bestillingReferanse = brevBestilling.referanse.brevbestillingReferanse,
                    status = BrevbestillingLøsningStatus.KLAR_FOR_EDITERING
                )
            ), BREV_SYSTEMBRUKER
        )

        brevBestilling = hentBrevAvType(behandling, TypeBrev.VEDTAK_INNVILGELSE)

        // Brevet er klar for forhåndsvisning og editering
        assertThat(brevBestilling.status).isEqualTo(no.nav.aap.behandlingsflyt.behandling.brev.bestilling.Status.FORHÅNDSVISNING_KLAR)

        util.ventPåSvar(sak.id.toLong(), behandling.id.toLong())

        behandling = hentBehandling(sak.id)

        alleAvklaringsbehov = hentAlleAvklaringsbehov(behandling.id)
        // Venter på at brevet skal fullføres
        assertThat(alleAvklaringsbehov).anySatisfy { assertTrue(it.erÅpent() && it.definisjon == Definisjon.SKRIV_BREV) }

        brevBestilling = hentBrevAvType(behandling, TypeBrev.VEDTAK_INNVILGELSE)
        løsAvklaringsBehov(
            behandling, SkrivBrevLøsning(brevbestillingReferanse = brevBestilling.referanse.brevbestillingReferanse),
        )

        brevBestilling = hentBrevAvType(behandling, TypeBrev.VEDTAK_INNVILGELSE)
        // Brevet er fullført
        assertThat(brevBestilling.status).isEqualTo(no.nav.aap.behandlingsflyt.behandling.brev.bestilling.Status.FULLFØRT)

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
    fun `stopper opp ved samordning ved funn av sykepenger, og løses ved info fra saksbehandler`() {
        val fom = LocalDate.now().minusMonths(3)
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
        sendInnDokument(
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
        util.ventPåSvar()

        val sak = hentSak(ident, periode)
        var behandling = hentBehandling(sak.id)
        assertThat(behandling.typeBehandling()).isEqualTo(TypeBehandling.Førstegangsbehandling)

        var alleAvklaringsbehov = hentAlleAvklaringsbehov(behandling.id)

        assertThat(alleAvklaringsbehov).isNotEmpty()
        assertThat(behandling.status()).isEqualTo(Status.UTREDES)

        løsSykdom(behandling)

        util.ventPåSvar(sak.id.toLong(), behandling.id.toLong())
        behandling = hentBehandling(sak.id)

        løsAvklaringsBehov(
            behandling,
            AvklarBistandsbehovLøsning(
                bistandsVurdering = BistandVurderingDto(
                    begrunnelse = "Trenger hjelp fra nav",
                    erBehovForAktivBehandling = true,
                    erBehovForArbeidsrettetTiltak = false,
                    erBehovForAnnenOppfølging = null
                ),
            ),
        )

        util.ventPåSvar(sak.id.toLong(), behandling.id.toLong())
        behandling = hentBehandling(sak.id)

        alleAvklaringsbehov = hentAlleAvklaringsbehov(behandling.id)
        løsAvklaringsBehov(
            behandling,
            KvalitetssikringLøsning(
                alleAvklaringsbehov
                    .filter { behov -> behov.erTotrinn() }
                    .map { behov ->
                        TotrinnsVurdering(
                            behov.definisjon.kode,
                            true,
                            "begrunnelse",
                            emptyList()
                        )
                    }),

            )

        util.ventPåSvar(sak.id.toLong(), behandling.id.toLong())
        behandling = hentBehandling(sak.id)

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

        util.ventPåSvar(sak.id.toLong(), behandling.id.toLong())
        behandling = hentBehandling(sak.id)

        assertThat(hentÅpneAvklaringsbehov(behandling.id).map { it.definisjon }).containsExactly(Definisjon.AVKLAR_SAMORDNING_GRADERING)


        løsAvklaringsBehov(
            behandling,
            AvklarSamordningGraderingLøsning(
                vurderingerForSamordning = VurderingerForSamordning(
                    vurderteSamordninger = listOf(
                        SamordningVurdering(
                            ytelseType = Ytelse.SYKEPENGER,
                            vurderingPerioder = listOf(
                                SamordningVurderingPeriode(
                                    periode = sykePengerPeriode,
                                    gradering = Prosent(90),
                                    kronesum = null
                                )
                            )
                        )
                    )
                ),
            ),
        )
        util.ventPåSvar(sak.id.toLong(), behandling.id.toLong())

        assertThat(hentÅpneAvklaringsbehov(behandling.id).map { it.definisjon }).isEqualTo(listOf(Definisjon.FORESLÅ_VEDTAK))
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
                        medlemskap = SøknadMedlemskapDto("JA", "NEI", "NEI", "NEI", null)
                    )
                ),
                periode = periode
            )
        )
        util.ventPåSvar()

        val sak = hentSak(ident, periode)
        var behandling = hentBehandling(sak.id)
        assertThat(behandling.typeBehandling()).isEqualTo(TypeBehandling.Førstegangsbehandling)

        var alleAvklaringsbehov = hentAlleAvklaringsbehov(behandling.id)
        assertThat(alleAvklaringsbehov).isNotEmpty()
        assertThat(behandling.status()).isEqualTo(Status.UTREDES)

        løsAvklaringsBehov(
            behandling,
            AvklarStudentLøsning(
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
        )
        util.ventPåSvar(sak.id.toLong(), behandling.id.toLong())
        behandling = hentBehandling(sak.id)

        løsAvklaringsBehov(
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
        util.ventPåSvar(sak.id.toLong(), behandling.id.toLong())
        behandling = hentBehandling(sak.id)

        løsAvklaringsBehov(
            behandling,
            AvklarBistandsbehovLøsning(
                bistandsVurdering = BistandVurderingDto(
                    begrunnelse = "Trenger hjelp fra nav",
                    erBehovForAktivBehandling = true,
                    erBehovForArbeidsrettetTiltak = false,
                    erBehovForAnnenOppfølging = null
                ),
            ),
        )

        util.ventPåSvar(sak.id.toLong(), behandling.id.toLong())
        behandling = hentBehandling(sak.id)

        // Saken står til en-trinnskontroll hos saksbehandler klar for å bli sendt til beslutter
        alleAvklaringsbehov = hentAlleAvklaringsbehov(behandling.id)
        assertThat(alleAvklaringsbehov).isNotEmpty()
        assertThat(alleAvklaringsbehov).anySatisfy { behov -> assertThat(behov.erÅpent() && behov.definisjon == Definisjon.KVALITETSSIKRING).isTrue() }
        assertThat(behandling.status()).isEqualTo(Status.UTREDES)

        alleAvklaringsbehov = hentAlleAvklaringsbehov(behandling.id)
        løsAvklaringsBehov(
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

        util.ventPåSvar(sak.id.toLong(), behandling.id.toLong())
        behandling = hentBehandling(sak.id)

        løsAvklaringsBehov(
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
        util.ventPåSvar(sak.id.toLong(), behandling.id.toLong())
        behandling = hentBehandling(sak.id)

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
        util.ventPåSvar(sak.id.toLong(), behandling.id.toLong())
        behandling = hentBehandling(sak.id)

        løsAvklaringsBehov(behandling, ForeslåVedtakLøsning())
        util.ventPåSvar(sak.id.toLong(), behandling.id.toLong())

        // Saken står til To-trinnskontroll hos beslutter
        alleAvklaringsbehov = hentAlleAvklaringsbehov(behandling.id)
        assertThat(alleAvklaringsbehov).anySatisfy { assertThat(it.erÅpent() && it.definisjon == Definisjon.FATTE_VEDTAK).isTrue() }
        assertThat(behandling.status()).isEqualTo(Status.UTREDES)

        behandling = hentBehandling(sak.id)

        alleAvklaringsbehov = hentAlleAvklaringsbehov(behandling.id)
        løsAvklaringsBehov(
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
        util.ventPåSvar(sak.id.toLong(), behandling.id.toLong())

        behandling = hentBehandling(sak.id)
        assertThat(behandling.status()).isEqualTo(Status.UTREDES)
        alleAvklaringsbehov = hentAlleAvklaringsbehov(behandling.id)
        assertThat(alleAvklaringsbehov).anySatisfy { assertThat(it.erÅpent() && it.definisjon == Definisjon.AVKLAR_SYKDOM).isTrue() }

        løsAvklaringsBehov(
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
        util.ventPåSvar(sak.id.toLong(), behandling.id.toLong())
        behandling = hentBehandling(sak.id)

        løsAvklaringsBehov(
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
        util.ventPåSvar(sak.id.toLong(), behandling.id.toLong())
        behandling = hentBehandling(sak.id)

        // Saken står til en-trinnskontroll hos saksbehandler klar for å bli sendt til beslutter
        alleAvklaringsbehov = hentAlleAvklaringsbehov(behandling.id)
        assertThat(alleAvklaringsbehov).anySatisfy { behov -> assertThat(behov.erÅpent() && behov.definisjon == Definisjon.FORESLÅ_VEDTAK).isTrue() }
        assertThat(behandling.status()).isEqualTo(Status.UTREDES)

        løsAvklaringsBehov(behandling, ForeslåVedtakLøsning())
        util.ventPåSvar(sak.id.toLong(), behandling.id.toLong())

        // Saken står til To-trinnskontroll hos beslutter
        alleAvklaringsbehov = hentAlleAvklaringsbehov(behandling.id)
        assertThat(alleAvklaringsbehov).anySatisfy { assertThat(it.erÅpent() && it.definisjon == Definisjon.FATTE_VEDTAK).isTrue() }
        assertThat(behandling.status()).isEqualTo(Status.UTREDES)

        behandling = hentBehandling(sak.id)

        løsAvklaringsBehov(
            behandling, FatteVedtakLøsning(
                alleAvklaringsbehov
                    .filter { behov -> behov.erTotrinn() }
                    .map { behov ->
                        TotrinnsVurdering(
                            behov.definisjon.kode,
                            true,
                            "begrunnelse",
                            emptyList()
                        )
                    }),
            Bruker("BESLUTTER")
        )
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
        val behandling = hentBehandling(sak.id)
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

        val alleAvklaringsbehov = hentAlleAvklaringsbehov(behandling.id)
        assertThat(alleAvklaringsbehov).allSatisfy { assertThat(it.definisjon.kode).isEqualTo(AvklaringsbehovKode.`9002`) }

        var brevbestilling = hentBrevAvType(behandling, TypeBrev.VEDTAK_AVSLAG)
        løsAvklaringsBehov(
            behandling, BrevbestillingLøsning(
                LøsBrevbestillingDto(
                    behandlingReferanse = behandling.referanse.referanse,
                    bestillingReferanse = brevbestilling.referanse.brevbestillingReferanse,
                    status = BrevbestillingLøsningStatus.KLAR_FOR_EDITERING
                )
            ),
            BREV_SYSTEMBRUKER
        )
        util.ventPåSvar(sak.id.toLong(), behandling.id.toLong())

        brevbestilling = hentBrevAvType(behandling, TypeBrev.VEDTAK_AVSLAG)
        løsAvklaringsBehov(
            behandling, SkrivBrevLøsning(brevbestillingReferanse = brevbestilling.referanse.brevbestillingReferanse)
        )
        util.ventPåSvar(sak.id.toLong(), behandling.id.toLong())

        val behov = hentÅpneAvklaringsbehov(behandling.id)
        assertThat(behov).isEmpty()

        util.ventPåSvar()

        assertThat(hentBehandling(sak.id).status()).isEqualTo(Status.AVSLUTTET)
        assertThat(hendelser.last().behandlingStatus).isEqualTo(Status.AVSLUTTET)
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

        var alleAvklaringsbehov = hentAlleAvklaringsbehov(behandling.id)
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
        assertThat(frist).isNotNull
        behandling = hentBehandling(sak.id)

        alleAvklaringsbehov = hentAlleAvklaringsbehov(behandling.id)
        assertThat(alleAvklaringsbehov)
            .hasSize(2)
            .anySatisfy { assertThat(it.erÅpent() && it.definisjon == Definisjon.MANUELT_SATT_PÅ_VENT).isTrue() }
            .anySatisfy { assertThat(it.erÅpent() && it.definisjon == Definisjon.AVKLAR_SYKDOM).isTrue() }

        util.ventPåSvar(sak.id.toLong(), behandling.id.toLong())

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

        alleAvklaringsbehov = hentAlleAvklaringsbehov(behandling.id)
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
        var alleAvklaringsbehov = hentAlleAvklaringsbehov(behandling.id)
        assertThat(alleAvklaringsbehov).anySatisfy { assertThat(it.erÅpent() && it.definisjon == Definisjon.AVKLAR_SYKDOM).isTrue() }

        // Oppretter bestilling av legeerklæring
        bestillLegeErklærling(behandling)

        alleAvklaringsbehov = hentAlleAvklaringsbehov(behandling.id)
        assertThat(alleAvklaringsbehov).anySatisfy { assertThat(it.erÅpent() && it.definisjon == Definisjon.BESTILL_LEGEERKLÆRING).isTrue() }
        util.ventPåSvar()

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

    private fun bestillLegeErklærling(behandling: Behandling) {
        dataSource.transaction { connection ->
            val avklaringsbehovene = hentAvklaringsbehov(behandling.id, connection)
            val sakService = SakService(SakRepositoryImpl(connection))
            val behandlingHendelseService = BehandlingHendelseServiceImpl(
                FlytJobbRepository((connection)),
                BrevbestillingRepositoryImpl(connection),
                sakService)
            avklaringsbehovene.leggTil(
                definisjoner = listOf(Definisjon.BESTILL_LEGEERKLÆRING),
                funnetISteg = behandling.aktivtSteg(),
                grunn = ÅrsakTilSettPåVent.VENTER_PÅ_MEDISINSKE_OPPLYSNINGER,
                bruker = SYSTEMBRUKER
            )
            behandlingHendelseService.stoppet(behandling, avklaringsbehovene)
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
        var alleAvklaringsbehov = hentAlleAvklaringsbehov(behandling.id)
        assertThat(alleAvklaringsbehov).anySatisfy { assertThat(it.erÅpent() && it.definisjon == Definisjon.AVKLAR_SYKDOM).isTrue() }

        // Oppretter bestilling av legeerklæring
        bestillLegeErklærling(behandling)

        alleAvklaringsbehov = hentAlleAvklaringsbehov(behandling.id)
        assertThat(alleAvklaringsbehov).anySatisfy { assertThat(it.erÅpent() && it.definisjon == Definisjon.BESTILL_LEGEERKLÆRING).isTrue() }

        util.ventPåSvar()

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
        dataSource.transaction { connection ->
            val avklaringsbehov = hentAvklaringsbehov(behandling.id, connection)
            assertThat(avklaringsbehov.alle()).anySatisfy { assertThat(it.erÅpent() && it.definisjon == Definisjon.AVKLAR_SYKDOM).isTrue() }
        }

        // Oppretter bestilling av legeerklæring
        dataSource.transaction { connection ->
            val avklaringsbehovene = hentAvklaringsbehov(behandling.id, connection)
            val sakService = SakService(SakRepositoryImpl(connection))
            val behandlingHendelseService = BehandlingHendelseServiceImpl(
                FlytJobbRepository((connection)),
                BrevbestillingRepositoryImpl(connection),
                sakService
            )
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

    @Test
    fun `Lager avklaringsbehov i medlemskap når kravene til manuell avklaring oppfylles`() {
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
        val behandling = hentBehandling(sak.id)

        // Validér avklaring
        dataSource.transaction { connection ->
            val avklaringsbehov = hentAvklaringsbehov(behandling.id, connection)
            assertThat(avklaringsbehov.åpne().all { it.definisjon == Definisjon.AVKLAR_LOVVALG_MEDLEMSKAP })
        }
    }

    private fun løsSykdom(behandling: Behandling) {
        løsAvklaringsBehov(
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
        dataSource.transaction { connection ->
            val avklaringsbehov = hentAvklaringsbehov(behandling.id, connection)
            assertThat(avklaringsbehov.åpne().none())
        }
    }

    @Test
    fun `Går videre i forutgåendemedlemskapsteget når manuell vurdering mottas`() {
        val ident = ident()
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
        val behandling = hentBehandling(sak.id)

        løsFramTilForutgåendeMedlemskap(behandling, sak, false, ident)

        // Validér avklaring
        dataSource.transaction { connection ->
            val avklaringsbehov = hentAvklaringsbehov(behandling.id, connection)
            assertThat(avklaringsbehov.åpne().all { it.definisjon == Definisjon.AVKLAR_FORUTGÅENDE_MEDLEMSKAP })
        }

        // Trigger manuell vurdering
        løsAvklaringsBehov(
            behandling,
            AvklarForutgåendeMedlemskapLøsning(
                manuellVurderingForForutgåendeMedlemskap = ManuellVurderingForForutgåendeMedlemskap(
                    "begrunnelse", true, null, null
                ),
                behovstype = AvklaringsbehovKode.`5020`
            ),
        )
        util.ventPåSvar()

        // Validér avklaring
        dataSource.transaction { connection ->
            val avklaringsbehov = hentAvklaringsbehov(behandling.id, connection)
            assertThat(avklaringsbehov.åpne().none())
        }
    }

    @Test
    fun `Oppfyller ikke forutgående medlemskap når unntak ikke oppfylles og ikke medlem i folketrygden`() {
        val ident = ident()
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
        val behandling = hentBehandling(sak.id)

        løsFramTilForutgåendeMedlemskap(behandling, sak, false, ident)

        // Validér avklaring
        dataSource.transaction { connection ->
            val avklaringsbehov = hentAvklaringsbehov(behandling.id, connection)
            assertThat(avklaringsbehov.åpne().all { it.definisjon == Definisjon.AVKLAR_FORUTGÅENDE_MEDLEMSKAP })
        }

        // Trigger manuell vurdering
        løsAvklaringsBehov(
            behandling,
            AvklarForutgåendeMedlemskapLøsning(
                manuellVurderingForForutgåendeMedlemskap = ManuellVurderingForForutgåendeMedlemskap(
                    "begrunnelseforutgående", false, false, null
                ),
                behovstype = AvklaringsbehovKode.`5020`
            )
        )
        util.ventPåSvar()

        // Validér riktig resultat
        dataSource.transaction { connection ->
            val avklaringsbehov = hentAvklaringsbehov(behandling.id, connection)
            val vilkårsResultat = hentVilkårsresultat(behandling.id).finnVilkår(Vilkårtype.MEDLEMSKAP).vilkårsperioder()
            assertThat(avklaringsbehov.åpne().none())
            assertTrue(vilkårsResultat.none { it.erOppfylt() })
        }
    }

    @Test
    fun `Oppfyller forutgående medlemskap når unntak finnes`() {
        val ident = ident()
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
        val behandling = hentBehandling(sak.id)

        løsFramTilForutgåendeMedlemskap(behandling, sak, false, ident)

        // Validér avklaring
        dataSource.transaction { connection ->
            val avklaringsbehov = hentAvklaringsbehov(behandling.id, connection)
            assertThat(avklaringsbehov.åpne().all { it.definisjon == Definisjon.AVKLAR_FORUTGÅENDE_MEDLEMSKAP })
        }

        // Trigger manuell vurdering
        løsAvklaringsBehov(
            behandling, AvklarForutgåendeMedlemskapLøsning(
                manuellVurderingForForutgåendeMedlemskap = ManuellVurderingForForutgåendeMedlemskap(
                    "begrunnelse", true, true, null
                ),
                behovstype = AvklaringsbehovKode.`5020`
            )
        )
        util.ventPåSvar()

        // Validér riktig resultat
        dataSource.transaction { connection ->
            val avklaringsbehov = hentAvklaringsbehov(behandling.id, connection)
            val vilkårsResultat = hentVilkårsresultat(behandling.id).finnVilkår(Vilkårtype.MEDLEMSKAP).vilkårsperioder()
            assertThat(avklaringsbehov.åpne().none())
            assertTrue(vilkårsResultat.all { it.erOppfylt() })
        }
    }

    @Test
    fun `Går forbi forutgåendemedlemskapsteget når yrkesskade eksisterer`() {
        val ident = ident()
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

        løsFramTilForutgåendeMedlemskap(behandling, sak, true, ident)

        // Validér avklaring
        dataSource.transaction { connection ->
            val avklaringsbehov = hentAvklaringsbehov(behandling.id, connection)
            assertThat(avklaringsbehov.åpne().none { it.definisjon == Definisjon.AVKLAR_FORUTGÅENDE_MEDLEMSKAP })
        }
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
        val behandling = hentBehandling(sak.id)

        // Validér avklaring
        dataSource.transaction { connection ->
            val avklaringsbehov = hentAvklaringsbehov(behandling.id, connection)
            assertThat(avklaringsbehov.åpne().all { it.definisjon == Definisjon.AVKLAR_LOVVALG_MEDLEMSKAP })
        }

        // Trigger manuell vurdering
        dataSource.transaction { connection ->
            AvklaringsbehovHendelseHåndterer(
                AvklaringsbehovOrkestrator(
                    connection,
                    BehandlingHendelseServiceImpl(
                        FlytJobbRepository(connection),
                        BrevbestillingRepositoryImpl(connection),
                        SakService(SakRepositoryImpl(connection))
                    )
                ),
                AvklaringsbehovRepositoryImpl(connection),
                BehandlingRepositoryImpl(connection)
            ).håndtere(
                behandling.id,
                LøsAvklaringsbehovHendelse(
                    løsning = AvklarLovvalgMedlemskapLøsning(
                        manuellVurderingForLovvalgMedlemskap = ManuellVurderingForLovvalgMedlemskap(
                            LovvalgVedSøknadsTidspunkt("crazy lovvalgsland vurdering", null),
                            MedlemskapVedSøknadsTidspunkt("crazy medlemskap vurdering", true),
                            false
                        ),
                        behovstype = AvklaringsbehovKode.`5017`
                    ),
                    behandlingVersjon = behandling.versjon,
                    bruker = Bruker("SAKSBEHANDLER")
                )
            )
        }
        util.ventPåSvar()

        // Validér riktig resultat
        dataSource.transaction { connection ->
            val avklaringsbehov = hentAvklaringsbehov(behandling.id, connection)
            val vilkårsResultat = hentVilkårsresultat(behandling.id).finnVilkår(Vilkårtype.LOVVALG).vilkårsperioder()
            assertThat(avklaringsbehov.åpne().none())
            assertTrue(vilkårsResultat.all { it.erOppfylt() })
        }
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
        val behandling = hentBehandling(sak.id)

        // Validér avklaring
        var åpneAvklaringsbehov = hentÅpneAvklaringsbehov(behandling.id)
        assertThat(åpneAvklaringsbehov.all { it.definisjon == Definisjon.AVKLAR_LOVVALG_MEDLEMSKAP })

        // Trigger manuell vurdering
        løsAvklaringsBehov(
            behandling, AvklarLovvalgMedlemskapLøsning(
                manuellVurderingForLovvalgMedlemskap = ManuellVurderingForLovvalgMedlemskap(
                    LovvalgVedSøknadsTidspunkt("crazy lovvalgsland vurdering", EØSLand.DNK),
                    MedlemskapVedSøknadsTidspunkt(null, null), false
                ),
                behovstype = AvklaringsbehovKode.`5017`
            )
        )
        util.ventPåSvar()

        // Validér avklaring
        dataSource.transaction { connection ->
        }

        // Validér riktig resultat
        åpneAvklaringsbehov = hentÅpneAvklaringsbehov(behandling.id)
        assertThat(åpneAvklaringsbehov.none())
        dataSource.transaction { connection ->
            val vilkårsResultat = hentVilkårsresultat(behandling.id).finnVilkår(Vilkårtype.LOVVALG).vilkårsperioder()
            assertTrue(vilkårsResultat.none { it.erOppfylt() })
        }
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
        val behandling = hentBehandling(sak.id)

        // Validér avklaring
        dataSource.transaction { connection ->
            val avklaringsbehov = hentAvklaringsbehov(behandling.id, connection)
            assertThat(avklaringsbehov.åpne().all { it.definisjon == Definisjon.AVKLAR_LOVVALG_MEDLEMSKAP })
        }

        // Trigger manuell vurdering
        løsAvklaringsBehov(
            behandling, AvklarLovvalgMedlemskapLøsning(
                manuellVurderingForLovvalgMedlemskap = ManuellVurderingForLovvalgMedlemskap(
                    LovvalgVedSøknadsTidspunkt("crazy lovvalgsland vurdering", EØSLand.NOR),
                    MedlemskapVedSøknadsTidspunkt("crazy medlemskap vurdering", false), false
                ),
                behovstype = AvklaringsbehovKode.`5017`
            )
        )
        util.ventPåSvar()

        // Validér avklaring
        dataSource.transaction { connection ->
            val avklaringsbehov = hentAvklaringsbehov(behandling.id, connection)
            assertThat(avklaringsbehov.åpne().none())
        }

        // Validér riktig resultat

        val vilkårsResultat = hentVilkårsresultat(behandling.id).finnVilkår(Vilkårtype.LOVVALG).vilkårsperioder()
        assertTrue(vilkårsResultat.none { it.erOppfylt() })
        assertThat(Avslagsårsak.IKKE_MEDLEM == vilkårsResultat.first().avslagsårsak)

    }

    private fun løsAvklaringsBehov(
        behandling: Behandling,
        avklaringsBehovLøsning: AvklaringsbehovLøsning,
        bruker: Bruker = Bruker("SAKSBEHANDLER"),
        ingenEndringIGruppe: Boolean = false
    ) {
        dataSource.transaction {
            AvklaringsbehovHendelseHåndterer(
                AvklaringsbehovOrkestrator(
                    it,
                    BehandlingHendelseServiceImpl(
                        FlytJobbRepository(it),
                        BrevbestillingRepositoryImpl(it),
                        SakService(SakRepositoryImpl(it))
                    )
                ), AvklaringsbehovRepositoryImpl(it), BehandlingRepositoryImpl(it)
            ).håndtere(
                behandling.id, LøsAvklaringsbehovHendelse(
                    løsning = avklaringsBehovLøsning,
                    behandlingVersjon = behandling.versjon,
                    bruker = bruker,
                    ingenEndringIGruppe = ingenEndringIGruppe
                )
            )
        }
    }

    private fun hentPerson(ident: Ident): Person {
        var person: Person? = null
        dataSource.transaction {
            person = PersonRepositoryImpl(it).finnEllerOpprett(listOf(ident))
        }
        return person!!
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
        return dataSource.transaction(readOnly = true) { connection ->
            val finnSisteBehandlingFor = BehandlingRepositoryImpl(connection).finnSisteBehandlingFor(sakId)
            requireNotNull(finnSisteBehandlingFor)
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

    private fun hentAlleAvklaringsbehov(behandlingId: BehandlingId): List<Avklaringsbehov> {
        return dataSource.transaction(readOnly = true) {
            AvklaringsbehovRepositoryImpl(it).hentAvklaringsbehovene(
                behandlingId
            ).alle()
        }
    }

    private fun sendInnDokument(
        ident: Ident,
        dokumentMottattPersonHendelse: DokumentMottattPersonHendelse
    ) {
        hendelsesMottak.håndtere(ident, dokumentMottattPersonHendelse)
    }


    private fun hentBrevAvType(behandling: Behandling, typeBrev: TypeBrev) =
        dataSource.transaction(readOnly = true) {
            BrevbestillingRepositoryImpl(it).hent(behandling.id)
                .first { it.typeBrev == typeBrev }
        }

    private fun løsFramTilForutgåendeMedlemskap(
        behandling: Behandling,
        sak: Sak,
        harYrkesskade: Boolean = false,
        ident: Ident
    ) {
        val person = TestPerson(
            identer = setOf(ident),
            statsborgerskap = listOf(PdlStatsborgerskap("MAC", LocalDate.now().minusYears(5), LocalDate.now())),
            yrkesskade = if (harYrkesskade) listOf(TestYrkesskade()) else emptyList(),
            personStatus = listOf(
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
                ),
            )
        )
        FakePersoner.leggTil(person)

        var behandling = behandling
        løsSykdom(behandling)
        util.ventPåSvar(sak.id.toLong(), behandling.id.toLong())
        behandling = hentBehandling(sak.id)

        løsAvklaringsBehov(
            behandling,
            AvklarBistandsbehovLøsning(
                bistandsVurdering = BistandVurderingDto(
                    begrunnelse = "Trenger hjelp fra nav",
                    erBehovForAktivBehandling = true,
                    erBehovForArbeidsrettetTiltak = false,
                    erBehovForAnnenOppfølging = null
                ),
            ),
        )

        util.ventPåSvar(sak.id.toLong(), behandling.id.toLong())
        behandling = hentBehandling(sak.id)

        val alleAvklaringsbehov = hentAlleAvklaringsbehov(behandling.id)
        løsAvklaringsBehov(
            behandling,
            KvalitetssikringLøsning(
                alleAvklaringsbehov
                    .filter { behov -> behov.erTotrinn() }
                    .map { behov ->
                        TotrinnsVurdering(
                            behov.definisjon.kode,
                            true,
                            "begrunnelse",
                            emptyList()
                        )
                    }),
        )

        util.ventPåSvar(sak.id.toLong(), behandling.id.toLong())
        behandling = hentBehandling(sak.id)
        if (harYrkesskade) {
            løsAvklaringsBehov(
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
        }

        behandling = hentBehandling(sak.id)
        util.ventPåSvar(sak.id.toLong(), behandling.id.toLong())
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
        util.ventPåSvar(sak.id.toLong(), behandling.id.toLong())
    }
}
