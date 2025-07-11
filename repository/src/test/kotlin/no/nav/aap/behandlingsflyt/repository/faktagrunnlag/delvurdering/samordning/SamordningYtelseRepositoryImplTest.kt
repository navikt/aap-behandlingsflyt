package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.samordning

import no.nav.aap.behandlingsflyt.behandling.samordning.Ytelse
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelse
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelsePeriode
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.repository.avklaringsbehov.FakePdlGateway
import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.FreshDatabaseExtension
import no.nav.aap.behandlingsflyt.test.ident
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Prosent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import javax.sql.DataSource

@ExtendWith(FreshDatabaseExtension::class)
class SamordningYtelseRepositoryImplTest(val dataSource: DataSource) {

    @Test
    fun `sette inn flere ytelser, skal hente ut nyeste`() {
        val behandling = dataSource.transaction { finnEllerOpprettBehandling(it, sak(it)) }

        val samordningYtelser = listOf(
            SamordningYtelse(
                ytelseType = Ytelse.SYKEPENGER,
                ytelsePerioder = listOf(
                    SamordningYtelsePeriode(
                        periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3)),
                        gradering = Prosent.`70_PROSENT`,
                    ),
                    SamordningYtelsePeriode(
                        periode = Periode(LocalDate.now().plusYears(7).plusDays(1), LocalDate.now().plusYears(10)),
                        gradering = Prosent.`30_PROSENT`,
                    )
                ),
                kilde = "kilde",
                saksRef = "abc"
            ),
            SamordningYtelse(
                ytelseType = Ytelse.OMSORGSPENGER,
                ytelsePerioder = listOf(
                    SamordningYtelsePeriode(
                        periode = Periode(LocalDate.now(), LocalDate.now().plusYears(6)),
                        gradering = Prosent.`50_PROSENT`,
                    ),
                    SamordningYtelsePeriode(
                        periode = Periode(LocalDate.now().plusYears(7).plusDays(1), LocalDate.now().plusYears(10)),
                        gradering = Prosent.`66_PROSENT`
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

        assertThat(uthentet!!.ytelser.size).isEqualTo(2)
        assertThat(samordningYtelser).containsExactlyInAnyOrderElementsOf(uthentet.ytelser)

        // Setter inn på nytt
        val samordningYtelser2 = listOf(
            SamordningYtelse(
                ytelseType = Ytelse.SYKEPENGER,
                ytelsePerioder = listOf(
                    SamordningYtelsePeriode(
                        periode = Periode(LocalDate.now().plusDays(1), LocalDate.now().plusYears(3)),
                        gradering = Prosent(66),
                    ),
                    SamordningYtelsePeriode(
                        periode = Periode(LocalDate.now().plusYears(3).plusDays(2), LocalDate.now().plusYears(6)),
                        gradering = Prosent(31)
                    )
                ),
                kilde = "kilde",
                saksRef = "abc"
            ),
            SamordningYtelse(
                ytelseType = Ytelse.OMSORGSPENGER,
                ytelsePerioder = listOf(
                    SamordningYtelsePeriode(
                        periode = Periode(LocalDate.now().plusDays(1), LocalDate.now().plusYears(2)),
                        gradering = Prosent(51),
                    ),
                    SamordningYtelsePeriode(
                        periode = Periode(LocalDate.now().plusYears(3).plusDays(2), LocalDate.now().plusYears(6)),
                        gradering = Prosent(67)
                    )
                ),
                kilde = "kilde",
                saksRef = "abc"
            )
        )

        dataSource.transaction {
            SamordningYtelseRepositoryImpl(it).lagre(
                behandling.id,
                samordningYtelser = samordningYtelser2
            )
        }

        val uthentet2 = dataSource.transaction {
            SamordningYtelseRepositoryImpl(it).hentHvisEksisterer(behandling.id)
        }

        assertThat(uthentet2!!.ytelser.size).isEqualTo(2)
        assertThat(samordningYtelser2).containsExactlyInAnyOrderElementsOf(uthentet2.ytelser)

        // Kopier:
        val kopiertBehandling = dataSource.transaction {
            val nyBehandling = finnEllerOpprettBehandling(it, sak(it))

            SamordningYtelseRepositoryImpl(it).kopier(behandling.id, nyBehandling.id)
        }
    }

    @Test
    fun `sette inn flere ytelser, skal hente ut eldste`() {
        val behandling = dataSource.transaction { finnEllerOpprettBehandling(it, sak(it)) }

        // Første sett med ytelser (eldste)
        val samordningYtelserEldste = listOf(
            SamordningYtelse(
                ytelseType = Ytelse.SYKEPENGER,
                ytelsePerioder = listOf(
                    SamordningYtelsePeriode(
                        periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3)),
                        gradering = Prosent.`70_PROSENT`,
                    )
                ),
                kilde = "kilde-eldste",
                saksRef = "abc-eldste"
            )
        )
        dataSource.transaction {
            SamordningYtelseRepositoryImpl(it).lagre(
                behandling.id,
                samordningYtelser = samordningYtelserEldste
            )
        }

        // Andre sett med ytelser (nyeste)
        val samordningYtelserNyeste = listOf(
            SamordningYtelse(
                ytelseType = Ytelse.OMSORGSPENGER,
                ytelsePerioder = listOf(
                    SamordningYtelsePeriode(
                        periode = Periode(LocalDate.now().plusDays(1), LocalDate.now().plusYears(2)),
                        gradering = Prosent(51),
                    )
                ),
                kilde = "kilde-nyeste",
                saksRef = "abc-nyeste"
            )
        )
        dataSource.transaction {
            SamordningYtelseRepositoryImpl(it).lagre(
                behandling.id,
                samordningYtelser = samordningYtelserNyeste
            )
        }

        // Verifiser at hentEldsteGrunnlag returnerer det eldste grunnlaget
        val eldsteGrunnlag = dataSource.transaction {
            SamordningYtelseRepositoryImpl(it).hentEldsteGrunnlag(behandling.id)
        }
        assertThat(eldsteGrunnlag).isNotNull
        assertThat(eldsteGrunnlag!!.ytelser.size).isEqualTo(1)
        assertThat(eldsteGrunnlag.ytelser[0].kilde).isEqualTo("kilde-eldste")
        assertThat(eldsteGrunnlag.ytelser[0].saksRef).isEqualTo("abc-eldste")
        assertThat(eldsteGrunnlag.ytelser[0].ytelseType).isEqualTo(Ytelse.SYKEPENGER)

        // Verifiser at hentHvisEksisterer returnerer det nyeste grunnlaget
        val nyesteGrunnlag = dataSource.transaction {
            SamordningYtelseRepositoryImpl(it).hentHvisEksisterer(behandling.id)
        }
        assertThat(nyesteGrunnlag).isNotNull
        assertThat(nyesteGrunnlag!!.ytelser.size).isEqualTo(1)
        assertThat(nyesteGrunnlag.ytelser[0].kilde).isEqualTo("kilde-nyeste")
        assertThat(nyesteGrunnlag.ytelser[0].saksRef).isEqualTo("abc-nyeste")
        assertThat(nyesteGrunnlag.ytelser[0].ytelseType).isEqualTo(Ytelse.OMSORGSPENGER)
    }

    @Test
    fun `sette inn for flere behandlinger, hente ut`() {
        val behandling1 = dataSource.transaction {
            finnEllerOpprettBehandling(it, sak(it))
        }
        val behandling2 = dataSource.transaction {
            finnEllerOpprettBehandling(it, sak(it))
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
                        gradering = Prosent.`70_PROSENT`,
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
            val nyBehandling = finnEllerOpprettBehandling(it, sak(it))

            SamordningYtelseRepositoryImpl(it).kopier(behandling1.id, nyBehandling.id)
        }
    }

    @Test
    fun `test sletting`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)
            val samordningYtelseRepository = SamordningYtelseRepositoryImpl(connection)
            samordningYtelseRepository.lagre(
                behandling.id, listOf(
                    SamordningYtelse(
                        ytelseType = Ytelse.SYKEPENGER,
                        ytelsePerioder = listOf(
                            SamordningYtelsePeriode(
                                periode = Periode(
                                    fom = LocalDate.of(2023, 1, 1),
                                    tom = LocalDate.of(2023, 12, 31)
                                ),
                                gradering = null,
                                kronesum = 1000
                            )
                        ),
                        kilde = "TEST1",
                        saksRef = "REF1"
                    )
                )
            )
            samordningYtelseRepository.lagre(
                behandling.id, listOf(
                    SamordningYtelse(
                        ytelseType = Ytelse.SYKEPENGER,
                        ytelsePerioder = listOf(
                            SamordningYtelsePeriode(
                                periode = Periode(
                                    fom = LocalDate.of(2024, 1, 1),
                                    tom = LocalDate.of(2024, 12, 31)
                                ),
                                gradering = null,
                                kronesum = 1000
                            )
                        ),
                        kilde = "TEST1",
                        saksRef = "REF1"
                    )
                )
            )
            assertDoesNotThrow {
                samordningYtelseRepository.slett(behandling.id)
            }
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

    private val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))
}
