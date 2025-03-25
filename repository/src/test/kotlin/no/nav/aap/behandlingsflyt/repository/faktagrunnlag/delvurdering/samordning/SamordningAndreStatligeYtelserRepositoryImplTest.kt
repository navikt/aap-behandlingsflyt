package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.samordning

import no.nav.aap.behandlingsflyt.faktagrunnlag.GrunnlagKopierer
import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.andrestatligeytelservurdering.AndreStatligeYtelser
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.andrestatligeytelservurdering.SamordningAndreStatligeYtelserVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.andrestatligeytelservurdering.SamordningAndreStatligeYtelserVurderingPeriode
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
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SamordningAndreStatligeYtelserRepositoryImplTest {
    private val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(1))
    private val periodeTo = Periode(LocalDate.now(), LocalDate.now().plusYears(2))

    @Test
    fun `skal lagre ned en helt ny vurdering og hente den opp igjen`() {
        val dataSource = InitTestDatabase.dataSource
        val behandling = dataSource.transaction {
            behandling(it, sak(it))
        }

        // Lagre ytelse
        val vurdering = SamordningAndreStatligeYtelserVurdering(
            begrunnelse = "En fin begrunnelse",
            vurdertAv = "Lokalsaksbehandler",
            vurderingPerioder = listOf(
                SamordningAndreStatligeYtelserVurderingPeriode(
                    periode = periode,
                    beløp = 344,
                    ytelse = AndreStatligeYtelser.DAGPENGER,
                ),
                SamordningAndreStatligeYtelserVurderingPeriode(
                periode = periodeTo,
                beløp = 399,
                ytelse = AndreStatligeYtelser.BARNEPENSJON,
                )
            )
        )
        dataSource.transaction {
            SamordningAndreStatligeYtelserRepositoryImpl(it).lagre(behandling.id, vurdering)
        }

        val uthentet = dataSource.transaction {
            SamordningAndreStatligeYtelserRepositoryImpl(it).hentHvisEksisterer(behandling.id)
        }

        assertThat(uthentet).isNotNull
        assertThat(uthentet?.vurdering).isEqualTo(vurdering)
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
}
