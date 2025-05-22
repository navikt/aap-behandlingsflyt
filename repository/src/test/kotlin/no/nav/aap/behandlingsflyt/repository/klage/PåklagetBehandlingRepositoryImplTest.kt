package no.nav.aap.behandlingsflyt.repository.klage

import no.nav.aap.behandlingsflyt.behandling.søknad.TrukketSøknadService
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.påklagetbehandling.PåklagetBehandlingVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.påklagetbehandling.PåklagetVedtakType
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.repository.avklaringsbehov.FakePdlGateway
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.klage.PåklagetBehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.ÅrsakTilBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.ident
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryAvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryTrukketSøknadRepository
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import java.time.LocalDate

internal class PåklagetBehandlingRepositoryImplTest {
    private val dataSource = InitTestDatabase.freshDatabase()
    
    @Test
    fun `Lagrer og henter påklagetbehandling med id`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)
            val klageBehandling = finnEllerOpprettBehandling(connection, sak, ÅrsakTilBehandling.MOTATT_KLAGE)
            
            val påklagetBehandlingRepository = PåklagetBehandlingRepositoryImpl(connection)
            val vurdering = PåklagetBehandlingVurdering(
                påklagetVedtakType = PåklagetVedtakType.KELVIN_BEHANDLING,
                påklagetBehandling = behandling.id,
                vurdertAv = "ident"
            )
            
            påklagetBehandlingRepository.lagre(klageBehandling.id, vurdering)
            val grunnlag = påklagetBehandlingRepository.hentHvisEksisterer(klageBehandling.id)!!
            assertThat(grunnlag.vurdering.påklagetVedtakType).isEqualTo(PåklagetVedtakType.KELVIN_BEHANDLING)
            assertThat(grunnlag.vurdering.påklagetBehandling).isEqualTo(behandling.id)
            assertThat(grunnlag.vurdering.vurdertAv).isEqualTo("ident")
            assertNotNull(grunnlag.vurdering.opprettet)
        }
    }

    @Test
    fun `Lagrer og henter påklagetbehandling med referanse`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)
            val klageBehandling = finnEllerOpprettBehandling(connection, sak, ÅrsakTilBehandling.MOTATT_KLAGE)

            val påklagetBehandlingRepository = PåklagetBehandlingRepositoryImpl(connection)
            val vurdering = PåklagetBehandlingVurdering(
                påklagetVedtakType = PåklagetVedtakType.KELVIN_BEHANDLING,
                påklagetBehandling = behandling.id,
                vurdertAv = "ident"
            )

            påklagetBehandlingRepository.lagre(klageBehandling.id, vurdering)
            val vurderingMedReferanse = påklagetBehandlingRepository.hentGjeldendeVurderingMedReferanse(klageBehandling.referanse)!!
            assertThat(vurderingMedReferanse.påklagetVedtakType).isEqualTo(PåklagetVedtakType.KELVIN_BEHANDLING)
            assertThat(vurderingMedReferanse.påklagetBehandling).isEqualTo(behandling.id)
            assertThat(vurderingMedReferanse.referanse?.referanse).isEqualTo(behandling.referanse.referanse)
            assertThat(vurderingMedReferanse.vurdertAv).isEqualTo("ident")
            assertNotNull(vurderingMedReferanse.opprettet)
        }
    }

    private fun sak(connection: DBConnection): Sak {
        return PersonOgSakService(
            FakePdlGateway,
            PersonRepositoryImpl(connection),
            SakRepositoryImpl(connection),
            BehandlingRepositoryImpl(connection),
            TrukketSøknadService(
                InMemoryAvklaringsbehovRepository,
                InMemoryTrukketSøknadRepository
            ),
        ).finnEllerOpprett(ident(), periode)
    }

    private companion object {
        private val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))
    }
}