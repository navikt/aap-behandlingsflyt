package no.nav.aap.behandlingsflyt.prosessering


import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Kvote
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.MeldepliktStatus
import no.nav.aap.behandlingsflyt.faktagrunnlag.Faktagrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.ArbeidsGradering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.Underveisperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokument
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepositoryImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.StrukturertDokument
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Meldekort
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Status
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.help.ident
import no.nav.aap.behandlingsflyt.help.opprettSak
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.ArbeidIPeriodeV0
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.MeldekortV0
import no.nav.aap.behandlingsflyt.prosessering.datadeling.DatadelingMeldekortService
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.meldeperiode.MeldeperiodeRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.underveis.UnderveisRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.dokument.arbeid.MeldekortRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.postgresRepositoryRegistry
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.minimalGatewayProvider
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Dagsatser
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.komponenter.verdityper.TimerArbeid
import no.nav.aap.verdityper.dokument.Kanal
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.LocalDate


class DatadelingMeldekortServiceTest {
    private companion object {
        private val testPeriode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))
        private val testIdent = ident()
        private lateinit var dataSource: TestDataSource

        @BeforeAll
        @JvmStatic
        fun setup() {
            dataSource = TestDataSource()
        }

        @AfterAll
        @JvmStatic
        fun tearDown() = dataSource.close()
    }

    @Test
    fun `opprettKontraktObjekter kan hente opp meldekort og mappe dem til kontrakt`() {
        dataSource.transaction { connection ->

            // SETUP
            val mottattDokumentRepository = MottattDokumentRepositoryImpl(connection)
            val meldekortRepository = MeldekortRepositoryImpl(connection)
            val underveisRepository = UnderveisRepositoryImpl(connection)
            val meldeperiodeRepository = MeldeperiodeRepositoryImpl(connection)

            val testMeg = DatadelingMeldekortService(
                postgresRepositoryRegistry.provider(connection), minimalGatewayProvider()
            )

            // Legg inn testdata
            val testSak = opprettSak(connection, testIdent, testPeriode.fom)

            // Aktiv behandling med meldekort
            val testBehandling = finnEllerOpprettBehandling(connection, testSak)

            val periodeStart = testSak.rettighetsperiode.fom

            val meldeperioder = lagreMeldeperioder(periodeStart, meldeperiodeRepository, testBehandling)

            lagreUnderveisperioder(meldeperioder, testBehandling, underveisRepository)


            val testMeldekort = lagreMeldekort(
                InnsendingReferanse(InnsendingReferanse.Type.JOURNALPOST, 100001.toString()),
                meldeperioder.first(),
                testSak,
                testBehandling,
                mottattDokumentRepository,
                meldekortRepository,
            )

            // Sjekk at vi får opp meldekortet vi nettopp lagret
            val hentetFrem = meldekortRepository.hentHvisEksisterer(testBehandling.id)
            assertThat(hentetFrem).isNotNull
            assertThat(hentetFrem!!.meldekort()).isNotEmpty
            val meldekortetsPeriode = testMeg.finnMeldekortetsPeriode(hentetFrem.meldekort().first(), meldeperioder)

            assertThat(meldekortetsPeriode).isEqualTo(meldeperioder.first())

            // Start selve testen
            val testet = testMeg.opprettKontraktObjekter(testSak.id, testBehandling.id)
            assertThat(testet).isNotNull
            assertThat(testet).hasSize(1)

            testet.first().apply {
                // test mapping
                assertThat(saksnummer).isEqualTo(testSak.saksnummer)
                assertThat(behandlingId).isEqualTo(testBehandling.id.id)
                assertThat(personIdent).isEqualTo(testIdent.identifikator)

                assertThat(mottattTidspunkt).isEqualTo(testMeldekort.mottattTidspunkt)
                assertThat(meldeperiodeFom).isEqualTo(meldeperioder.first().fom)
                assertThat(meldeperiodeTom).isEqualTo(meldeperioder.first().tom)

                for (dag in timerArbeidPerPeriode) {
                    val antall = meldeperioder.filter { it.overlapper(Periode(dag.periodeFom, dag.periodeTom)) }
                    assertThat(antall).hasSize(1)
                }
                val timetall = timerArbeidPerPeriode.sumOf { it.timerArbeidet }
                assertThat(timetall).isEqualTo(16.0.toBigDecimal())

                assertThat(this.arbeidsgradering).isNotEmpty()
            }

        }
    }
}

private fun lagreUnderveisperioder(
    meldeperioder: List<Periode>,
    testBehandling: Behandling,
    underveisRepository: UnderveisRepositoryImpl
) {
    underveisRepository.lagre(
        testBehandling.id, meldeperioder.map {
            Underveisperiode(
                periode = it,
                meldePeriode = it,
                utfall = Utfall.OPPFYLT,
                rettighetsType = RettighetsType.BISTANDSBEHOV,
                avslagsårsak = null,
                grenseverdi = Prosent.`70_PROSENT`,
                institusjonsoppholdReduksjon = Prosent.`0_PROSENT`,
                arbeidsgradering = ArbeidsGradering(
                    totaltAntallTimer = TimerArbeid(10.toBigDecimal()),
                    andelArbeid = Prosent.`30_PROSENT`,
                    fastsattArbeidsevne = Prosent.`50_PROSENT`,
                    gradering = Prosent.`50_PROSENT`,
                    opplysningerMottatt = it.tom.plusDays(1)
                ),
                trekk = Dagsatser(0),
                brukerAvKvoter = setOf(Kvote.ORDINÆR),
                meldepliktStatus = MeldepliktStatus.MELDT_SEG,
                meldepliktGradering = Prosent.`100_PROSENT`
            )
        },
        input = object : Faktagrunnlag {}
    )
}

private fun lagreMeldeperioder(
    periodeStart: LocalDate,
    meldeperiodeRepository: MeldeperiodeRepositoryImpl,
    testBehandling: Behandling
): List<Periode> {
    val førsteMeldeperiode = Periode(periodeStart, periodeStart.plusDays(13))
    val aktuellPeriode = Periode(periodeStart.plusDays(28), periodeStart.plusDays(41))
    meldeperiodeRepository.lagreFørsteMeldeperiode(testBehandling.id, førsteMeldeperiode)
    val meldeperioderDb = meldeperiodeRepository.hentMeldeperioder(testBehandling.id, aktuellPeriode)
    assertThat(meldeperioderDb).hasSize(3)
    return meldeperioderDb
}

private fun lagreMeldekort(
    referanse: InnsendingReferanse,
    meldeperiode: Periode,
    testSak: Sak,
    testBehandling: Behandling,
    mottattDokumentRepository: MottattDokumentRepositoryImpl,
    meldekortRepository: MeldekortRepositoryImpl
): Meldekort {
    val mottattTidspunkt = meldeperiode.tom.plusDays(1).atStartOfDay()
    val periodeStart = meldeperiode.fom
    val strukturertDokument = MeldekortV0(
        harDuArbeidet = true,
        timerArbeidPerPeriode = listOf(
            ArbeidIPeriodeV0(
                fraOgMedDato = periodeStart, tilOgMedDato = periodeStart.plusDays(1), timerArbeid = 4.0
            ), ArbeidIPeriodeV0(
                periodeStart.plusDays(2), periodeStart.plusDays(3),
                4.5
            ), ArbeidIPeriodeV0(
                periodeStart.plusDays(4), periodeStart.plusDays(5),
                7.5
            )
        ),
        begrunnelse = "...",
        opprettetAv = "..."
    )
    val mottattMeldekort = MottattDokument(
        referanse = referanse,
        sakId = testSak.id,
        behandlingId = testBehandling.id,
        mottattTidspunkt = mottattTidspunkt,
        type = InnsendingType.MELDEKORT,
        kanal = Kanal.DIGITAL,
        status = Status.BEHANDLET,
        strukturertDokument = StrukturertDokument(strukturertDokument),
    )
    mottattDokumentRepository.lagre(mottattMeldekort)

    meldekortRepository.lagre(
        testBehandling.id, setOf(
            Meldekort.fraKontrakt(
                mottattMeldekort.referanse.asJournalpostId,
                mottattMeldekort.mottattTidspunkt,
                mottattMeldekort.opprettetTid,
                strukturertDokument
            )

        )
    )

    return meldekortRepository.hent(testBehandling.id).meldekort().first()
}

