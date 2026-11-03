package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.register.arena

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.arena.ArenaMaksdato
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.help.opprettSak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.LocalDate

class ArenaMaksdatoRepositoryImplTest {

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

    private fun opprettBehandlingId(): BehandlingId = dataSource.transaction { connection ->
        val sak = opprettSak(connection, 1 januar 2026)
        finnEllerOpprettBehandling(connection, sak).id
    }

    @Test
    fun `lagre og hent Arena maksdato`() {
        val behandlingId = opprettBehandlingId()
        val arenaMaksdato = ArenaMaksdato(
            arenaSaksnummer = "123",
            status = "LØPENDE",
            maksdato = LocalDate.of(2026, 12, 31),
            vedtakTil = LocalDate.of(2026, 6, 30),
            vedtakId = 42,
        )

        dataSource.transaction { connection ->
            ArenaMaksdatoRepositoryImpl(connection).lagre(behandlingId, arenaMaksdato)
        }

        val grunnlag = dataSource.transaction(readOnly = true) { connection ->
            ArenaMaksdatoRepositoryImpl(connection).hentHvisEksisterer(behandlingId)
        }

        assertThat(grunnlag?.arenaMaksdato).isEqualTo(arenaMaksdato)
    }

    @Test
    fun `ny lagring erstatter gammelt grunnlag`() {
        val behandlingId = opprettBehandlingId()
        val gammelt = ArenaMaksdato("123", "LØPENDE", LocalDate.of(2026, 12, 31), null, 42)
        val nytt = gammelt.copy(status = "AVSLUTTET")

        dataSource.transaction { connection ->
            ArenaMaksdatoRepositoryImpl(connection).lagre(behandlingId, gammelt)
            ArenaMaksdatoRepositoryImpl(connection).lagre(behandlingId, nytt)
        }

        val grunnlag = dataSource.transaction(readOnly = true) { connection ->
            ArenaMaksdatoRepositoryImpl(connection).hentHvisEksisterer(behandlingId)
        }

        assertThat(grunnlag?.arenaMaksdato).isEqualTo(nytt)
    }

    @Test
    fun `lagre og hente grunnlag uten Arena-sak`() {
        val behandlingId = opprettBehandlingId()

        dataSource.transaction { connection ->
            ArenaMaksdatoRepositoryImpl(connection).lagre(behandlingId, null)
        }

        val grunnlag = dataSource.transaction(readOnly = true) { connection ->
            ArenaMaksdatoRepositoryImpl(connection).hentHvisEksisterer(behandlingId)
        }

        assertThat(grunnlag).isNotNull
        assertThat(grunnlag!!.arenaMaksdato).isNull()
    }

    @Test
    fun `kopier kopierer grunnlag til ny behandling`() {
        val fraBehandlingId = opprettBehandlingId()
        val tilBehandlingId = opprettBehandlingId()
        val arenaMaksdato = ArenaMaksdato("123", "LØPENDE", null, LocalDate.of(2026, 6, 30), 42)

        dataSource.transaction { connection ->
            ArenaMaksdatoRepositoryImpl(connection).lagre(fraBehandlingId, arenaMaksdato)
            ArenaMaksdatoRepositoryImpl(connection).kopier(fraBehandlingId, tilBehandlingId)
        }

        val kopiert = dataSource.transaction(readOnly = true) { connection ->
            ArenaMaksdatoRepositoryImpl(connection).hentHvisEksisterer(tilBehandlingId)
        }

        assertThat(kopiert?.arenaMaksdato).isEqualTo(arenaMaksdato)
    }
}
