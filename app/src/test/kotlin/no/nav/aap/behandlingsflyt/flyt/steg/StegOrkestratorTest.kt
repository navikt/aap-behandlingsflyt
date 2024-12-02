package no.nav.aap.behandlingsflyt.flyt.steg

import kotlinx.coroutines.runBlocking
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepositoryImpl
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.FakePdlGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravGrunnlagImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.flyt.steg.internal.StegKonstruktørImpl
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.periodisering.PerioderTilVurderingService
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Årsak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.db.SakRepositoryImpl
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.StegStatus
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.ÅrsakTilBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class StegOrkestratorTest {

    companion object {
        val dataSource = InitTestDatabase.dataSource
    }

    @Test
    fun `ved avklaringsbehov skal vi gå gjennom statusene START-UTFØRER-AVKARLINGSPUNKT`() {
        dataSource.transaction { connection ->
            val ident = Ident("123123123126")
            val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))

            val sak = runBlocking { PersonOgSakService(connection, FakePdlGateway).finnEllerOpprett(ident, periode) }
            val behandling = SakOgBehandlingService(connection).finnEllerOpprettBehandling(
                sak.saksnummer,
                listOf(Årsak(ÅrsakTilBehandling.MOTTATT_SØKNAD))
            ).behandling
            assertThat(behandling.typeBehandling()).isEqualTo(TypeBehandling.Førstegangsbehandling)

            val kontekst = behandling.flytKontekst()

            val resultat = StegOrkestrator(
                aktivtSteg = TestFlytSteg,
                informasjonskravGrunnlag = InformasjonskravGrunnlagImpl(connection),
                behandlingFlytRepository = BehandlingRepositoryImpl(connection),
                avklaringsbehovRepository = AvklaringsbehovRepositoryImpl(connection),
                perioderTilVurderingService = PerioderTilVurderingService(
                    SakService(SakRepositoryImpl(connection)),
                    BehandlingRepositoryImpl(connection)
                ),
                stegKonstruktør = StegKonstruktørImpl(connection)
            ).utfør(
                kontekst,
                behandling,
                listOf()
            )

            assertThat(resultat).isNotNull

            assertThat(behandling.stegHistorikk()).hasSize(4)
            assertThat(behandling.stegHistorikk()[0].status()).isEqualTo(StegStatus.START)
            assertThat(behandling.stegHistorikk()[1].status()).isEqualTo(StegStatus.OPPDATER_FAKTAGRUNNLAG)
            assertThat(behandling.stegHistorikk()[2].status()).isEqualTo(StegStatus.UTFØRER)
            assertThat(behandling.stegHistorikk()[3].status()).isEqualTo(StegStatus.AVKLARINGSPUNKT)
        }
    }
}
