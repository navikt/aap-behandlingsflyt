package no.nav.aap.behandlingsflyt.behandling.samordning

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningVurderingPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelse
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelsePeriode
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.samordning.SamordningYtelseRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.samordning.ytelsesvurdering.SamordningYtelseVurderingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
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
        val behandlingId = dataSource.transaction { opprettSakdata(it) }
        dataSource.transaction { connection ->
            val ytelseVurderingRepo = SamordningYtelseVurderingRepositoryImpl(connection)
            val samordningYtelseRepository = SamordningYtelseRepositoryImpl(connection)
            opprettYtelseData(samordningYtelseRepository, behandlingId)
            opprettVurderingData(ytelseVurderingRepo, behandlingId)
        }

        dataSource.transaction { connection ->
            val ytelseVurderingRepo = SamordningYtelseVurderingRepositoryImpl(connection)
            val samordningYtelseRepository = SamordningYtelseRepositoryImpl(connection)
            val service = SamordningService(ytelseVurderingRepo, samordningYtelseRepository)

            val hentedeVurderinger = service.hentVurderinger(behandlingId)
            val hentedeYtelser = service.hentYtelser(behandlingId)
            val tidligereVurderinger = service.tidligereVurderinger(hentedeVurderinger)
            assertThat(service.vurder(hentedeYtelser, tidligereVurderinger)).isNotEmpty
        }
    }

    @Test
    fun `sammenlign perioder med registerdata, finner ikke-vurderte perioder`() {
        val behandlingId = dataSource.transaction { opprettSakdata(it) }

        // Opprett registerdata med vurdering fra 1 januar til 10 januar
        dataSource.transaction { connection ->
            opprettYtelseData(
                SamordningYtelseRepositoryImpl(connection), behandlingId, ytelser = listOf(
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
                        begrunnelse = "En god begrunnelse",
                        maksDatoEndelig = false,
                        maksDato = LocalDate.now().plusYears(1),
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

        val (ytelser, vurderinger) = dataSource.transaction { connection ->
            val service = SamordningService(
                SamordningYtelseVurderingRepositoryImpl(connection),
                SamordningYtelseRepositoryImpl(connection)
            )
            Pair(service.hentYtelser(behandlingId), service.hentVurderinger(behandlingId))
        }
        val ikkeVurdertePerioder = dataSource.transaction { connection ->
            val service = SamordningService(
                SamordningYtelseVurderingRepositoryImpl(connection),
                SamordningYtelseRepositoryImpl(connection)
            )

            val tidligereVurderinger = service.tidligereVurderinger(vurderinger)
            service.perioderSomIkkeHarBlittVurdert(ytelser, tidligereVurderinger)
        }

        // Forvent at ikke-vurderte perioder er fra 1 jan til 4 jan
        assertThat(ikkeVurdertePerioder).hasSize(1)
        assertThat(ikkeVurdertePerioder.first().periode).isEqualTo(
            Periode(1 januar 2024, 4 januar 2024)
        )
    }

    @Test
    fun `krever vurdering om det finnes samordningdata`() {
        val behandlingId = dataSource.transaction { opprettSakdata(it) }
        dataSource.transaction { connection ->
            opprettYtelseData(SamordningYtelseRepositoryImpl(connection), behandlingId)

            val service = SamordningService(
                SamordningYtelseVurderingRepositoryImpl(connection),
                SamordningYtelseRepositoryImpl(connection)
            )
            val vurderinger = service.hentVurderinger(behandlingId)
            val ytelser = service.hentYtelser(behandlingId)

            val tidligereVurderinger = service.tidligereVurderinger(vurderinger)


            assertThrows<IllegalArgumentException> { service.vurder(ytelser, tidligereVurderinger) }
        }
    }

    private fun opprettVurderingData(
        repo: SamordningYtelseVurderingRepositoryImpl,
        behandlingId: BehandlingId,
        vurderinger: List<SamordningVurdering> = listOf(
            SamordningVurdering(
                Ytelse.SYKEPENGER,
                begrunnelse = "En god begrunnelse",
                maksDatoEndelig = false,
                maksDato = LocalDate.now().plusYears(1),
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
        repo: SamordningYtelseRepositoryImpl,
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
        repo.lagre(behandlingId, ytelser)
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