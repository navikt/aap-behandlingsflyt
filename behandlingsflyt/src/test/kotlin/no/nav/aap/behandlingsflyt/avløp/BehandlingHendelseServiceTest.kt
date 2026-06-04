package no.nav.aap.behandlingsflyt.avløp

import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepository
import no.nav.aap.behandlingsflyt.help.person
import no.nav.aap.behandlingsflyt.hendelse.avløp.BehandlingHendelseServiceImpl
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.BehandlingFlytStoppetHendelse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.pip.PipService
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingService
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class BehandlingHendelseServiceTest {
    private val sakService = mockk<SakService>()
    private val flytJobbRepository = mockk<FlytJobbRepository>()
    private val mottattDokumentRepository = mockk<MottattDokumentRepository>()
    private val pipRepository = mockk<PipService>()
    private val behandlingService = mockk<BehandlingService>()
    private val unleashGateway = mockk<UnleashGateway>()

    @AfterEach
    fun afterEach() {
        checkUnnecessaryStub(
            sakService,
            flytJobbRepository,
            mottattDokumentRepository,
            pipRepository,
            behandlingService,
            unleashGateway
        )
    }

    @Test
    fun `verifiser at FlytJobbRepository blir kalt med riktige argumenter`() {
        // SETUP

        every { flytJobbRepository.leggTil(any()) } returns Unit

        every {
            mottattDokumentRepository.hentDokumenterAvType(
                any<BehandlingId>(),
                InnsendingType.OPPFØLGINGSOPPGAVE
            )
        } returns emptySet()

        every {
            mottattDokumentRepository.hentDokumenterAvType(
                any<BehandlingId>(),
                InnsendingType.NY_ÅRSAK_TIL_BEHANDLING
            )
        } returns emptySet()

        every {
            mottattDokumentRepository.hentDokumenterAvType(
                any<BehandlingId>(),
                InnsendingType.MANUELL_REVURDERING
            )
        } returns emptySet()


        every { pipRepository.finnIdenterPåBehandling(any<BehandlingReferanse>()) } returns emptyList()


        val behandlingHendelseService =
            BehandlingHendelseServiceImpl(
                flytJobbRepository,
                sakService,
                mottattDokumentRepository,
                pipRepository,
                behandlingService,
                unleashGateway,
            )

        val behandling = Behandling(
            BehandlingId(0),
            sakId = SakId(1),
            typeBehandling = TypeBehandling.Førstegangsbehandling,
            årsakTilOpprettelse = ÅrsakTilOpprettelse.SØKNAD,
            forrigeBehandlingId = null,
            versjon = 1
        )

        every { behandlingService.utledFaktiskBehandlingstype(behandling) }.returns(behandling.typeBehandling())
        
        every { sakService.hent(SakId(1)) } returns Sak(
            id = SakId(1),
            saksnummer = Saksnummer("1"),
            person = person(),
            rettighetsperiode = Periode(LocalDate.now(), LocalDate.now())
        )

        val avklaringsbehovene = mockk<Avklaringsbehovene>()

        every { avklaringsbehovene.alle() } returns emptyList()
        every { avklaringsbehovene.hentÅpneVentebehov() } returns emptyList()
        every { unleashGateway.isEnabled(any()) } returns true


        // ACT

        behandlingHendelseService.stoppet(behandling, avklaringsbehovene)

        // VERIFY

        val calls = mutableListOf<JobbInput>()
        verify {
            flytJobbRepository.leggTil(capture(calls))
        }

        val hendelse = DefaultJsonMapper.fromJson<BehandlingFlytStoppetHendelse>(calls.first().payload())
        assertThat(hendelse.referanse.referanse).isEqualTo(behandling.referanse.referanse)
    }
}