package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.revurdering

import no.nav.aap.behandlingsflyt.behandling.avbrytrevurdering.AvbrytRevurderingVurdering
import no.nav.aap.behandlingsflyt.behandling.avbrytrevurdering.AvbrytRevurderingÅrsak
import no.nav.aap.behandlingsflyt.help.FakePdlGateway
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.avbrytrevurdering.AvbrytRevurderingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.test.ident
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Bruker
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate

class AvbrytRevurderingRepositoryImplTest {
    private val dataSource = InitTestDatabase.freshDatabase()
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
        val sak = sak()
        dataSource.transaction { connection ->
            val førstegangsbehandling =
                opprettBehandlingMedVurdering(TypeBehandling.Førstegangsbehandling, sak.id, null, vurderingsbehovOgÅrsakSøknad)
            val revurderingBehandling =
                opprettBehandlingMedVurdering(TypeBehandling.Revurdering, sak.id, førstegangsbehandling.id, vurderingsbehovOgÅrsakManuell)

            val avbrytRevurderingRepository = AvbrytRevurderingRepositoryImpl(connection)
            val vurdering = AvbrytRevurderingVurdering(
                årsak = AvbrytRevurderingÅrsak.REVURDERINGEN_BLE_OPPRETTET_VED_EN_FEIL,
                begrunnelse = "En begrunnelse",
                vurdertAv = Bruker(ident = "12345"),
            )

            avbrytRevurderingRepository.lagre(revurderingBehandling.id, vurdering)
            val grunnlag = avbrytRevurderingRepository.hentHvisEksisterer(revurderingBehandling.id)!!
            assertEqualVurdering(grunnlag.vurdering, vurdering)
        }
    }

    @Test
    fun `kan lagre flere vurderinger på samme revurdering og hente ut nyeste vurdering som del av grunnlaget`() {
        val sak = sak()
        dataSource.transaction { connection ->
            val førstegangsbehandling =
                opprettBehandlingMedVurdering(TypeBehandling.Førstegangsbehandling, sak.id, null, vurderingsbehovOgÅrsakSøknad)
            val revurderingBehandling =
                opprettBehandlingMedVurdering(TypeBehandling.Revurdering, sak.id, førstegangsbehandling.id, vurderingsbehovOgÅrsakManuell)

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
            assertEqualVurdering(grunnlag.vurdering, vurdering2)
        }
    }

    private fun sak(): Sak {
        return dataSource.transaction { connection ->
            val personOgSakService =
                PersonOgSakService(
                    FakePdlGateway,
                    PersonRepositoryImpl(connection),
                    SakRepositoryImpl(connection)
                )
            personOgSakService.finnEllerOpprett(ident(), Periode(LocalDate.now(), LocalDate.now().plusYears(3)))
        }
    }

    private fun assertEqualVurdering(
        vurdering1: AvbrytRevurderingVurdering,
        vurdering2: AvbrytRevurderingVurdering
    ) {
        Assertions.assertThat(vurdering1.årsak).isEqualTo(vurdering2.årsak)
        Assertions.assertThat(vurdering1.begrunnelse).isEqualTo(vurdering2.begrunnelse)
        Assertions.assertThat(vurdering1.vurdertAv.ident).isEqualTo(vurdering2.vurdertAv.ident)
    }

    private fun opprettBehandlingMedVurdering(
        typeBehandling: TypeBehandling,
        sakId: SakId,
        forrigeBehandlingId: BehandlingId?,
        årsak: VurderingsbehovOgÅrsak,
    ): Behandling {
        return dataSource.transaction { connection ->
            val behandlingRepo = BehandlingRepositoryImpl(connection)
            val behandling = behandlingRepo.opprettBehandling(sakId, typeBehandling, forrigeBehandlingId, årsak)
            behandling
        }
    }
}