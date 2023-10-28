package no.nav.aap.behandlingsflyt

import no.nav.aap.behandlingsflyt.dbstuff.DBConnection
import no.nav.aap.behandlingsflyt.dbstuff.InitTestDatabase
import no.nav.aap.behandlingsflyt.sak.SakRepository
import no.nav.aap.behandlingsflyt.sak.Ident
import no.nav.aap.behandlingsflyt.sak.PersonRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SakRepositoryTest {

    private val personRepository = PersonRepository(DBConnection(dataSource.connection))
    private val sakRepository = SakRepository(DBConnection(dataSource.connection))

    @Test
    fun `skal avklare yrkesskade hvis det finnes spor av yrkesskade`() {
        val person = personRepository.finnEllerOpprett(Ident("23067823253"))

        sakRepository.finnEllerOpprett(
            person,
            Periode(
                LocalDate.now().minusMonths(3),
                LocalDate.now().plusYears(1)))

        val alleSaker = sakRepository.finnAlle()
        assertThat(alleSaker).isNotEmpty
        assertThat(alleSaker).hasSize(1)
    }

    companion object {
        val dataSource = InitTestDatabase.dataSource
    }
}
