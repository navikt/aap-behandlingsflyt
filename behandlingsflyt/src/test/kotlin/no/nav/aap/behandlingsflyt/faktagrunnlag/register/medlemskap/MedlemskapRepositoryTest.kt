package no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap

import no.nav.aap.behandlingsflyt.faktagrunnlag.FakePdlGateway
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.test.ident
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class MedlemskapRepositoryTest {
    private val dataSource = InitTestDatabase.freshDatabase()

    @Test
    fun `lagre og hente inn unntak`() {
        val behandlingId = dataSource.transaction { connection ->
            // SETUP
            val sak = PersonOgSakService(
                FakePdlGateway,
                PersonRepositoryImpl(connection),
                SakRepositoryImpl(connection)
            ).finnEllerOpprett(
                ident(),
                Periode(fom = LocalDate.now().minusYears(2), tom = LocalDate.now())
            )
            val behandling = finnEllerOpprettBehandling(connection, sak)

            // ACT
            val repo = MedlemskapRepository(connection)
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
            val repo = MedlemskapRepository(connection)

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
    }
}