package no.nav.aap.behandlingsflyt.repository.behandling.tilkjentytelse

import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.Reduksjon11_9
import no.nav.aap.behandlingsflyt.help.FakePdlGateway
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.test.ident
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Beløp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class Reduksjon11_9RepositoryImplTest {

    val database = InitTestDatabase.freshDatabase()
    val periode = Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2027, 12, 31))
    val reduksjon_brudd_1 = Reduksjon11_9(LocalDate.of(2025, 1, 1), Beløp(1000))
    val reduksjon_rimelig_grunn = Reduksjon11_9(LocalDate.of(2025, 1, 2), Beløp(0))
    val reduksjon_brudd_2 = Reduksjon11_9(LocalDate.of(2025, 1, 3), Beløp(500))

    @Test
    fun `skal lagre ned reduksjoner `() {
        database.transaction { connection ->
            val reduksjon11_9repository = Reduksjon11_9RepositoryImpl(connection)
            val behandling = lagSakOgBehandling(connection)

            reduksjon11_9repository.lagre(behandling.id, listOf(reduksjon_brudd_1, reduksjon_rimelig_grunn, reduksjon_brudd_2))
            val reduksjoner = reduksjon11_9repository.hent(behandling.id)

            assertThat(reduksjoner).hasSize(3)
            assertThat(reduksjoner).contains(reduksjon_brudd_1)
            assertThat(reduksjoner).contains(reduksjon_brudd_2)
            assertThat(reduksjoner).contains(reduksjon_rimelig_grunn)
        }
    }

    @Test
    fun `skal oppdatere reduksjoner `() {
        database.transaction { connection ->
            val reduksjon11_9repository = Reduksjon11_9RepositoryImpl(connection)
            val behandling = lagSakOgBehandling(connection)

            reduksjon11_9repository.lagre(behandling.id, listOf(reduksjon_brudd_1, reduksjon_rimelig_grunn, reduksjon_brudd_2))
            val reduksjoner = reduksjon11_9repository.hent(behandling.id)
            assertThat(reduksjoner).hasSize(3)

            reduksjon11_9repository.lagre(behandling.id, listOf(reduksjon_brudd_1))
            val reduksjonerOppdatert = reduksjon11_9repository.hent(behandling.id)
            assertThat(reduksjonerOppdatert).hasSize(1)

            reduksjon11_9repository.lagre(behandling.id, listOf())
            val reduksjonerOppdatertTom = reduksjon11_9repository.hent(behandling.id)
            assertThat(reduksjonerOppdatertTom).isEmpty()
        }
    }

    @Test
    fun `skal slette reduksjoner `() {
        database.transaction { connection ->
            val reduksjon11_9repository = Reduksjon11_9RepositoryImpl(connection)
            val behandling = lagSakOgBehandling(connection)

            reduksjon11_9repository.lagre(behandling.id, listOf(reduksjon_brudd_1, reduksjon_rimelig_grunn, reduksjon_brudd_2))
            val reduksjoner = reduksjon11_9repository.hent(behandling.id)

            assertThat(reduksjoner).hasSize(3)
            reduksjon11_9repository.slett(behandling.id)

            val reduksjonerOppdatert = reduksjon11_9repository.hent(behandling.id)
            assertThat(reduksjonerOppdatert).hasSize(0)
        }
    }

    @Test
    fun `skal ikke lagre ned reduksjoner dersom det ikke finnes noen`() {
        database.transaction { connection ->
            val reduksjon11_9repository = Reduksjon11_9RepositoryImpl(connection)
            val behandling = lagSakOgBehandling(connection)
            reduksjon11_9repository.lagre(behandling.id, emptyList())
            val reduksjoner = reduksjon11_9repository.hent(behandling.id)
            assertThat(reduksjoner).isEmpty()
        }
    }


    private fun lagSakOgBehandling(connection: DBConnection): Behandling {
        val ident = ident()
        val sak = PersonOgSakService(
            FakePdlGateway,
            PersonRepositoryImpl(connection),
            SakRepositoryImpl(connection)
        ).finnEllerOpprett(
            ident,
            periode
        )
        return finnEllerOpprettBehandling(connection, sak)
    }

}