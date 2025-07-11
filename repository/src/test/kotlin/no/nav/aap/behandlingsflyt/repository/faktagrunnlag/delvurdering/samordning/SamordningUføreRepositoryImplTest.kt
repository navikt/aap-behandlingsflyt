package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.samordning

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.uførevurdering.SamordningUføreVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.uførevurdering.SamordningUføreVurderingPeriode
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
internal class SamordningUføreRepositoryImplTest(val dataSource: DataSource) {

    private val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(1))

    @Test
    fun `skal lagre ned en helt ny vurdering og hente den opp igjen`() {
        val behandling = dataSource.transaction {
            finnEllerOpprettBehandling(it, sak(it))
        }

        // Lagre ytelse
        val vurdering = SamordningUføreVurdering(
            begrunnelse = "En fin begrunnelse", vurderingPerioder = listOf(
                SamordningUføreVurderingPeriode(
                    virkningstidspunkt = periode.fom,
                    uføregradTilSamordning = Prosent.`50_PROSENT`
                ),
                SamordningUføreVurderingPeriode(
                    virkningstidspunkt = periode.fom.plusMonths(4),
                    uføregradTilSamordning = Prosent.`70_PROSENT`
                )
            ),
            "ident"
        )
        dataSource.transaction {
            SamordningUføreRepositoryImpl(it).lagre(behandling.id, vurdering)
        }

        val uthentet = dataSource.transaction {
            SamordningUføreRepositoryImpl(it).hentHvisEksisterer(behandling.id)
        }

        assertThat(uthentet).isNotNull
        assertThat(uthentet?.vurdering).usingRecursiveComparison().ignoringFields("vurdertTidspunkt")
            .isEqualTo(vurdering)
    }

    @Test
    fun `test sletting`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)
            val samordningUføreRepository = SamordningUføreRepositoryImpl(connection)
            samordningUføreRepository.lagre(
                behandling.id,
                SamordningUføreVurdering(
                    begrunnelse = "En fin begrunnelse", vurderingPerioder = listOf(
                        SamordningUføreVurderingPeriode(
                            virkningstidspunkt = periode.fom,
                            uføregradTilSamordning = Prosent.`50_PROSENT`
                        ),
                        SamordningUføreVurderingPeriode(
                            virkningstidspunkt = periode.fom.plusMonths(4),
                            uføregradTilSamordning = Prosent.`70_PROSENT`
                        )
                    ),
                    vurdertAv = "ident"
                )
            )
            samordningUføreRepository.lagre(
                behandling.id,
                SamordningUføreVurdering(
                    begrunnelse = "En fin begrunnelse", vurderingPerioder = listOf(
                        SamordningUføreVurderingPeriode(
                            virkningstidspunkt = periode.fom,
                            uføregradTilSamordning = Prosent.`50_PROSENT`
                        ),
                        SamordningUføreVurderingPeriode(
                            virkningstidspunkt = periode.fom.plusMonths(2),
                            uføregradTilSamordning = Prosent.`70_PROSENT`
                        )
                    ),
                    vurdertAv = "ident"
                )
            )
            assertDoesNotThrow {
                samordningUføreRepository.slett(behandling.id)
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
}
