package no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate

import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.httpklient.exception.VerdiIkkeFunnetException
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.*

class BehandlingReferanseServiceTest {
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
    fun `kaster NoSuchElementException hvis behandling ikke funnet`() {
        dataSource.transaction { connection ->
            val repo = BehandlingRepositoryImpl(connection)
            val service = BehandlingReferanseService(repo)
            assertThrows<VerdiIkkeFunnetException> {
                service.behandling(
                    BehandlingReferanse(
                        UUID.randomUUID()
                    )
                )
            }
        }
    }
}