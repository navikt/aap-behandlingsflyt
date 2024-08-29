package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import kotlin.reflect.full.primaryConstructor

class AvklaringsbehovsLøserTest {

    @Test
    fun `alle subtyper skal ha unik verdi`() {
        val utledSubtypes = AvklaringsbehovsLøser::class.sealedSubclasses
        InitTestDatabase.dataSource.transaction { dbConnection ->
            val løsningSubtypes = utledSubtypes.map { it.primaryConstructor!!.call(dbConnection).forBehov() }.toSet()

            Assertions.assertThat(løsningSubtypes).hasSize(utledSubtypes.size)
        }
    }
}