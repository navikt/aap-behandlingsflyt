package no.nav.aap.behandlingsflyt.prosessering.datadeling

import no.nav.aap.behandlingsflyt.behandling.avbrytrevurdering.AvbrytRevurderingVurdering
import no.nav.aap.behandlingsflyt.behandling.avbrytrevurdering.AvbrytRevurderingÅrsak
import no.nav.aap.behandlingsflyt.behandling.søknad.AarsakTilTrekkSoknad
import no.nav.aap.behandlingsflyt.behandling.søknad.TrukketSøknadVurdering
import no.nav.aap.behandlingsflyt.datadeling.SakStatus
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.help.opprettInMemorySakOgRevurdering
import no.nav.aap.behandlingsflyt.help.sak
import no.nav.aap.behandlingsflyt.test.Fakes
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryAvbrytRevurderingRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryTrukketSøknadRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.inMemoryRepositoryProvider
import no.nav.aap.behandlingsflyt.test.testGatewayProvider
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.verdityper.dokument.JournalpostId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant

@Fakes
class SakstatusDatadelingServiceTest {
    @Test
    fun `utlede status, åpen førstegangbehandling`() {
        val sak = sak(inMemoryRepositoryProvider)
        val behandling = finnEllerOpprettBehandling(inMemoryRepositoryProvider, testGatewayProvider(), sak.saksnummer)

        val service = SakstatusDatadelingService(inMemoryRepositoryProvider, testGatewayProvider())

        val res = service.utledSakstatus(behandling.referanse)

        assertThat(res.status).isEqualTo(SakStatus.DatadelingBehandlingStatus.SOKNAD_UNDER_BEHANDLING)

    }

    @Test
    fun `utlede status, trukket søknad gir FERDIGBEHANDLET`() {
        val sak = sak(inMemoryRepositoryProvider)
        val behandling = finnEllerOpprettBehandling(inMemoryRepositoryProvider, testGatewayProvider(), sak.saksnummer)

        InMemoryTrukketSøknadRepository.lagreTrukketSøknadVurdering(
            behandling.id,
            TrukketSøknadVurdering(
                journalpostId = JournalpostId("123"),
                begrunnelse = "Bruker ønsker ikke lenger søknaden behandlet",
                skalTrekkes = true,
                vurdertAv = Bruker("Z999999"),
                vurdert = Instant.now(),
                aarsak = AarsakTilTrekkSoknad.BRUKER_ONSKER_IKKE_SOKE_LENGER,
            )
        )

        val service = SakstatusDatadelingService(inMemoryRepositoryProvider, testGatewayProvider())

        val res = service.utledSakstatus(behandling.referanse)

        assertThat(res.status).isEqualTo(SakStatus.DatadelingBehandlingStatus.FERDIGBEHANDLET)
    }

    @Test
    fun `utlede status, avbrutt revurdering gir FERDIGBEHANDLET`() {
        val (_, _, revurdering) = opprettInMemorySakOgRevurdering()

        InMemoryAvbrytRevurderingRepository.lagre(
            revurdering.id,
            AvbrytRevurderingVurdering(
                årsak = AvbrytRevurderingÅrsak.REVURDERINGEN_BLE_OPPRETTET_VED_EN_FEIL,
                begrunnelse = "Revurderingen er ikke lenger aktuell",
                vurdertAv = Bruker("Z999999"),
            )
        )

        val service = SakstatusDatadelingService(inMemoryRepositoryProvider, testGatewayProvider())

        val res = service.utledSakstatus(revurdering.referanse)

        assertThat(res.status).isEqualTo(SakStatus.DatadelingBehandlingStatus.FERDIGBEHANDLET)
    }
}