package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.revurdering

import no.nav.aap.behandlingsflyt.behandling.avbrytrevurdering.AvbrytRevurderingVurdering
import no.nav.aap.behandlingsflyt.behandling.avbrytrevurdering.AvbrytRevurderingÅrsak
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.help.sak
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.avbrytrevurdering.AvbrytRevurderingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.postgresRepositoryRegistry
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.verdityper.Bruker
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class AvbrytRevurderingRepositoryImplTest {
    companion object {
        private lateinit var dataSource: TestDataSource

        @BeforeAll
        @JvmStatic
        fun setup() {
            dataSource = TestDataSource()
        }

        @AfterAll
        @JvmStatic
        fun tearDown() = dataSource.close()
    }

    private val vurderingsbehovOgÅrsakSøknad = VurderingsbehovOgÅrsak(
        vurderingsbehov = listOf(VurderingsbehovMedPeriode(type = Vurderingsbehov.MOTTATT_SØKNAD)),
        årsak = ÅrsakTilOpprettelse.SØKNAD,
    )
    private val vurderingsbehovOgÅrsakManuell = VurderingsbehovOgÅrsak(
        vurderingsbehov = listOf(VurderingsbehovMedPeriode(type = Vurderingsbehov.REVURDERING_AVBRUTT)),
        årsak = ÅrsakTilOpprettelse.MANUELL_OPPRETTELSE,
    )

    @Test
    fun `Lagrer og henter avbrutt revurdering`() {
        val sak = dataSource.transaction { sak(it) }

        dataSource.transaction { connection ->
            val førstegangsbehandling =
                finnEllerOpprettBehandling(connection, sak, vurderingsbehovOgÅrsakSøknad.vurderingsbehov)

            postgresRepositoryRegistry.provider(connection).provide<BehandlingRepository>().oppdaterBehandlingStatus(
                førstegangsbehandling.id,
                Status.AVSLUTTET
            )
            val revurderingBehandling =
                opprettBehandlingMedVurdering(
                    connection,
                    TypeBehandling.Revurdering,
                    sak.id,
                    førstegangsbehandling.id,
                    vurderingsbehovOgÅrsakManuell
                )

            val avbrytRevurderingRepository = AvbrytRevurderingRepositoryImpl(connection)
            val vurdering = AvbrytRevurderingVurdering(
                årsak = AvbrytRevurderingÅrsak.REVURDERINGEN_BLE_OPPRETTET_VED_EN_FEIL,
                begrunnelse = "En begrunnelse",
                vurdertAv = Bruker(ident = "12345"),
            )

            avbrytRevurderingRepository.lagre(revurderingBehandling.id, vurdering)
            val grunnlag = avbrytRevurderingRepository.hentHvisEksisterer(revurderingBehandling.id)!!
            assertThat(grunnlag.vurdering).isEqualTo(vurdering)
        }
    }

    @Test
    fun `kan lagre flere vurderinger på samme revurdering og hente ut nyeste vurdering som del av grunnlaget`() {
        val sak = dataSource.transaction { sak(it) }

        dataSource.transaction { connection ->
            val førstegangsbehandling =
                opprettBehandlingMedVurdering(
                    connection,
                    TypeBehandling.Førstegangsbehandling,
                    sak.id,
                    null,
                    vurderingsbehovOgÅrsakSøknad
                )
            val revurderingBehandling =
                opprettBehandlingMedVurdering(
                    connection,
                    TypeBehandling.Revurdering,
                    sak.id,
                    førstegangsbehandling.id,
                    vurderingsbehovOgÅrsakManuell
                )

            val avbrytRevurderingRepository = AvbrytRevurderingRepositoryImpl(connection)
            val vurdering1 = AvbrytRevurderingVurdering(
                årsak = AvbrytRevurderingÅrsak.REVURDERINGEN_BLE_OPPRETTET_VED_EN_FEIL,
                begrunnelse = "En begrunnelse",
                vurdertAv = Bruker(ident = "12345"),
            )

            val vurdering2 = AvbrytRevurderingVurdering(
                årsak = AvbrytRevurderingÅrsak.DET_HAR_OPPSTAATT_EN_FEIL_OG_BEHANDLINGEN_MAA_STARTES_PAA_NYTT,
                begrunnelse = "Begrunnelse2",
                vurdertAv = Bruker(ident = "54321"),
            )

            avbrytRevurderingRepository.lagre(revurderingBehandling.id, vurdering1)
            avbrytRevurderingRepository.lagre(revurderingBehandling.id, vurdering2)
            val grunnlag = avbrytRevurderingRepository.hentHvisEksisterer(revurderingBehandling.id)!!
            assertThat(grunnlag.vurdering).isEqualTo(vurdering2)

        }
    }

    private fun opprettBehandlingMedVurdering(
        connection: DBConnection,
        typeBehandling: TypeBehandling,
        sakId: SakId,
        forrigeBehandlingId: BehandlingId?,
        årsak: VurderingsbehovOgÅrsak,
    ): Behandling {
        val behandlingRepo = BehandlingRepositoryImpl(connection)
        val behandling = behandlingRepo.opprettBehandling(sakId, typeBehandling, forrigeBehandlingId, årsak)
        return behandling
    }
}