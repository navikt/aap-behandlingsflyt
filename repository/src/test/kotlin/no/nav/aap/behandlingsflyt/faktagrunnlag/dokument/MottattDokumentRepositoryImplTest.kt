package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument

import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Status
import no.nav.aap.behandlingsflyt.help.FakePdlGateway
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.ident
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.verdityper.dokument.Kanal
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

internal class MottattDokumentRepositoryImplTest {
    companion object {
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
    fun `lagre og hent ut igjen`() {
        // SETUP
        val (sak, behandling) = dataSource.transaction {
            val sak = sak(it)
            val behandling = finnEllerOpprettBehandling(it, sak)
            Pair(sak, behandling)
        }

        val mottattDokument = MottattDokument(
            referanse = InnsendingReferanse(InnsendingReferanse.Type.JOURNALPOST, "referanse"),
            sakId = sak.id,
            behandlingId = behandling.id,
            mottattTidspunkt = LocalDateTime.now(),
            type = InnsendingType.SØKNAD,
            kanal = Kanal.PAPIR,
            status = Status.MOTTATT,
            strukturertDokument = null,
        )

        // ACT
        settInnDokument(mottattDokument)

        // VERIFY
        val res = hentDokumenterAvType(sak, InnsendingType.SØKNAD)

        assertThat(res).hasSize(1)
        assertThat(res.first()).extracting(
            MottattDokument::behandlingId,
            MottattDokument::sakId,
            MottattDokument::kanal
        ).containsExactly(
            mottattDokument.behandlingId, mottattDokument.sakId, mottattDokument.kanal
        )
    }

    @Test
    fun oppdaterStatus() {
        // SETUP
        val (sak, behandling) = dataSource.transaction {
            val sak = sak(it)
            val behandling = finnEllerOpprettBehandling(it, sak)
            Pair(sak, behandling)
        }

        val mottattDokument = MottattDokument(
            referanse = InnsendingReferanse(InnsendingReferanse.Type.JOURNALPOST, "referanse"),
            sakId = sak.id,
            behandlingId = null,
            mottattTidspunkt = LocalDateTime.now(),
            type = InnsendingType.SØKNAD,
            status = Status.MOTTATT,
            kanal = Kanal.PAPIR,
            strukturertDokument = null,
        )

        // ACT
        settInnDokument(mottattDokument)

        // VERIFY
        dataSource.transaction {
            MottattDokumentRepositoryImpl(it).oppdaterStatus(
                dokumentReferanse = mottattDokument.referanse,
                behandlingId = behandling.id,
                sakId = sak.id,
                status = Status.BEHANDLET
            )
        }

        val res = hentDokumenterAvType(sak, InnsendingType.SØKNAD)

        assertThat(res).hasSize(1)
        val hentetDokument = res.first()

        assertThat(hentetDokument.behandlingId).isEqualTo(behandling.id)
        assertThat(hentetDokument.status).isEqualTo(Status.BEHANDLET)
    }

    @Test
    fun hentDokumenterAvType() {
        // SETUP
        val (sak, _) = dataSource.transaction {
            val sak = sak(it)
            val behandling = finnEllerOpprettBehandling(it, sak)
            Pair(sak, behandling)
        }

        val søknadDokument = MottattDokument(
            referanse = InnsendingReferanse(InnsendingReferanse.Type.JOURNALPOST, "referanse"),
            sakId = sak.id,
            behandlingId = null,
            mottattTidspunkt = LocalDateTime.now(),
            type = InnsendingType.SØKNAD,
            status = Status.MOTTATT,
            kanal = Kanal.PAPIR,
            strukturertDokument = null,
        )

        val meldekortDokument = MottattDokument(
            referanse = InnsendingReferanse(InnsendingReferanse.Type.JOURNALPOST, "referanse2"),
            sakId = sak.id,
            behandlingId = null,
            mottattTidspunkt = LocalDateTime.now(),
            type = InnsendingType.MELDEKORT,
            status = Status.MOTTATT,
            kanal = Kanal.DIGITAL,
            strukturertDokument = null,
        )

        // ACT
        settInnDokument(søknadDokument)
        settInnDokument(meldekortDokument)


        val res = hentDokumenterAvType(sak, InnsendingType.SØKNAD)

        // VERIFY
        assertThat(res).hasSize(1)
        assertThat(res.first()).extracting(MottattDokument::behandlingId, MottattDokument::sakId, MottattDokument::type)
            .containsExactly(
                søknadDokument.behandlingId, søknadDokument.sakId, søknadDokument.type
            )

        val res2 = hentDokumenterAvType(sak, InnsendingType.MELDEKORT)

        assertThat(res2).hasSize(1)
        assertThat(res2.first()).extracting(
            MottattDokument::behandlingId, MottattDokument::sakId, MottattDokument::type
        ).containsExactly(
            meldekortDokument.behandlingId, meldekortDokument.sakId, meldekortDokument.type
        )

    }

    private fun settInnDokument(mottattDokument: MottattDokument) {
        dataSource.transaction {
            MottattDokumentRepositoryImpl(it).lagre(
                mottattDokument
            )
        }
    }

    private fun hentDokumenterAvType(sak: Sak, brevkategori: InnsendingType): Set<MottattDokument> {
        val res = dataSource.transaction {
            MottattDokumentRepositoryImpl(it).hentDokumenterAvType(sak.id, brevkategori)
        }
        return res
    }

    private fun sak(connection: DBConnection): Sak {
        return PersonOgSakService(
            FakePdlGateway,
            PersonRepositoryImpl(connection),
            SakRepositoryImpl(connection)
        ).finnEllerOpprett(
            ident(), Periode(LocalDate.now(), LocalDate.now().plusYears(3))
        )
    }
}