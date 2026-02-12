package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.`andreYtelserOppgittISøknad`

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.AndreUtbetalingerYtelser
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.AndreYtelserSøknad
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.help.sak
import no.nav.aap.behandlingsflyt.test.desember
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class AndreYtelserOppgittISøknadRepositoryImplTest {
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
    fun `lagre og hente ut igjen`() {
        val behandling = dataSource.transaction {
            val sak = sak(it, Periode(1 januar 2023, 31 desember 2023))
            finnEllerOpprettBehandling(it, sak)
        }

        val input = AndreYtelserSøknad(
            ekstraLønn = true,
            afpKilder = "aASDA",
            stønad = listOf(
                AndreUtbetalingerYtelser.YTELSE_FRA_UTENLANDSKE_TRYGDEMYNDIGHETER,
                AndreUtbetalingerYtelser.LÅN_FRA_LÅNEKASSEN
            )
        )

        dataSource.transaction {
            AndreYtelserOppgittISøknadRepositoryImpl(it).lagre(behandling.id, input)
        }

        val resultat = dataSource.transaction {
            AndreYtelserOppgittISøknadRepositoryImpl(it).hent(behandling.id)
        }

        assertThat(resultat).isEqualTo(input)

        dataSource.transaction {
            AndreYtelserOppgittISøknadRepositoryImpl(it).slett(behandling.id)
        }

        val resultat2 = dataSource.transaction {
            AndreYtelserOppgittISøknadRepositoryImpl(it).hentHvisEksisterer(behandling.id)
        }
        assertThat(resultat2).isNull()
    }
}