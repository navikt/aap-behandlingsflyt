package no.nav.aap.behandlingsflyt.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.dbconnect.transaction
import no.nav.aap.behandlingsflyt.dbtest.InitTestDatabase
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