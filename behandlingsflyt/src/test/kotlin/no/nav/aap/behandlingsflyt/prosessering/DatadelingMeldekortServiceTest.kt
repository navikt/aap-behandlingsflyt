package no.nav.aap.behandlingsflyt.prosessering


import no.nav.aap.behandlingsflyt.behandling.underveis.regler.MeldepliktStatus
import no.nav.aap.behandlingsflyt.behandling.vilkår.alder.Aldersgrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.ArbeidsGradering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.Underveisperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisperiodeId
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisÅrsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokument
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepositoryImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.ArbeidIPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Meldekort
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Status
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.help.FakePdlGateway
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.meldeperiode.MeldeperiodeRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.underveis.UnderveisRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.dokument.arbeid.MeldekortRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.ident
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDatabase
import no.nav.aap.komponenter.dbtest.TestDatabaseExtension
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Dagsatser
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.komponenter.verdityper.TimerArbeid
import no.nav.aap.verdityper.dokument.Kanal
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal
import java.time.LocalDate
import javax.sql.DataSource


@ExtendWith(TestDatabaseExtension::class)
class DatadelingMeldekortServiceTest {
    @TestDatabase
    lateinit var dataSource: DataSource

    private companion object {
        private val testPeriode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))
        private val testIdent = ident()
    }

    @Test
    fun `opprettKontraktObjekter kan hente opp meldekort og mappe dem til kontrakt`() {
        dataSource.transaction { connection ->

            // SETUP
            val behandlingRepository = BehandlingRepositoryImpl(connection)
            val mottattDokumentRepository = MottattDokumentRepositoryImpl(connection)
            val meldekortRepository = MeldekortRepositoryImpl(connection)
            val personRepository = PersonRepositoryImpl(connection)
            val sakRepository = SakRepositoryImpl(connection)
            val personOgSakService = PersonOgSakService(
                FakePdlGateway, personRepository, sakRepository
            )
            val underveisRepository = UnderveisRepositoryImpl(connection)
            val meldeperiodeRepository = MeldeperiodeRepositoryImpl(connection)

            val testMeg = DatadelingMeldekortService(
                sakRepository,
                underveisRepository,
                meldekortRepository,
                meldeperiodeRepository
            )

            // Legg inn testdata
            val testSak = personOgSakService.finnEllerOpprett(testIdent, testPeriode)

            // aktiv behandling med meldekort
            val testBehandling = behandlingRepository.opprettBehandling(
                sakId = testSak.id,
                typeBehandling = TypeBehandling.Førstegangsbehandling,
                forrigeBehandlingId = null,
                vurderingsbehovOgÅrsak = VurderingsbehovOgÅrsak(
                    emptyList(), ÅrsakTilOpprettelse.MELDEKORT
                )
            )

            val periodeStart = testSak.opprettetTidspunkt.plusDays(1).toLocalDate()

            val meldeperioder = lagreMeldeperioder(periodeStart, meldeperiodeRepository, testBehandling)
            val testUnderveisGrunnlag = lagreUnderveisGrunnlag(
                underveisRepository, testBehandling, meldeperioder,
                testPeriode
            )
            val testUnderveisperiode = testUnderveisGrunnlag.perioder.first()

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

            // Start selve testen
            val testet = testMeg.opprettKontraktObjekter(testSak.id, testBehandling.id)
            assertThat(testet).isNotNull
            assertThat(testet).hasSize(1)

            testet.first().apply {
                // test mapping
                assertThat(saksnummer).isEqualTo(testSak.saksnummer)
                assertThat(behandlingId).isEqualTo(testBehandling.id.id)
                assertThat(personIdent).isEqualTo(testIdent.identifikator)

                assertThat(meldeperiodeFom).isEqualTo(meldeperioder.first().fom)
                assertThat(meldeperiodeTom).isEqualTo(meldeperioder.first().tom)

                for (dag in detaljertArbeidIPeriode) {
                    val antall = meldeperioder.filter { it.overlapper(Periode(dag.periodeFom, dag.periodeTom)) }
                    assertThat(antall).hasSize(1)
                    assertThat(dag.mottattTidspunkt).isEqualTo(testMeldekort.mottattTidspunkt)
                    assertThat(dag.journalpostId).isEqualTo(testMeldekort.journalpostId.identifikator)
                }

                val timetall = detaljertArbeidIPeriode.sumOf { it.timerArbeidet }
                assertThat(timetall).isEqualTo(16.0.toBigDecimal())

                assertThat(avslagsårsakKode).isEqualTo(testUnderveisperiode.avslagsårsak?.name)
                assertThat(rettighetsTypeKode).isEqualTo(testUnderveisperiode.rettighetsType?.name)
                assertThat(meldepliktStatusKode).isEqualTo(testUnderveisperiode.meldepliktStatus?.name)
            }

        }
    }

}

private fun lagreUnderveisGrunnlag(
    underveisRepository: UnderveisRepositoryImpl,
    testBehandling: Behandling,
    meldeperioder: List<Periode>,
    testPeriode: Periode,
): UnderveisGrunnlag {
    val testGrunnlag = Aldersgrunnlag(testPeriode, Fødselsdato(LocalDate.now().minusYears(20)))
    underveisRepository.lagre(
        testBehandling.id,
        listOf(testUnderveisperiode(testPeriode, meldeperioder.first())),
        testGrunnlag
    )
    val lagretUnderveisperiode = underveisRepository.hent(testBehandling.id)
    assertThat(lagretUnderveisperiode.perioder).hasSize(1)

    return lagretUnderveisperiode
}

private fun lagreMeldeperioder(
    periodeStart: LocalDate,
    meldeperiodeRepository: MeldeperiodeRepositoryImpl,
    testBehandling: Behandling
): List<Periode> {
    val meldeperioder = listOf(
        Periode(periodeStart, periodeStart.plusDays(13)),
        Periode(periodeStart.plusDays(14), periodeStart.plusDays(27)),
        Periode(periodeStart.plusDays(28), periodeStart.plusDays(41)),
    )
    meldeperiodeRepository.lagre(testBehandling.id, meldeperioder)
    val meldeperioderDb = meldeperiodeRepository.hent(testBehandling.id)
    assertThat(meldeperioderDb).hasSize(meldeperioder.size)
    return meldeperioder
}

private fun testUnderveisperiode(testPeriode: Periode, meldeperiode: Periode): Underveisperiode =
    Underveisperiode(
        testPeriode,
        utfall = Utfall.OPPFYLT,
        rettighetsType = RettighetsType.BISTANDSBEHOV,
        avslagsårsak = UnderveisÅrsak.VARIGHETSKVOTE_BRUKT_OPP,
        grenseverdi = Prosent.`100_PROSENT`,
        arbeidsgradering = ArbeidsGradering(
            totaltAntallTimer = TimerArbeid(
                antallTimer = BigDecimal(0)
            ),
            andelArbeid = Prosent.`100_PROSENT`,
            fastsattArbeidsevne = Prosent.`100_PROSENT`,
            gradering = Prosent.`100_PROSENT`,
            opplysningerMottatt = null,
        ),
        trekk = Dagsatser(0),
        brukerAvKvoter = emptySet(),
        id = UnderveisperiodeId(0),
        institusjonsoppholdReduksjon = Prosent(0),
        meldepliktStatus = MeldepliktStatus.RIMELIG_GRUNN,
        meldePeriode = meldeperiode,
    )

private fun lagreMeldekort(
    referanse: InnsendingReferanse,
    meldeperiode: Periode,
    testSak: Sak,
    testBehandling: Behandling,
    mottattDokumentRepository: MottattDokumentRepositoryImpl,
    meldekortRepository: MeldekortRepositoryImpl
): Meldekort {
    val mottattTidspunkt = meldeperiode.tom.plusDays(1).atStartOfDay()
    val mottattMeldekort = MottattDokument(
        referanse = referanse,
        sakId = testSak.id,
        behandlingId = testBehandling.id,
        mottattTidspunkt = mottattTidspunkt,
        type = InnsendingType.MELDEKORT,
        kanal = Kanal.DIGITAL,
        status = Status.BEHANDLET,
        strukturertDokument = null,
    )
    mottattDokumentRepository.lagre(mottattMeldekort)
    val periodeStart = meldeperiode.fom
    meldekortRepository.lagre(
        testBehandling.id, setOf(
            Meldekort(
                mottattMeldekort.referanse.asJournalpostId, setOf(
                    ArbeidIPeriode(
                        Periode(periodeStart, periodeStart.plusDays(1)), TimerArbeid(4.0.toBigDecimal())
                    ), ArbeidIPeriode(
                        Periode(periodeStart.plusDays(2), periodeStart.plusDays(3)),
                        TimerArbeid(4.5.toBigDecimal())
                    ), ArbeidIPeriode(
                        Periode(periodeStart.plusDays(4), periodeStart.plusDays(5)),
                        TimerArbeid(7.5.toBigDecimal())
                    )
                ), mottattTidspunkt = mottattMeldekort.mottattTidspunkt
            )
        )
    )

    return meldekortRepository.hent(testBehandling.id).meldekort().first()
}

