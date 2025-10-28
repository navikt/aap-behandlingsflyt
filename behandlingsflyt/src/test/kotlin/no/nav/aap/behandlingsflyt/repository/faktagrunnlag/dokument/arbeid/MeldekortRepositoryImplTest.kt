package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.dokument.arbeid

import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokument
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepositoryImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.ArbeidIPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Meldekort
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Status
import no.nav.aap.behandlingsflyt.help.FakePdlGateway
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.ident
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.TimerArbeid
import no.nav.aap.verdityper.dokument.JournalpostId
import no.nav.aap.verdityper.dokument.Kanal
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AutoClose
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

class MeldekortRepositoryImplTest {

    companion object {
        private val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))
    }

    @AutoClose
    private val dataSource = TestDataSource()

    @Test
    fun `Skal lagre ned meldekort på sak`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)

            val meldekortRepositoryImpl = MeldekortRepositoryImpl(connection)
            val dokumentRepositoryImpl = MottattDokumentRepositoryImpl(connection)

            val meldekortFørInnsending = meldekortRepositoryImpl.hentHvisEksisterer(behandling.id)
            assertThat(meldekortFørInnsending).isNull()

            val mottattMeldekort = meldekortDokument(sak, behandling, "1", LocalDateTime.now().withNano(0).minusMinutes(5))
            val meldekortPeriode = Periode(periode.fom, periode.fom.plusDays(1))
            val meldekortInitielt = Meldekort(
                journalpostId = mottattMeldekort.referanse.asJournalpostId,
                timerArbeidPerPeriode = setOf(
                    ArbeidIPeriode(periode = meldekortPeriode, timerArbeid = TimerArbeid(BigDecimal.TEN))
                ), mottattTidspunkt = mottattMeldekort.mottattTidspunkt
            )
            dokumentRepositoryImpl.lagre(mottattMeldekort)
            meldekortRepositoryImpl.lagre(behandling.id, meldekortene = setOf(meldekortInitielt))
            val meldekortInitieltDb = meldekortRepositoryImpl.hentHvisEksisterer(behandling.id)
            assertThat(meldekortInitieltDb).isNotNull
            assertThat(meldekortInitieltDb!!.meldekort()).hasSize(1)
            assertThat(meldekortInitieltDb.meldekort().first()).isEqualTo(meldekortInitielt)

            // Korrigert meldekort
            val mottattMeldekortKorrigert = meldekortDokument(sak, behandling, "2", LocalDateTime.now().withNano(0))
            dokumentRepositoryImpl.lagre(mottattMeldekortKorrigert)
            val meldekortKorrigert = Meldekort(
                journalpostId = mottattMeldekortKorrigert.referanse.asJournalpostId,
                timerArbeidPerPeriode = setOf(
                    ArbeidIPeriode(periode = meldekortPeriode, timerArbeid = TimerArbeid(BigDecimal.ZERO))
                ), mottattTidspunkt = mottattMeldekortKorrigert.mottattTidspunkt
            )

            meldekortRepositoryImpl.lagre(behandling.id, setOf(meldekortInitielt, meldekortKorrigert))
            val meldekortKorrigertDb = meldekortRepositoryImpl.hentHvisEksisterer(behandling.id)
            assertThat(meldekortKorrigertDb).isNotNull
            assertThat(meldekortKorrigertDb!!.meldekort()).isEqualTo(listOf(meldekortInitielt, meldekortKorrigert))

        }
    }

    private fun meldekortDokument(
        sak: Sak,
        behandling: Behandling,
        journalpostId: String,
        mottattTidspunkt: LocalDateTime = LocalDateTime.now().withNano(0)

    ) = MottattDokument(
        referanse = InnsendingReferanse(JournalpostId(journalpostId)),
        sakId = sak.id,
        behandlingId = behandling.id,
        mottattTidspunkt = mottattTidspunkt,
        type = InnsendingType.MELDEKORT,
        kanal = Kanal.DIGITAL,
        status = Status.BEHANDLET,
        strukturertDokument = null
    )

    private fun sak(connection: DBConnection): Sak {
        return PersonOgSakService(
            FakePdlGateway,
            PersonRepositoryImpl(connection),
            SakRepositoryImpl(connection)
        ).finnEllerOpprett(ident(), periode)
    }
}