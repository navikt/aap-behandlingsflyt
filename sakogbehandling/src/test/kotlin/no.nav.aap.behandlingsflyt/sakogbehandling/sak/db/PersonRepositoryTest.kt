package no.nav.aap.behandlingsflyt.sakogbehandling.sak.db

import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class PersonRepositoryTest {
    @BeforeEach
    fun beforeEach() {
        InitTestDatabase.migrate()
    }

    @AfterEach
    fun afterEach() {
        InitTestDatabase.clean()
    }

    @Test
    fun `skal finne samme person`() {
        val person = InitTestDatabase.dataSource.transaction { connection ->
            val personRepository = PersonRepositoryImpl(connection)
            personRepository.finnEllerOpprett(
                listOf(
                    Ident("12346432345", aktivIdent = false),
                    Ident("23067823253")
                )
            )
        }
        val person2 = InitTestDatabase.dataSource.transaction { connection ->
            val personRepository = PersonRepositoryImpl(connection)
            personRepository.finnEllerOpprett(
                listOf(
                    Ident("23067823253"),
                    Ident("12346432345", aktivIdent = false)
                )
            )
        }


        Assertions.assertThat(person).isNotNull
        Assertions.assertThat(person).isEqualTo(person2)
    }

    @Test
    fun `en oppdatert person returnerer nye identer`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val personRepository = PersonRepositoryImpl(connection)
            personRepository.finnEllerOpprett(
                listOf(
                    Ident("12346432345", aktivIdent = true),
                )
            )
        }
        val person2 = InitTestDatabase.dataSource.transaction { connection ->
            val personRepository = PersonRepositoryImpl(connection)
            personRepository.finnEllerOpprett(
                listOf(
                    Ident("12346432345", aktivIdent = true),
                    Ident("12346432347", aktivIdent = false)
                )
            )
        }

        Assertions.assertThat(person2.identer()).containsExactly(
            Ident("12346432345", aktivIdent = true),
            Ident("12346432347", aktivIdent = false)
        )
    }
}