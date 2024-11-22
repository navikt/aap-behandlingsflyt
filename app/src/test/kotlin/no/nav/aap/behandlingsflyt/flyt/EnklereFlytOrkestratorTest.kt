package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.flyt.testutil.DummyBehandlingHendelseService
import no.nav.aap.behandlingsflyt.flyt.testutil.DummyInformasjonskravGrunnlag
import no.nav.aap.behandlingsflyt.flyt.testutil.DummyStegKonstruktør
import no.nav.aap.behandlingsflyt.flyt.testutil.DummyVentebehovEvaluererService
import no.nav.aap.behandlingsflyt.flyt.testutil.InMemoryAvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.flyt.testutil.InMemoryBehandlingRepository
import no.nav.aap.behandlingsflyt.flyt.testutil.InMemorySakRepository
import no.nav.aap.behandlingsflyt.forretningsflyt.behandlingstyper.Førstegangsbehandling
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.periodisering.PerioderTilVurderingService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.behandlingsflyt.test.modell.genererIdent
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

class EnklereFlytOrkestratorTest {

    private val sakRepository = InMemorySakRepository
    private val sakService = SakService(InMemorySakRepository)
    private val behandlingRepository = InMemoryBehandlingRepository

    private val flytOrkestrator = FlytOrkestrator(
        stegKonstruktør = DummyStegKonstruktør(),
        perioderTilVurderingService = PerioderTilVurderingService(
            sakService = sakService,
            behandlingRepository = behandlingRepository
        ),
        informasjonskravGrunnlag = DummyInformasjonskravGrunnlag(),
        behandlingRepository = behandlingRepository,
        behandlingFlytRepository = behandlingRepository,
        ventebehovEvaluererService = DummyVentebehovEvaluererService(),
        sakRepository = sakRepository,
        avklaringsbehovRepository = InMemoryAvklaringsbehovRepository,
        behandlingHendelseService = DummyBehandlingHendelseService
    )

    @Test
    fun `skal gå gjennom alle stegene definert i beandlings`() {
        val person = Person(1, UUID.randomUUID(), listOf(genererIdent(LocalDate.now().minusYears(23))))

        val sak = sakRepository.finnEllerOpprett(person, Periode(LocalDate.now(), LocalDate.now().plusYears(1)))
        val behandling =
            behandlingRepository.opprettBehandling(sak.id, listOf(), TypeBehandling.Førstegangsbehandling, null)

        val flytKontekst = flytOrkestrator.opprettKontekst(behandling.sakId, behandling.id)
        flytOrkestrator.forberedBehandling(flytKontekst)
        flytOrkestrator.prosesserBehandling(flytKontekst)

        assertThat(behandling.stegHistorikk()).isNotEmpty()
        assertThat(behandling.stegHistorikk().map { tilstand -> tilstand.steg() }.distinct()).containsExactlyElementsOf(
            Førstegangsbehandling.flyt().stegene()
        )

        assertThat(behandling.status()).isEqualTo(Status.AVSLUTTET)
    }
}