package no.nav.aap.behandlingsflyt.repository.behandling.vedtak.samid

import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.help.opprettSak
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class SamIdRepositoryTest {
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
    fun `Kan skrive og lese`() {
        dataSource.transaction { connection ->
            val samRepo = SamIdRepositoryImpl(connection)
            val samId = "123456789"

            val sak = opprettSak(connection, Periode(1 januar 2020, 1 januar 2021))
            val behandling = finnEllerOpprettBehandling(connection, sak)

            samRepo.lagre(behandling.id, samId)
            val hentetSamId = samRepo.hentHvisEksisterer(behandling.id)

            assertThat(hentetSamId).isEqualTo(samId)
        }
    }
}