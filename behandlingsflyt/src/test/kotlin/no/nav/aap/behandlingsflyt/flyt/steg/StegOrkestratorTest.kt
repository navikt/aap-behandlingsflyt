package no.nav.aap.behandlingsflyt.flyt.steg

import no.nav.aap.behandlingsflyt.faktagrunnlag.FakePdlGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravGrunnlagImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.InformasjonskravRepositoryImpl
import no.nav.aap.behandlingsflyt.flyt.steg.internal.StegKonstruktørImpl
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.periodisering.FlytKontekstMedPeriodeService
import no.nav.aap.behandlingsflyt.repository.avklaringsbehov.AvklaringsbehovRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.postgresRepositoryRegistry
import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.StegStatus
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.behandlingsflyt.test.FakeUnleash
import no.nav.aap.behandlingsflyt.test.FreshDatabaseExtension
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import javax.sql.DataSource

@ExtendWith(FreshDatabaseExtension::class)
internal class StegOrkestratorTest(val dataSource: DataSource) {

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

            val behandling = finnEllerOpprettBehandling(connection, sak)
            assertThat(behandling.typeBehandling()).isEqualTo(TypeBehandling.Førstegangsbehandling)

            val kontekst = behandling.flytKontekst()

            val resultat = StegOrkestrator(
                informasjonskravGrunnlag = InformasjonskravGrunnlagImpl(
                    InformasjonskravRepositoryImpl(connection),
                    postgresRepositoryRegistry.provider(connection),
                ),
                behandlingRepository = BehandlingRepositoryImpl(connection),
                avklaringsbehovRepository = AvklaringsbehovRepositoryImpl(connection),
                stegKonstruktør = StegKonstruktørImpl(postgresRepositoryRegistry.provider(connection))
            ).utfør(
                TestFlytSteg,
                FlytKontekstMedPeriodeService(
                    SakService(SakRepositoryImpl(connection)),
                    BehandlingRepositoryImpl(connection),
                    FakeUnleash,
               ).utled(kontekst, TestFlytSteg.type()),
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
