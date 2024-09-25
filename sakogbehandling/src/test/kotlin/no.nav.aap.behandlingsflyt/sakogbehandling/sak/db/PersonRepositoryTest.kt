package no.nav.aap.behandlingsflyt.sakogbehandling.sak.db

import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.verdityper.sakogbehandling.Ident
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

internal class PersonRepositoryTest {

    @Test
    fun `skal finne samme person`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val personRepository = PersonRepository(connection)
            val person = personRepository.finnEllerOpprett(
                listOf(
                    Ident("23067823253"),
                    Ident("12346432345", aktivIdent = false)
                )
            )

            val person2 = personRepository.finnEllerOpprett(listOf(Ident("12346432345", aktivIdent = false), Ident("23067823253")))

            Assertions.assertThat(person).isNotNull
            Assertions.assertThat(person).isEqualTo(person2)
        }
    }
}