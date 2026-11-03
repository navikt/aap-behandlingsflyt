package no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap

import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.help.opprettSak
import no.nav.aap.behandlingsflyt.help.sak
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.register.medlemsskap.MedlemskapUnntakRepositoryImpl
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.time.LocalDate

internal class MedlemskapUnntakRepositoryImplTest {

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
    fun `lagre og hente inn unntak`() {
        val (sak, behandling) = dataSource.transaction {
            val sak = opprettSak(
                it,
                LocalDate.now().minusYears(2)
            )
            val behandling = finnEllerOpprettBehandling(it, sak)
            Pair(sak, behandling)
        }

        val behandlingId = dataSource.transaction { connection ->
            val medlemskapRepo = MedlemskapUnntakRepositoryImpl(connection)
            medlemskapRepo.lagreUnntakMedlemskap(
                behandlingId = behandling.id,
                medlemskapUnntakGrunnlag = MedlemskapUnntakGrunnlag(
                    listOf(
                        MedlemskapDataIntern(
                            unntakId = 1234,
                            ident = "13028911111",
                            fraOgMed = "1989-02-13",
                            tilOgMed = "1999-02-14",
                            status = "GYLD",
                            statusaarsak = null,
                            medlem = true,
                            grunnlag = "FLK-TRGD",
                            lovvalg = "FLK_TRGD",
                            helsedel = true,
                            lovvalgsland = "NOR",
                            kilde = KildesystemMedl(KildesystemKode.MEDL, "MEDL")
                        )
                    ).tilDomene()
                )
            )
            behandling.id
        }

        val uthentet = dataSource.transaction { connection ->
            val medlemskapRepo = MedlemskapUnntakRepositoryImpl(connection)
            medlemskapRepo.hentHvisEksisterer(behandlingId = behandlingId)
        }

        assertThat(uthentet).isNotNull()
        assertThat(uthentet!!.unntak).hasSize(1)
        assertThat(uthentet.unntak.first()).isEqualTo(
            Segment(
                periode = Periode(fom = LocalDate.parse("1989-02-13"), tom = LocalDate.parse("1999-02-14")),
                Unntak(
                    status = "GYLD",
                    statusaarsak = null,
                    medlem = true,
                    lovvalg = "FLK_TRGD",
                    helsedel = true,
                    grunnlag = "FLK-TRGD",
                    lovvalgsland = "NOR",
                    kilde = KildesystemMedl(KildesystemKode.MEDL, "MEDL")
                )
            )
        )

        val nyBehandling = dataSource.transaction {
            BehandlingRepositoryImpl(it).oppdaterBehandlingStatus(
                behandlingId,
                no.nav.aap.behandlingsflyt.kontrakt.behandling.Status.AVSLUTTET
            )
            finnEllerOpprettBehandling(it, sak)
        }
        val nyBehandlingId = nyBehandling.id

        val res = dataSource.transaction {
            MedlemskapUnntakRepositoryImpl(it).hentHvisEksisterer(nyBehandlingId)
        }

        assertThat(res).isNotNull
    }

    @Test
    fun `test sletting`() {
        TestDataSource().use { dataSource ->
            dataSource.transaction { connection ->
                val sak = sak(connection)
                val behandling = finnEllerOpprettBehandling(connection, sak)
                val medlemsskapRepository = MedlemskapUnntakRepositoryImpl(connection)

                medlemsskapRepository.lagreUnntakMedlemskap(
                    behandlingId = behandling.id,
                    medlemskapUnntakGrunnlag = MedlemskapUnntakGrunnlag(
                        listOf(
                            MedlemskapDataIntern(
                                unntakId = 123,
                                fraOgMed = "2017-02-13",
                                tilOgMed = "2018-02-13",
                                grunnlag = "grunnlag",
                                helsedel = true,
                                ident = "13028911111",
                                lovvalg = "lovvalg",
                                medlem = true,
                                status = "GYLD",
                                statusaarsak = null,
                                lovvalgsland = "NORGE",
                                kilde = KildesystemMedl(KildesystemKode.MEDL, "MEDL")
                            )
                        ).tilDomene()
                    )
                )

                medlemsskapRepository.lagreUnntakMedlemskap(
                    behandlingId = behandling.id,
                    medlemskapUnntakGrunnlag = MedlemskapUnntakGrunnlag(
                        listOf(
                            Segment(
                                Periode(
                                    fom = LocalDate.of(2019, 2, 13),
                                    tom = LocalDate.of(2020, 2, 13)
                                ),
                                Unntak(
                                    grunnlag = "grunnlag",
                                    helsedel = true,
                                    lovvalg = "lovvalg",
                                    medlem = true,
                                    status = "GYLD",
                                    statusaarsak = null,
                                    lovvalgsland = "NORGE",
                                    kilde = KildesystemMedl(KildesystemKode.MEDL, "MEDL")
                                )
                            )
                        )
                    )
                )

                medlemsskapRepository.lagreUnntakMedlemskap(
                    behandlingId = behandling.id,
                    medlemskapUnntakGrunnlag = MedlemskapUnntakGrunnlag(
                        listOf(
                            MedlemskapDataIntern(
                                unntakId = 125,
                                fraOgMed = "2020-02-13",
                                tilOgMed = "2021-02-13",
                                grunnlag = "grunnlag",
                                helsedel = true,
                                ident = "13028911111",
                                lovvalg = "lovvalg",
                                medlem = true,
                                status = "GYLD",
                                statusaarsak = null,
                                lovvalgsland = "NORGE",
                                kilde = KildesystemMedl(KildesystemKode.MEDL, "MEDL")
                            )
                        ).tilDomene()
                    )
                )

                assertDoesNotThrow { medlemsskapRepository.slett(behandling.id) }
            }
        }
    }
}
