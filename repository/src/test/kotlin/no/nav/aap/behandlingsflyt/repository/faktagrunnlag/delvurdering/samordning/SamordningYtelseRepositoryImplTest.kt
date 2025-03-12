package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.samordning

import no.nav.aap.behandlingsflyt.behandling.samordning.Ytelse
import no.nav.aap.behandlingsflyt.faktagrunnlag.GrunnlagKopierer
import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelse
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelsePeriode
import no.nav.aap.behandlingsflyt.repository.avklaringsbehov.FakePdlGateway
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Årsak
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.ÅrsakTilBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.ident
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Prosent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SamordningYtelseRepositoryImplTest {
    private val dataSource = InitTestDatabase.dataSource

    @Test
    fun `sette inn ytelse, hente ut`() {
        val behandling = dataSource.transaction {
            behandling(it, sak(it))
        }
        val samordningYtelser = listOf(
            SamordningYtelse(
                ytelseType = Ytelse.SYKEPENGER,
                ytelsePerioder = listOf(
                    SamordningYtelsePeriode(
                        periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3)),
                        gradering = Prosent.`70_PROSENT`,
                    ),
                    SamordningYtelsePeriode(
                        periode = Periode(LocalDate.now().plusYears(3).plusDays(1), LocalDate.now().plusYears(6)),
                        gradering = Prosent.`30_PROSENT`,
                    )
                ),
                kilde = "kilde",
                saksRef = "abc"
            )
        )
        dataSource.transaction {
            SamordningYtelseRepositoryImpl(it).lagre(
                behandling.id,
                samordningYtelser = samordningYtelser
            )
        }

        val uthentet = dataSource.transaction {
            SamordningYtelseRepositoryImpl(it).hentHvisEksisterer(behandling.id)
        }

        assertThat(samordningYtelser).isEqualTo(uthentet!!.ytelser)

        // Kopier:
        val kopiertBehandling = dataSource.transaction {
            val nyBehandling = behandling(it, sak(it))

            SamordningYtelseRepositoryImpl(it).kopier(behandling.id, nyBehandling.id)
        }
    }

    @Test
    fun `sette inn for flere behandlinger, hente ut`() {
        val behandling1 = dataSource.transaction {
            behandling(it, sak(it))
        }
        val behandling2 = dataSource.transaction {
            behandling(it, sak(it))
        }
        val samordningYtelser1 = listOf(
            SamordningYtelse(
                ytelseType = Ytelse.SYKEPENGER,
                ytelsePerioder = listOf(
                    SamordningYtelsePeriode(
                        periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3)),
                        gradering = Prosent.`70_PROSENT`,
                    ),
                    SamordningYtelsePeriode(
                        periode = Periode(LocalDate.now().plusYears(3).plusDays(1), LocalDate.now().plusYears(6)),
                        gradering = Prosent.`30_PROSENT`,
                    )
                ),
                kilde = "kilde",
                saksRef = "abc"
            )
        )
        val samordningYtelser2 = listOf(
            SamordningYtelse(
                ytelseType = Ytelse.SYKEPENGER,
                ytelsePerioder = listOf(
                    SamordningYtelsePeriode(
                        periode = Periode(LocalDate.now().minusDays(1), LocalDate.now().plusYears(3)),
                        gradering = Prosent.`70_PROSENT`
                    ),
                    SamordningYtelsePeriode(
                        periode = Periode(LocalDate.now().plusYears(3).plusDays(1), LocalDate.now().plusYears(6)),
                        gradering = Prosent.`100_PROSENT`,
                    )
                ),
                kilde = "kilde2",
                saksRef = "xxx"
            )
        )
        dataSource.transaction {
            SamordningYtelseRepositoryImpl(it).lagre(
                behandling1.id,
                samordningYtelser = samordningYtelser1
            )
            SamordningYtelseRepositoryImpl(it).lagre(
                behandling2.id,
                samordningYtelser = samordningYtelser2
            )
        }

        val uthentet1 = dataSource.transaction {
            SamordningYtelseRepositoryImpl(it).hentHvisEksisterer(behandling1.id)
        }
        assertThat(samordningYtelser1).isEqualTo(uthentet1!!.ytelser)
        val uthentet2 = dataSource.transaction {
            SamordningYtelseRepositoryImpl(it).hentHvisEksisterer(behandling2.id)
        }
        assertThat(samordningYtelser2).isEqualTo(uthentet2!!.ytelser)

        // Kopier:
        val kopiertBehandling = dataSource.transaction {
            val nyBehandling = behandling(it, sak(it))

            SamordningYtelseRepositoryImpl(it).kopier(behandling1.id, nyBehandling.id)
        }
    }

    private fun sak(connection: DBConnection): Sak {
        return PersonOgSakService(
            FakePdlGateway,
            PersonRepositoryImpl(connection),
            SakRepositoryImpl(connection)
        ).finnEllerOpprett(
            ident(),
            periode
        )
    }

    private fun behandling(connection: DBConnection, sak: Sak): Behandling {
        return SakOgBehandlingService(
            GrunnlagKopierer(connection), SakRepositoryImpl(connection),
            BehandlingRepositoryImpl(connection)
        ).finnEllerOpprettBehandling(
            sak.saksnummer,
            listOf(Årsak(ÅrsakTilBehandling.MOTTATT_SØKNAD))
        ).behandling
    }

    private val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))
}