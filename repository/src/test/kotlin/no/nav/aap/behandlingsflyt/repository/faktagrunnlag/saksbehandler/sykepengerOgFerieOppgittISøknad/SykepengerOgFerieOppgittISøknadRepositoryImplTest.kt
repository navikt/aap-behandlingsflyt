package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.sykepengerOgFerieOppgittISøknad

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykepengerOgFerieOppgittISøknad.SykepengerOgFerieSøknad
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.help.sak
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SykepengerOgFerieOppgittISøknadRepositoryImplTest {
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
    fun `lagre og hente ut igjen, med feriePerioder`() {
        val behandling = dataSource.transaction {
            val sak = sak(it, 1 januar 2023)
            finnEllerOpprettBehandling(it, sak)
        }

        val input = SykepengerOgFerieSøknad(
            mottarSykepenger = true,
            feriePerioder = listOf(Periode(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 14))),
            ferieDager = null
        )

        dataSource.transaction {
            SykepengerOgFerieOppgittISøknadRepositoryImpl(it).lagre(behandling.id, input)
        }

        val resultat = dataSource.transaction {
            SykepengerOgFerieOppgittISøknadRepositoryImpl(it).hentHvisEksisterer(behandling.id)
        }

        assertThat(resultat).isEqualTo(input)

        dataSource.transaction {
            SykepengerOgFerieOppgittISøknadRepositoryImpl(it).slett(behandling.id)
        }

        val resultat2 = dataSource.transaction {
            SykepengerOgFerieOppgittISøknadRepositoryImpl(it).hentHvisEksisterer(behandling.id)
        }
        assertThat(resultat2).isNull()
    }

    @Test
    fun `lagre og hente ut igjen, med ferieDager`() {
        val behandling = dataSource.transaction {
            val sak = sak(it, 1 januar 2023)
            finnEllerOpprettBehandling(it, sak)
        }

        val input = SykepengerOgFerieSøknad(
            mottarSykepenger = false,
            feriePerioder = emptyList(),
            ferieDager = 5
        )

        dataSource.transaction {
            SykepengerOgFerieOppgittISøknadRepositoryImpl(it).lagre(behandling.id, input)
        }

        val resultat = dataSource.transaction {
            SykepengerOgFerieOppgittISøknadRepositoryImpl(it).hentHvisEksisterer(behandling.id)
        }

        assertThat(resultat).isEqualTo(input)
    }

    @Test
    fun `lagre på nytt deaktiverer gammelt grunnlag`() {
        val behandling = dataSource.transaction {
            val sak = sak(it, 1 januar 2023)
            finnEllerOpprettBehandling(it, sak)
        }

        val førsteSvar = SykepengerOgFerieSøknad(mottarSykepenger = true, feriePerioder = emptyList(), ferieDager = 3)
        val andreSvar = SykepengerOgFerieSøknad(mottarSykepenger = false, feriePerioder = emptyList(), ferieDager = null)

        dataSource.transaction {
            SykepengerOgFerieOppgittISøknadRepositoryImpl(it).lagre(behandling.id, førsteSvar)
        }
        dataSource.transaction {
            SykepengerOgFerieOppgittISøknadRepositoryImpl(it).lagre(behandling.id, andreSvar)
        }

        val resultat = dataSource.transaction {
            SykepengerOgFerieOppgittISøknadRepositoryImpl(it).hentHvisEksisterer(behandling.id)
        }

        assertThat(resultat).isEqualTo(andreSvar)
    }

    @Test
    fun `kopier fra en behandling til en annen`() {
        val (fraBehandling, tilBehandling) = dataSource.transaction {
            val sak = sak(it, 1 januar 2023)
            val fra = finnEllerOpprettBehandling(it, sak)
            BehandlingRepositoryImpl(it).oppdaterBehandlingStatus(fra.id, Status.AVSLUTTET)
            val til = finnEllerOpprettBehandling(it, sak)
            fra to til
        }

        val input = SykepengerOgFerieSøknad(
            mottarSykepenger = true,
            feriePerioder = listOf(Periode(LocalDate.of(2024, 6, 1), LocalDate.of(2024, 6, 10))),
            ferieDager = null
        )

        dataSource.transaction {
            SykepengerOgFerieOppgittISøknadRepositoryImpl(it).lagre(fraBehandling.id, input)
        }
        dataSource.transaction {
            SykepengerOgFerieOppgittISøknadRepositoryImpl(it).kopier(fraBehandling.id, tilBehandling.id)
        }

        val resultat = dataSource.transaction {
            SykepengerOgFerieOppgittISøknadRepositoryImpl(it).hentHvisEksisterer(tilBehandling.id)
        }

        assertThat(resultat).isEqualTo(input)
    }
}
