package no.nav.aap.behandlingsflyt.repository.sak

import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class PersonRepositoryImplTest {
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
    fun `lagre, hente ut igjen`() {
        val identer = listOf(Ident("12345678910"), Ident("12345", aktivIdent = false))
        val person = dataSource.transaction {
            val personRepository = PersonRepositoryImpl(it)
            personRepository.finnEllerOpprett(identer)
        }

        assertThat(person.identer()).containsExactlyInAnyOrderElementsOf(identer)

        // Hent med ID
        val res = dataSource.transaction {
            PersonRepositoryImpl(it).hent(person.id)
        }
        assertThat(res).isEqualTo(person)

        // Oppdater
        val nyIdentÅLeggeTil = Ident(
            "271828", aktivIdent = false
        )
        dataSource.transaction {
            PersonRepositoryImpl(it).finnEllerOpprett(listOf(nyIdentÅLeggeTil, person.aktivIdent()))
        }

        // Hent på nytt
        val oppdatert = dataSource.transaction {
            PersonRepositoryImpl(it).hent(person.id)
        }

        assertThat(oppdatert.identer()).containsExactlyInAnyOrderElementsOf(
            listOf(nyIdentÅLeggeTil) + identer
        )

        // Identer fjernes
        dataSource.transaction {
            PersonRepositoryImpl(it).finnEllerOpprett(listOf(person.aktivIdent()))
        }
    }
}