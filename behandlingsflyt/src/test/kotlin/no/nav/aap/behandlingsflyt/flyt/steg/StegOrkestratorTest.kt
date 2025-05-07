package no.nav.aap.behandlingsflyt.flyt.steg

import no.nav.aap.behandlingsflyt.faktagrunnlag.FakePdlGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.GrunnlagKopierer
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravGrunnlagImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravRepositoryImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.flyt.steg.internal.StegKonstruktørImpl
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.periodisering.PerioderTilVurderingService
import no.nav.aap.behandlingsflyt.repository.avklaringsbehov.AvklaringsbehovRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Årsak
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.StegStatus
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.ÅrsakTilBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.behandlingsflyt.test.FakeUnleash
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.repository.RepositoryRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class StegOrkestratorTest {

    companion object {
        private val dataSource = InitTestDatabase.freshDatabase()
    }

    @Test
    fun `ved avklaringsbehov skal vi gå gjennom statusene START-UTFØRER-AVKARLINGSPUNKT`() {
        dataSource.transaction { connection ->
            val ident = Ident("123123123126")
            val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))

            val sak = PersonOgSakService(
                FakePdlGateway,
                PersonRepositoryImpl(connection),
                SakRepositoryImpl(connection)
            ).finnEllerOpprett(ident, periode)

            val behandling = SakOgBehandlingService(
                GrunnlagKopierer(connection), SakRepositoryImpl(connection),
                BehandlingRepositoryImpl(connection)
            ).finnEllerOpprettBehandling(
                sak.saksnummer,
                listOf(Årsak(ÅrsakTilBehandling.MOTTATT_SØKNAD))
            ).behandling
            assertThat(behandling.typeBehandling()).isEqualTo(TypeBehandling.Førstegangsbehandling)

            val kontekst = behandling.flytKontekst()

            val resultat = StegOrkestrator(
                aktivtSteg = TestFlytSteg,
                informasjonskravGrunnlag = InformasjonskravGrunnlagImpl(
                    InformasjonskravRepositoryImpl(connection),
                    RepositoryRegistry.provider(connection),
                ),
                behandlingRepository = BehandlingRepositoryImpl(connection),
                avklaringsbehovRepository = AvklaringsbehovRepositoryImpl(connection),
                perioderTilVurderingService = PerioderTilVurderingService(
                    SakService(SakRepositoryImpl(connection)),
                    BehandlingRepositoryImpl(connection),
                    FakeUnleash(mapOf()),
                ),
                stegKonstruktør = StegKonstruktørImpl(connection)
            ).utfør(
                kontekst,
                behandling,
                listOf()
            )

            assertThat(resultat).isNotNull

            val stegHistorikk = BehandlingRepositoryImpl(connection).hentStegHistorikk(behandling.id)
            assertThat(stegHistorikk).hasSize(4)
            assertThat(stegHistorikk[0].status()).isEqualTo(StegStatus.START)
            assertThat(stegHistorikk[1].status()).isEqualTo(StegStatus.OPPDATER_FAKTAGRUNNLAG)
            assertThat(stegHistorikk[2].status()).isEqualTo(StegStatus.UTFØRER)
            assertThat(stegHistorikk[3].status()).isEqualTo(StegStatus.AVKLARINGSPUNKT)
        }
    }
}
