package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid

import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokument
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepositoryImpl
import no.nav.aap.behandlingsflyt.help.FakePdlGateway
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.dokument.arbeid.MeldekortRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.ident
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDatabase
import no.nav.aap.komponenter.dbtest.TestDatabaseExtension
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.TimerArbeid
import no.nav.aap.verdityper.dokument.Kanal
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import java.time.LocalDateTime
import javax.sql.DataSource
import kotlin.random.Random

@ExtendWith(TestDatabaseExtension::class)
class BehandlingOgMeldekortServiceImplTest {
    @TestDatabase
    lateinit var dataSource: DataSource

    private companion object {
        private val testPeriode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))
        private val testIdent = ident()
    }

    @Test
    fun hentAlleForIdent() {
        dataSource.transaction { connection ->

            // SETUP
            val behandlingRepositoryImpl = BehandlingRepositoryImpl(connection)
            val mottattDokumentRepository = MottattDokumentRepositoryImpl(connection)
            val meldekortRepository = MeldekortRepositoryImpl(connection)
            val personRepository = PersonRepositoryImpl(connection)
            val sakRepository = SakRepositoryImpl(connection)
            val personOgSakService = PersonOgSakService(
                FakePdlGateway, personRepository, sakRepository
            )

            val meldekortogBehandlingServiceImpl = BehandlingOgMeldekortServiceImpl(
                behandlingRepositoryImpl,
                meldekortRepository,
                mottattDokumentRepository,
                personOgSakService,
            )

            val testSak = personOgSakService.finnEllerOpprett(testIdent, testPeriode)

            val førsteMeldekortLevertPå = testSak.opprettetTidspunkt.plusDays(17)

            // Første meldeperiode
            opprettMeldekortForEnPeriode(
                testSak,
                behandlingRepositoryImpl,
                mottattDokumentRepository,
                meldekortRepository,
                typeBehandling = TypeBehandling.Førstegangsbehandling,
                mottattTidspunkt = førsteMeldekortLevertPå
            )

            // Andre meldeperiode
            opprettMeldekortForEnPeriode(
                testSak,
                behandlingRepositoryImpl,
                mottattDokumentRepository,
                meldekortRepository,
                typeBehandling = TypeBehandling.Revurdering,
                mottattTidspunkt = førsteMeldekortLevertPå.plusWeeks(2)
            )

            // Sjekk at vi fikk opp de to meldekortene og behandlingene vi nettopp lagde
            val meldekortene = meldekortogBehandlingServiceImpl.hentAlle(testIdent)
            assertThat(meldekortene).hasSize(2)
            meldekortene.forEach { (behandling, meldekortListe) ->
                assertThat(behandling.sakId).isEqualTo(testSak.id)
                assertThat(meldekortListe).hasSize(1)
                assertThat(meldekortListe.first().timerArbeidPerPeriode).hasSize(3)
            }

            val meldekorteneIgjen = meldekortogBehandlingServiceImpl.hentAlle(testSak)
            assertThat(meldekorteneIgjen).hasSize(2)

            // sjekk at vi fikk samme resultat ved de to metodene
            meldekortene.forEachIndexed { index, (behandling, meldekortListe) ->
                val (behandlingIgjen, meldekortListeIgjen) = meldekorteneIgjen[index]
                assertThat(behandling.id).isEqualTo(behandlingIgjen.id)
                assertThat(behandling.referanse).isEqualTo(behandlingIgjen.referanse)
                assertThat(meldekortListe).isEqualTo(meldekortListeIgjen)
            }

        }

    }

    private fun opprettMeldekortForEnPeriode(
        sak: Sak,
        behandlingRepositoryImpl: BehandlingRepositoryImpl,
        mottattDokumentRepository: MottattDokumentRepositoryImpl,
        meldekortRepository: MeldekortRepositoryImpl,
        typeBehandling: TypeBehandling,
        mottattTidspunkt: LocalDateTime
    ) {

        val behandling = behandlingRepositoryImpl.opprettBehandling(
            sakId = sak.id, typeBehandling, null, VurderingsbehovOgÅrsak(
                emptyList(), ÅrsakTilOpprettelse.MELDEKORT
            )
        )

        val mottattDokument = MottattDokument(
            referanse = tilfeldigReferanse(),
            sakId = sak.id,
            behandlingId = behandling.id,
            mottattTidspunkt = mottattTidspunkt,
            type = InnsendingType.MELDEKORT,
            kanal = Kanal.DIGITAL,
            status = Status.BEHANDLET,
            strukturertDokument = null,
        )
        mottattDokumentRepository.lagre(mottattDokument)

        val periodestart = mottattTidspunkt.toLocalDate()
        meldekortRepository.lagre(
            behandling.id, setOf(
                Meldekort(
                    mottattDokument.referanse.asJournalpostId, setOf(
                        ArbeidIPeriode(
                            Periode(periodestart, periodestart.plusDays(1)), TimerArbeid(4.0.toBigDecimal())
                        ), ArbeidIPeriode(
                            Periode(periodestart.plusDays(2), periodestart.plusDays(3)), TimerArbeid(4.5.toBigDecimal())
                        ), ArbeidIPeriode(
                            Periode(periodestart.plusDays(4), periodestart.plusDays(5)), TimerArbeid(7.5.toBigDecimal())
                        )
                    ), mottattTidspunkt = mottattTidspunkt
                )
            )
        )

    }

    private fun tilfeldigReferanse(): InnsendingReferanse =
        InnsendingReferanse(InnsendingReferanse.Type.JOURNALPOST, Random.nextLong(100000, 999999).toString())

}
