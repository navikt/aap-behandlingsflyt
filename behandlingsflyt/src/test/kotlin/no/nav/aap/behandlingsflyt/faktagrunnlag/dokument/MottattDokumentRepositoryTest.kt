package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument

import no.nav.aap.behandlingsflyt.faktagrunnlag.FakePdlGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Status
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Årsak
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.ÅrsakTilBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.ident
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.verdityper.dokument.Kanal
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class MottattDokumentRepositoryTest {

    @Test
    fun `lagre og hent ut igjen`() {
        // SETUP
        val (sak, behandling) = InitTestDatabase.dataSource.transaction {
            val sak = sak(it)
            val behandling = behandling(it, sak)
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
        val (sak, behandling) = InitTestDatabase.dataSource.transaction {
            val sak = sak(it)
            val behandling = behandling(it, sak)
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
        InitTestDatabase.dataSource.transaction {
            MottattDokumentRepository(it).oppdaterStatus(
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
        val (sak, _) = InitTestDatabase.dataSource.transaction {
            val sak = sak(it)
            val behandling = behandling(it, sak)
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

        val pliktkortDokument = MottattDokument(
            referanse = InnsendingReferanse(InnsendingReferanse.Type.JOURNALPOST, "referanse2"),
            sakId = sak.id,
            behandlingId = null,
            mottattTidspunkt = LocalDateTime.now(),
            type = InnsendingType.PLIKTKORT,
            status = Status.MOTTATT,
            kanal = Kanal.DIGITAL,
            strukturertDokument = null,
        )

        // ACT
        settInnDokument(søknadDokument)
        settInnDokument(pliktkortDokument)


        val res = hentDokumenterAvType(sak, InnsendingType.SØKNAD)

        // VERIFY
        assertThat(res).hasSize(1)
        assertThat(res.first()).extracting(MottattDokument::behandlingId, MottattDokument::sakId, MottattDokument::type)
            .containsExactly(
                søknadDokument.behandlingId, søknadDokument.sakId, søknadDokument.type
            )

        val res2 = hentDokumenterAvType(sak, InnsendingType.PLIKTKORT)

        assertThat(res2).hasSize(1)
        assertThat(res2.first()).extracting(
            MottattDokument::behandlingId, MottattDokument::sakId, MottattDokument::type
        ).containsExactly(
            pliktkortDokument.behandlingId, pliktkortDokument.sakId, pliktkortDokument.type
        )

    }

    private fun settInnDokument(mottattDokument: MottattDokument) {
        InitTestDatabase.dataSource.transaction {
            MottattDokumentRepository(it).lagre(
                mottattDokument
            )
        }
    }

    private fun hentDokumenterAvType(sak: Sak, brevkategori: InnsendingType): Set<MottattDokument> {
        val res = InitTestDatabase.dataSource.transaction {
            MottattDokumentRepository(it).hentDokumenterAvType(sak.id, brevkategori)
        }
        return res
    }

    private fun sak(connection: DBConnection): Sak {
        return PersonOgSakService(connection, FakePdlGateway).finnEllerOpprett(
            ident(), Periode(LocalDate.now(), LocalDate.now().plusYears(3))
        )
    }

    private fun behandling(connection: DBConnection, sak: Sak): Behandling {
        return SakOgBehandlingService(connection).finnEllerOpprettBehandling(
            sak.saksnummer, listOf(Årsak(ÅrsakTilBehandling.MOTTATT_SØKNAD))
        ).behandling
    }
}