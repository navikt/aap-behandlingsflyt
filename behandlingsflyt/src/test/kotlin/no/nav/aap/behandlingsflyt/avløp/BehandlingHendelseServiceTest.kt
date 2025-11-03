package no.nav.aap.behandlingsflyt.avløp

import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.MeldekortRepository
import no.nav.aap.behandlingsflyt.hendelse.avløp.BehandlingHendelseServiceImpl
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.BehandlingFlytStoppetHendelse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.pip.PipRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import java.util.*

@ExtendWith(MockKExtension::class)
@MockKExtension.CheckUnnecessaryStub
class BehandlingHendelseServiceTest {
    @Test
    fun `verifiser at FlytJobbRepository blir kalt med riktige argumenter`() {
        // SETUP
        val sakService = mockk<SakService>()
        val flytJobbRepository = mockk<FlytJobbRepository>()
        val mottattDokumentRepository = mockk<MottattDokumentRepository>()
        val pipRepository = mockk<PipRepository>()
        val meldekortRepository = mockk<MeldekortRepository>()

        every { flytJobbRepository.leggTil(any()) } returns Unit

        every { meldekortRepository.hentHvisEksisterer(BehandlingId(0)) } returns null

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
                meldekortRepository
            )

        val behandling = Behandling(
            BehandlingId(0),
            sakId = SakId(1),
            typeBehandling = TypeBehandling.Førstegangsbehandling,
            årsakTilOpprettelse = ÅrsakTilOpprettelse.SØKNAD,
            forrigeBehandlingId = null,
            versjon = 1
        )

        every { sakService.hent(SakId(1)) } returns Sak(
            id = SakId(1),
            saksnummer = Saksnummer("1"),
            person = Person(UUID.randomUUID(), listOf(Ident("123", true))),
            rettighetsperiode = Periode(LocalDate.now(), LocalDate.now())
        )

        val avklaringsbehovene = mockk<Avklaringsbehovene>()

        every { avklaringsbehovene.alle() } returns emptyList()
        every { avklaringsbehovene.hentÅpneVentebehov() } returns emptyList()


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