package no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.FakePdlGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.adapter.MedlemskapResponse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Årsak
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.ÅrsakTilBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.db.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.db.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.test.ident
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class MedlemskapRepositoryTest {
    @Test
    fun `lagre og hente inn unntak`() {
        val behandlingId = InitTestDatabase.dataSource.transaction { connection ->
            // SETUP
            val sak = PersonOgSakService(
                FakePdlGateway,
                PersonRepositoryImpl(connection),
                SakRepositoryImpl(connection)
            ).finnEllerOpprett(
                ident(),
                Periode(fom = LocalDate.now().minusYears(2), tom = LocalDate.now())
            )
            val behandling = SakOgBehandlingService(connection).finnEllerOpprettBehandling(
                sak.saksnummer,
                listOf(Årsak(ÅrsakTilBehandling.MOTTATT_SØKNAD))
            ).behandling

            // ACT
            val repo = MedlemskapRepository(connection)
            repo.lagreUnntakMedlemskap(
                behandlingId = behandling.id,
                listOf(
                    MedlemskapResponse(
                        unntakId = 1234,
                        ident = "13028911111",
                        fraOgMed = "1989-02-13",
                        tilOgMed = "1999-02-14",
                        status = "GYLD",
                        statusaarsak = null,
                        medlem = true,
                        grunnlag = "FLK-TRGD",
                        lovvalg = "FLK_TRGD",
                        helsedel = true
                    )
                )
            )
            behandling.id
        }

        val uthentet = InitTestDatabase.dataSource.transaction { connection ->
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
                    grunnlag = "FLK-TRGD"
                )
            )
        )
    }
}