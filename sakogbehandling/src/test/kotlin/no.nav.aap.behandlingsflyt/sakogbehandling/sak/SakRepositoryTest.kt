package no.nav.aap.behandlingsflyt.sakogbehandling.sak

import no.nav.aap.behandlingsflyt.dbconnect.transaction
import no.nav.aap.behandlingsflyt.dbtest.InitTestDatabase
import no.nav.aap.verdityper.Periode
import no.nav.aap.verdityper.sakogbehandling.Ident
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class SakRepositoryTest {

    @Test
    fun `skal avklare yrkesskade hvis det finnes spor av yrkesskade`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val personRepository = PersonRepository(connection)
            val sakRepository = sakRepository(connection)

            val person = personRepository.finnEllerOpprett(listOf(Ident("23067823253")))

            val sak = sakRepository.finnEllerOpprett(
                person,
                Periode(
                    LocalDate.now().minusMonths(3),
                    LocalDate.now().plusYears(1)
                )
            )

            val alleSaker = sakRepository.finnAlle()
            Assertions.assertThat(alleSaker).isNotEmpty

            Assertions.assertThat(alleSaker).contains(sak)
        }
    }
}