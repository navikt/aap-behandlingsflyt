package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

class AvklaringsbehovsLøserTest {

    @Test
    fun `alle subtyper skal ha unik verdi`() {
        val utledSubtypes = AvklaringsbehovsLøser::class.sealedSubclasses
        InitTestDatabase.dataSource.transaction { dbConnection ->
            val løsningSubtypes = utledSubtypes.map {
                it.constructors
                    .find { it.parameters.singleOrNull()?.type?.classifier == DBConnection::class }!!
                    .call(dbConnection).forBehov()
            }.toSet()

            Assertions.assertThat(løsningSubtypes).hasSize(utledSubtypes.size)
        }
    }
}