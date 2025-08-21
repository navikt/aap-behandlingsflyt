package no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap

import no.nav.aap.behandlingsflyt.help.FakePdlGateway
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.register.medlemsskap.MedlemskapRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.ident
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.time.LocalDate


internal class MedlemskapRepositoryTest {
    companion object {
        private val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))
        private val dataSource = InitTestDatabase.freshDatabase()

        @AfterAll
        @JvmStatic
        fun afterAll() {
            InitTestDatabase.closerFor(dataSource)
        }
    }


    @Test
    fun `lagre og hente inn unntak`() {
        // SETUP
        val (sak, behandling) = dataSource.transaction {
            val sak = PersonOgSakService(
                FakePdlGateway,
                PersonRepositoryImpl(it),
                SakRepositoryImpl(it)
            ).finnEllerOpprett(
                ident(),
                Periode(fom = LocalDate.now().minusYears(2), tom = LocalDate.now())
            )
            val behandling = finnEllerOpprettBehandling(it, sak)
            Pair(sak, behandling)
        }

        // ACT
        val behandlingId = dataSource.transaction { connection ->
            val repo = MedlemskapRepositoryImpl(connection)
            repo.lagreUnntakMedlemskap(
                behandlingId = behandling.id,
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
                )
            )
            behandling.id
        }

        val uthentet = dataSource.transaction { connection ->
            val repo = MedlemskapRepositoryImpl(connection)

            repo.hentHvisEksisterer(behandlingId = behandlingId)
        }

        // ASSERT
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

        // Test kopier-metode
        val nyBehandling = dataSource.transaction {
            BehandlingRepositoryImpl(it).oppdaterBehandlingStatus(
                behandlingId,
                no.nav.aap.behandlingsflyt.kontrakt.behandling.Status.AVSLUTTET
            )
            finnEllerOpprettBehandling(it, sak)
        }
        val nyBehandlingId = nyBehandling.id

        // Hent ut kopi
        val res = dataSource.transaction {
            MedlemskapRepositoryImpl(it).hentHvisEksisterer(nyBehandlingId)
        }

        assertThat(res).isNotNull
    }

    @Test
    fun `test sletting`() {
        InitTestDatabase.freshDatabase().transaction { connection ->
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)
            val medlemsskapRepository = MedlemskapRepositoryImpl(connection)
            medlemsskapRepository.lagreUnntakMedlemskap(
                behandling.id, listOf(
                    MedlemskapDataIntern(
                        unntakId = 123,
                        fraOgMed = "2017-02-13",
                        tilOgMed = "2018-02-13",
                        grunnlag = "grunnlag",
                        helsedel = true,
                        ident = "02429118789",
                        lovvalg = "lovvalg",
                        medlem = true,
                        status = "GYLD",
                        statusaarsak = null,
                        lovvalgsland = "NORGE",
                        kilde = KildesystemMedl(KildesystemKode.MEDL, "MEDL")
                    )
                )
            )
            medlemsskapRepository.lagreUnntakMedlemskap(
                behandling.id, listOf(
                    MedlemskapDataIntern(
                        unntakId = 124,
                        fraOgMed = "2019-02-13",
                        tilOgMed = "2020-02-13",
                        grunnlag = "grunnlag",
                        helsedel = true,
                        ident = "02429118789",
                        lovvalg = "lovvalg",
                        medlem = true,
                        status = "GYLD",
                        statusaarsak = null,
                        lovvalgsland = "NORGE",
                        kilde = KildesystemMedl(KildesystemKode.MEDL, "MEDL")
                    )
                )
            )
            medlemsskapRepository.lagreUnntakMedlemskap(
                behandling.id, listOf(
                    MedlemskapDataIntern(
                        unntakId = 125,
                        fraOgMed = "2020-02-13",
                        tilOgMed = "2021-02-13",
                        grunnlag = "grunnlag",
                        helsedel = true,
                        ident = "02429118789",
                        lovvalg = "lovvalg",
                        medlem = true,
                        status = "GYLD",
                        statusaarsak = null,
                        lovvalgsland = "NORGE",
                        kilde = KildesystemMedl(KildesystemKode.MEDL, "MEDL")
                    )
                )
            )
            assertDoesNotThrow { medlemsskapRepository.slett(behandling.id) }
        }
    }

    private fun sak(connection: DBConnection): Sak {
        return PersonOgSakService(
            FakePdlGateway,
            PersonRepositoryImpl(connection),
            SakRepositoryImpl(connection)
        ).finnEllerOpprett(ident(), periode)
    }
}