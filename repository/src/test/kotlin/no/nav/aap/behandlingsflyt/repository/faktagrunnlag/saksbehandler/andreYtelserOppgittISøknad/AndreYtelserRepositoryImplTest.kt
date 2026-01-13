package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.andreYtelserOppgittISøknad

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.AndreUtbetalingerYtelser
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.AndreYtelserSøknad
import no.nav.aap.behandlingsflyt.help.FakePdlGateway
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling

import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.desember
import no.nav.aap.behandlingsflyt.test.ident
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class AndreYtelserRepositoryImplTest {
    private lateinit var dataSource: TestDataSource

    @BeforeEach
    fun setUp() {
        dataSource = TestDataSource()
    }

    @AfterEach
    fun tearDown() {
        dataSource.close()
    }

    @Test
    fun `lagre og slett`() {
        val sak = dataSource.transaction { sak(it) }

        val behandling1 = dataSource.transaction {
            finnEllerOpprettBehandling(it, sak)
        }
        val stønad = listOf<AndreUtbetalingerYtelser>(
            AndreUtbetalingerYtelser.ØKONOMISK_SOSIALHJELP,
            AndreUtbetalingerYtelser.OMSORGSSTØNAD,
            AndreUtbetalingerYtelser.AFP
        )
        val andreUtbetalinger = AndreYtelserSøknad(
            ekstraLønn = true,
            stønad = stønad,
            afpKilder = "Arbeidsgiver"
        )

        dataSource.transaction {
            AndreYtelserOppgittISøknadRepositoryImpl(it).lagre(
                behandling1.id, andreUtbetalinger
            )
        }

        val ytelser = dataSource.transaction {
            AndreYtelserOppgittISøknadRepositoryImpl(it).hent(
                behandling1.id
            )
        }


        assertThat(ytelser).isEqualTo(andreUtbetalinger)

        dataSource.transaction {
            AndreYtelserOppgittISøknadRepositoryImpl(it).slett(
                behandling1.id
            )
        }
    }



    @Test
    fun `lagre og kopier, så slette gamle`() {

        val sak = dataSource.transaction { sak(it) }

        val behandling1 = dataSource.transaction {
            finnEllerOpprettBehandling(it, sak)
        }
        val stønad = listOf<AndreUtbetalingerYtelser>(
            AndreUtbetalingerYtelser.ØKONOMISK_SOSIALHJELP,
            AndreUtbetalingerYtelser.OMSORGSSTØNAD,
            AndreUtbetalingerYtelser.AFP
        )
        val andreUtbetalinger = AndreYtelserSøknad(
            ekstraLønn = true,
            stønad = stønad,
            afpKilder = "Arbeidsgiver"
        )
        val sak2 = dataSource.transaction { sak(it) }
        val behandling2 = dataSource.transaction {
            finnEllerOpprettBehandling(it, sak2)
        }

        dataSource.transaction {
            AndreYtelserOppgittISøknadRepositoryImpl(it).lagre(
                behandling1.id, andreUtbetalinger
            )
        }
        dataSource.transaction {
            AndreYtelserOppgittISøknadRepositoryImpl(it).kopier(
                behandling1.id, behandling2.id
            )
        }

        val ytelser = dataSource.transaction {
            AndreYtelserOppgittISøknadRepositoryImpl(it).hent(
                behandling2.id
            )
        }

        assertThat(ytelser).isEqualTo(andreUtbetalinger)

        dataSource.transaction {
            AndreYtelserOppgittISøknadRepositoryImpl(it).slett(
                behandling1.id
            )
        }

        assertThrows<IllegalArgumentException> {
            dataSource.transaction {
                AndreYtelserOppgittISøknadRepositoryImpl(it).hent(
                    behandling1.id
                )
            }
        }

        val ytelser2 = dataSource.transaction {
            AndreYtelserOppgittISøknadRepositoryImpl(it).hent(
                behandling2.id
            )
        }

        assertThat(ytelser2).isEqualTo(andreUtbetalinger)

    }


    @Test
    fun `lagre og henter andre ytelser`() {

        val sak = dataSource.transaction { sak(it) }

        val behandling1 = dataSource.transaction {
            finnEllerOpprettBehandling(it, sak)
        }
        val stønad1 = listOf<AndreUtbetalingerYtelser>(
            AndreUtbetalingerYtelser.ØKONOMISK_SOSIALHJELP,
            AndreUtbetalingerYtelser.OMSORGSSTØNAD
        )
        val andreUtbetalinger1 = AndreYtelserSøknad(
            ekstraLønn = true,
            stønad = stønad1
        )
        val sak2 = dataSource.transaction { sak(it) }
        val behandling2 = dataSource.transaction {
            finnEllerOpprettBehandling(it, sak2)
        }
        val stønad2 = listOf<AndreUtbetalingerYtelser>(
            AndreUtbetalingerYtelser.OMSORGSSTØNAD,
            AndreUtbetalingerYtelser.INTRODUKSJONSSTØNAD,
        )

        val andreUtbetalinger2 = AndreYtelserSøknad(
            ekstraLønn = true,
            stønad = stønad2
        )
        dataSource.transaction {
            AndreYtelserOppgittISøknadRepositoryImpl(it).lagre(
                behandling1.id, andreUtbetalinger1
            )
        }
        dataSource.transaction {
            AndreYtelserOppgittISøknadRepositoryImpl(it).lagre(
                behandling2.id, andreUtbetalinger2
            )
        }
        val ytelser1 = dataSource.transaction {
            AndreYtelserOppgittISøknadRepositoryImpl(it).hent(
                behandling1.id
            )
        }
        val ytelser2 = dataSource.transaction {
            AndreYtelserOppgittISøknadRepositoryImpl(it).hent(
                behandling2.id
            )
        }

        assertThat(ytelser1).isEqualTo(andreUtbetalinger1)
        assertThat(ytelser2).isEqualTo(andreUtbetalinger2)

    }


    private fun sak(connection: DBConnection): Sak {
        return PersonOgSakService(
            FakePdlGateway,
            PersonRepositoryImpl(connection),
            SakRepositoryImpl(connection)
        ).finnEllerOpprett(ident(), Periode(1 januar 2022, 31.desember(2023)))
    }
}