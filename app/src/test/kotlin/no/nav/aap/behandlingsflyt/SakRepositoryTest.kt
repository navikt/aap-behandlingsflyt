package no.nav.aap.behandlingsflyt

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.dbtest.InitTestDatabase
import no.nav.aap.verdityper.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sak.PersonRepository
import no.nav.aap.behandlingsflyt.sak.sakRepository
import no.nav.aap.verdityper.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SakRepositoryTest {

    private val personRepository = PersonRepository(DBConnection(dataSource.connection))
    private val sakRepository = sakRepository(DBConnection(dataSource.connection))

    @Test
    fun `skal avklare yrkesskade hvis det finnes spor av yrkesskade`() {
        val person = personRepository.finnEllerOpprett(Ident("23067823253"))

        val sak = sakRepository.finnEllerOpprett(
            person,
            Periode(
                LocalDate.now().minusMonths(3),
                LocalDate.now().plusYears(1)
            )
        )

        val alleSaker = sakRepository.finnAlle()
        assertThat(alleSaker).isNotEmpty

        assertThat(alleSaker).contains(sak)
    }

    companion object {
        val dataSource = InitTestDatabase.dataSource
    }
}
