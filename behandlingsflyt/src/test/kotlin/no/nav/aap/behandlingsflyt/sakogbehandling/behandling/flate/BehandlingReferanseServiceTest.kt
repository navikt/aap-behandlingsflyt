package no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate

import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.httpklient.exception.VerdiIkkeFunnetException
import org.junit.jupiter.api.AutoClose
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BehandlingReferanseServiceTest {
    @AutoClose
    private val dataSource = TestDataSource()

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