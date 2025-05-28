package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.samordning

import no.nav.aap.behandlingsflyt.behandling.søknad.TrukketSøknadService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.uførevurdering.SamordningUføreVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.uførevurdering.SamordningUføreVurderingPeriode
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.repository.avklaringsbehov.AvklaringsbehovRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.avklaringsbehov.FakePdlGateway
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.søknad.TrukketSøknadRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
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

internal class SamordningUføreRepositoryImplTest {
    private val dataSource = InitTestDatabase.freshDatabase()

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
            )
        )
        dataSource.transaction {
            SamordningUføreRepositoryImpl(it).lagre(behandling.id, vurdering)
        }

        val uthentet = dataSource.transaction {
            SamordningUføreRepositoryImpl(it).hentHvisEksisterer(behandling.id)
        }

        assertThat(uthentet).isNotNull
        assertThat(uthentet?.vurdering).isEqualTo(vurdering)
    }

    private fun sak(connection: DBConnection): Sak {
        return PersonOgSakService(
            FakePdlGateway,
            PersonRepositoryImpl(connection),
            SakRepositoryImpl(connection),
            BehandlingRepositoryImpl(connection),
            TrukketSøknadService(
                AvklaringsbehovRepositoryImpl(connection),
                TrukketSøknadRepositoryImpl(connection)
            ),
        ).finnEllerOpprett(
            ident(),
            periode
        )
    }
}
