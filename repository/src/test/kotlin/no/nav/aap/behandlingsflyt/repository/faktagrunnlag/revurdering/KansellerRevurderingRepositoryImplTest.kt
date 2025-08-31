package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.revurdering

import no.nav.aap.behandlingsflyt.behandling.kansellerrevurdering.KansellerRevurderingVurdering
import no.nav.aap.behandlingsflyt.behandling.kansellerrevurdering.KansellerRevurderingÅrsak
import no.nav.aap.behandlingsflyt.help.FakePdlGateway
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.kansellerrevurdering.KansellerRevurderingRepositoryImpl
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

class KansellerRevurderingRepositoryImplTest {
    private val dataSource = InitTestDatabase.freshDatabase()
    private val vurderingsbehovOgÅrsakSøknad = VurderingsbehovOgÅrsak(
        vurderingsbehov = listOf(VurderingsbehovMedPeriode(type = Vurderingsbehov.MOTTATT_SØKNAD)),
        årsak = ÅrsakTilOpprettelse.SØKNAD,
    )
    private val vurderingsbehovOgÅrsakManuell = VurderingsbehovOgÅrsak(
        vurderingsbehov = listOf(VurderingsbehovMedPeriode(type = Vurderingsbehov.REVURDERING_KANSELLERT)),
        årsak = ÅrsakTilOpprettelse.MANUELL_OPPRETTELSE,
    )

    @Test
    fun `Lagrer og henter kansellert revurdering`() {
        val sak = sak()
        dataSource.transaction { connection ->
            val førstegangsbehandling =
                opprettBehandlingMedVurdering(TypeBehandling.Førstegangsbehandling, sak.id, null, vurderingsbehovOgÅrsakSøknad)
            val revurderingBehandling =
                opprettBehandlingMedVurdering(TypeBehandling.Revurdering, sak.id, førstegangsbehandling.id, vurderingsbehovOgÅrsakManuell)

            val kansellerRevurderingRepository = KansellerRevurderingRepositoryImpl(connection)
            val vurdering = KansellerRevurderingVurdering(
                årsak = KansellerRevurderingÅrsak.REVURDERINGEN_ER_FEILREGISTRERT,
                begrunnelse = "En begrunnelse",
                vurdertAv = Bruker(ident = "12345"),
            )

            kansellerRevurderingRepository.lagre(revurderingBehandling.id, vurdering)
            val grunnlag = kansellerRevurderingRepository.hentHvisEksisterer(revurderingBehandling.id)!!
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

            val kansellerRevurderingRepository = KansellerRevurderingRepositoryImpl(connection)
            val vurdering1 = KansellerRevurderingVurdering(
                årsak = KansellerRevurderingÅrsak.REVURDERINGEN_ER_FEILREGISTRERT,
                begrunnelse = "En begrunnelse",
                vurdertAv = Bruker(ident = "12345"),
            )

            val vurdering2 = KansellerRevurderingVurdering(
                årsak = KansellerRevurderingÅrsak.REVURDERING_ER_IKKE_LENGER_AKTUELL,
                begrunnelse = "Begrunnelse2",
                vurdertAv = Bruker(ident = "54321"),
            )

            kansellerRevurderingRepository.lagre(revurderingBehandling.id, vurdering1)
            kansellerRevurderingRepository.lagre(revurderingBehandling.id, vurdering2)
            val grunnlag = kansellerRevurderingRepository.hentHvisEksisterer(revurderingBehandling.id)!!
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
        vurdering1: KansellerRevurderingVurdering,
        vurdering2: KansellerRevurderingVurdering
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