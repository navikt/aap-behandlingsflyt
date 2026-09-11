package no.nav.aap.behandlingsflyt.repository.sak

import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

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

    /**
     * Simulerer at N steg i en jobb hver henter samme person.
     * Fra APM-trace: en jobb med 202 steg ga 235 PERSON+PERSON_IDENT-queries.
     *
     * Med cache:          alltid 2 queries uavhengig av N (SELECT person + SELECT person_ident)
     * Uten cache (mange-ekstra-kall): 2 × N queries
     *
     * Ved N=202 (reell jobstørrelse fra prod):
     *   Med cache:    2 queries   (sparer 232)
     *   Uten cache:   404 queries
     */
    @ParameterizedTest(name = "{0} steg")
    @ValueSource(ints = [1, 10, 50, 100, 202])
    fun `SQL-queries ved N steg som henter samme person`(nSteg: Int) {
        val identer = listOf(Ident("77700011122"))
        val person = dataSource.transaction { PersonRepositoryImpl(it).finnEllerOpprett(identer) }

        val countingDs = CountingDataSource(dataSource)
        val totalQueries = countingDs.transaction { conn ->
            val repo = PersonRepositoryImpl(conn)
            countingDs.reset()
            repeat(nSteg) { repo.hent(person.id) }
            countingDs.queryCount
        }

        // Med cache: alltid 2 queries uavhengig av N
        // Uten cache (mange-ekstra-kall): 2 * N queries — testen feiler
        assertThat(totalQueries)
            .withFailMessage("$nSteg steg → $totalQueries queries (forventet 2 med cache, uten cache ville gitt ${2 * nSteg})")
            .isEqualTo(2)
    }

    @Test
    fun `antall SQL-queries ved hent to ganger i samme transaksjon - cache gir 0 ekstra queries`() {
        val identer = listOf(Ident("55566677788"))
        val person = dataSource.transaction { PersonRepositoryImpl(it).finnEllerOpprett(identer) }

        val countingDs = CountingDataSource(dataSource)
        countingDs.transaction { conn ->
            val repo = PersonRepositoryImpl(conn)

            countingDs.reset()
            repo.hent(person.id)
            val queriesEtterFørstekall = countingDs.queryCount  // SELECT person + SELECT person_ident = 2

            repo.hent(person.id)
            val queriesEtterAndrekall = countingDs.queryCount   // ingen nye queries — cache-treff

            // Første kall: forventer 2 queries (SELECT person + SELECT person_ident)
            assertThat(queriesEtterFørstekall).isEqualTo(2)
            // Andre kall: ingen nye queries — cache-treff
            assertThat(queriesEtterAndrekall).isEqualTo(2)
        }
    }

    @Test
    fun `hent returnerer cachet person uten nytt DB-kall i samme transaksjon`() {
        val identer = listOf(Ident("44455566677"))
        val person = dataSource.transaction { PersonRepositoryImpl(it).finnEllerOpprett(identer) }

        // To kall på hent() i samme repository-instans skal returnere identisk objekt (cache-treff)
        dataSource.transaction { conn ->
            val repo = PersonRepositoryImpl(conn)
            val first = repo.hent(person.id)
            val second = repo.hent(person.id)
            assertThat(first).isSameAs(second)
        }
    }

    @Test
    fun `cache invalideres etter finnEllerOpprett slik at oppdaterte identer er synlige`() {
        val originalIdent = Ident("99988877766")
        val nyIdent = Ident("11122233344", aktivIdent = false)

        dataSource.transaction { conn ->
            val repo = PersonRepositoryImpl(conn)
            val person = repo.finnEllerOpprett(listOf(originalIdent))

            // Prime cachen
            repo.hent(person.id)

            // Skriv ny ident — skal evicte cachen
            repo.finnEllerOpprett(listOf(originalIdent, nyIdent))

            // Neste hent skal reflektere skrivingen, ikke returnere cachet gammel versjon
            val oppdatert = repo.hent(person.id)
            assertThat(oppdatert.identer()).contains(nyIdent)
        }
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