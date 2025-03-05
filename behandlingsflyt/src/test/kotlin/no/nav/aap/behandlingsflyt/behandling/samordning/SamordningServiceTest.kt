package no.nav.aap.behandlingsflyt.behandling.samordning

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningVurderingPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelse
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelsePeriode
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.samordning.ytelsesvurdering.SamordningYtelseVurderingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Prosent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class SamordningServiceTest {


    private val dataSource = InitTestDatabase.dataSource

    @Test
    fun `gjør vurderinger når all data er tilstede`() {
        dataSource.transaction { connection ->
            val ytelseVurderingRepo = SamordningYtelseVurderingRepositoryImpl(connection)
            val behandlingId = opprettSakdata(connection)
            opprettYtelseData(ytelseVurderingRepo, behandlingId)
            opprettVurderingData(ytelseVurderingRepo, behandlingId)

            val service = SamordningService(ytelseVurderingRepo)

            val grunnlag = ytelseVurderingRepo.hentHvisEksisterer(behandlingId)!!
            val tidligereVurderinger = service.tidligereVurderinger(grunnlag)
            assertThat(service.vurder(grunnlag, tidligereVurderinger)).isNotEmpty
        }
    }

    @Test
    fun `sammenlign perioder med registerdata, finner ikke-vurderte perioder`() {
        val behandlingId = dataSource.transaction { opprettSakdata(it) }

        // Opprett registerdata med vurdering fra 1 januar til 10 januar
        dataSource.transaction { connection ->
            val ytelseVurderingRepo = SamordningYtelseVurderingRepositoryImpl(connection)
            opprettYtelseData(
                ytelseVurderingRepo, behandlingId, ytelser = listOf(
                    SamordningYtelse(
                        ytelseType = Ytelse.SYKEPENGER,
                        ytelsePerioder = listOf(
                            SamordningYtelsePeriode(
                                periode = Periode(1 januar 2024, 10 januar 2024),
                                gradering = Prosent.`70_PROSENT`
                            )
                        ),
                        kilde = "kilde",
                    )
                )
            )
        }

        // Registrer vurdering fra 5 januar til 10 januar
        dataSource.transaction { connection ->
            val ytelseVurderingRepo = SamordningYtelseVurderingRepositoryImpl(connection)
            opprettVurderingData(
                ytelseVurderingRepo, behandlingId, vurderinger = listOf(
                    SamordningVurdering(
                        ytelseType = Ytelse.SYKEPENGER,
                        vurderingPerioder = listOf(
                            SamordningVurderingPeriode(
                                periode = Periode(5 januar 2024, 10 januar 2024),
                                gradering = Prosent.`50_PROSENT`,
                            )
                        )
                    )
                )
            )
        }

        val input = dataSource.transaction { connection ->
            SamordningYtelseVurderingRepositoryImpl(connection).hentHvisEksisterer(behandlingId)!!
        }
        val ikkeVurdertePerioder = dataSource.transaction { connection ->
            val service = SamordningService(SamordningYtelseVurderingRepositoryImpl(connection))

            val tidligereVurderinger = service.tidligereVurderinger(input)
            service.perioderSomIkkeHarBlittVurdert(input, tidligereVurderinger)
        }

        // Forvent at ikke-vurderte perioder er fra 1 jan til 4 jan
        assertThat(ikkeVurdertePerioder).hasSize(1)
        assertThat(ikkeVurdertePerioder.first().periode).isEqualTo(
            Periode(1 januar 2024, 4 januar 2024)
        )
    }

    @Test
    fun `krever vurdering om det finnes samordningdata`() {
        dataSource.transaction { connection ->
            val ytelseVurderingRepo = SamordningYtelseVurderingRepositoryImpl(connection)
            val behandlingId = opprettSakdata(connection)
            opprettYtelseData(ytelseVurderingRepo, behandlingId)

            val service = SamordningService(
                ytelseVurderingRepo
            )
            val input = ytelseVurderingRepo.hentHvisEksisterer(behandlingId)!!
            val tidligereVurderinger = service.tidligereVurderinger(input)

            assertThrows<IllegalArgumentException> { service.vurder(input, tidligereVurderinger) }
        }
    }

    private fun opprettVurderingData(
        repo: SamordningYtelseVurderingRepositoryImpl,
        behandlingId: BehandlingId,
        vurderinger: List<SamordningVurdering> = listOf(
            SamordningVurdering(
                Ytelse.SYKEPENGER,
                listOf(
                    SamordningVurderingPeriode(
                        Periode(LocalDate.now(), LocalDate.now().plusDays(5)),
                        Prosent(50),
                        0
                    )
                )
            )
        )
    ) {
        repo.lagreVurderinger(behandlingId, vurderinger)
    }

    private fun opprettYtelseData(
        repo: SamordningYtelseVurderingRepositoryImpl,
        behandlingId: BehandlingId,
        ytelser: List<SamordningYtelse> = listOf(
            SamordningYtelse(
                Ytelse.SYKEPENGER,
                listOf(
                    SamordningYtelsePeriode(
                        Periode(LocalDate.now(), LocalDate.now().plusDays(5)),
                        Prosent(50),
                        0
                    )
                ),
                "kilde",
                "ref"
            )
        )
    ) {
        repo.lagreYtelser(behandlingId, ytelser)
    }

    private fun opprettSakdata(connection: DBConnection): BehandlingId {
        val person = PersonRepositoryImpl(connection).finnEllerOpprett(listOf(Ident("ident", true)))
        val sakId = SakRepositoryImpl(connection).finnEllerOpprett(
            person,
            Periode(LocalDate.now(), LocalDate.now().plusDays(5))
        ).id
        return BehandlingRepositoryImpl(connection).opprettBehandling(
            sakId,
            listOf(),
            TypeBehandling.Førstegangsbehandling,
            null
        ).id
    }
}