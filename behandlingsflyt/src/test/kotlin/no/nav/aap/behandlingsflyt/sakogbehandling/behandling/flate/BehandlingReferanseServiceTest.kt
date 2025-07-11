package no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate

import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.test.FreshDatabaseExtension
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.httpklient.exception.VerdiIkkeFunnetException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import java.util.*
import javax.sql.DataSource

@ExtendWith(FreshDatabaseExtension::class)
class BehandlingReferanseServiceTest(val source: DataSource) {
    @Test
    fun `kaster NoSuchElementException hvis behandling ikke funnet`() {
        source.transaction { connection ->
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