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
    fun `finn eksisterende person`() {
        val identer = listOf(Ident("12345678910"), Ident("12345", aktivIdent = false))
        dataSource.transaction {
            val personRepository = PersonRepositoryImpl(it)
            personRepository.finnEllerOpprett(identer)
        }

        val funnet = dataSource.transaction {
            val personRepository = PersonRepositoryImpl(it)
            personRepository.finn(identer)
        }
        assertThat(funnet).isNotNull
        assertThat(funnet!!.identer()).isEqualTo(identer)
    }

    @Test
    fun `finner ingen ukjent person`() {
        val identer = listOf(Ident("12345678910"), Ident("12345", aktivIdent = false))
        dataSource.transaction {
            val personRepository = PersonRepositoryImpl(it)
            personRepository.finnEllerOpprett(identer)
        }

        val finnesIkke = listOf(Ident("finnes_ikke"), Ident("mangler", aktivIdent = false))
        val ingenPersoner = dataSource.transaction {
            val personRepository = PersonRepositoryImpl(it)
            personRepository.finn(finnesIkke)
        }
        assertThat(ingenPersoner).isNull()
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