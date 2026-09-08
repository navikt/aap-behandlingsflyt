package no.nav.aap.behandlingsflyt.avløp

import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehov
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokument
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.StrukturertDokument
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Status
import no.nav.aap.behandlingsflyt.help.person
import no.nav.aap.behandlingsflyt.hendelse.avløp.BehandlingHendelseServiceImpl
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.BehandlingFlytStoppetHendelse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.NyÅrsakTilBehandlingV0
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.pip.PipService
import no.nav.aap.behandlingsflyt.prosessering.MeldeperiodeTilMeldekortBackendJobbUtfører
import no.nav.aap.behandlingsflyt.prosessering.VarsleOppgaveOmHendelseJobbUtFører
import no.nav.aap.behandlingsflyt.prosessering.datadeling.DatadelingMeldePerioderOgSakStatusJobbUtfører
import no.nav.aap.behandlingsflyt.prosessering.datadeling.DatadelingMeldekortJobbUtfører
import no.nav.aap.behandlingsflyt.prosessering.statistikk.StatistikkJobbUtfører
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
import no.nav.aap.verdityper.dokument.Kanal
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

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

        val (behandlingHendelseService, behandling) = settOppFlyt()

        val avklaringsbehovene = mockk<Avklaringsbehovene>()

        every { avklaringsbehovene.alle() } returns listOf(
            Avklaringsbehov(
                1L,
                Definisjon.AVKLAR_LOVVALG_MEDLEMSKAP,
                emptyList(),
                StegType.VURDER_LOVVALG,
                true
            )
        )
        every { avklaringsbehovene.hentÅpneVentebehov() } returns emptyList()

        // ACT

        behandlingHendelseService.stoppet(behandling, avklaringsbehovene)

        // VERIFY

        val calls = mutableListOf<JobbInput>()
        verify {
            flytJobbRepository.leggTil(capture(calls))
        }

        assertThat(calls.size).isEqualTo(5)
        val jobbTyperOpprettet = calls.map { it.type() }
        assertThat(jobbTyperOpprettet).contains(VarsleOppgaveOmHendelseJobbUtFører.type)
        assertThat(jobbTyperOpprettet).contains(StatistikkJobbUtfører.type)
        assertThat(jobbTyperOpprettet).contains(DatadelingMeldePerioderOgSakStatusJobbUtfører.type)
        assertThat(jobbTyperOpprettet).contains(DatadelingMeldekortJobbUtfører.type)
        assertThat(jobbTyperOpprettet).contains(MeldeperiodeTilMeldekortBackendJobbUtfører.type)

        val hendelse = DefaultJsonMapper.fromJson<BehandlingFlytStoppetHendelse>(calls.first().payload())
        assertThat(hendelse.referanse.referanse).isEqualTo(behandling.referanse.referanse)
    }

    @Test
    fun `skal ikke opprette oppgaveHendelse dersom avklaringsbehovene er tomme`() {
        // SETUP

        val (behandlingHendelseService, behandling) = settOppFlyt()

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

        assertThat(calls.size).isEqualTo(4)
        val jobbTyperOpprettet = calls.map { it.type() }
        assertThat(jobbTyperOpprettet).doesNotContain(VarsleOppgaveOmHendelseJobbUtFører.type)
        assertThat(jobbTyperOpprettet).contains(StatistikkJobbUtfører.type)
        assertThat(jobbTyperOpprettet).contains(DatadelingMeldePerioderOgSakStatusJobbUtfører.type)
        assertThat(jobbTyperOpprettet).contains(DatadelingMeldekortJobbUtfører.type)
        assertThat(jobbTyperOpprettet).contains(MeldeperiodeTilMeldekortBackendJobbUtfører.type)
    }


    @Test
    fun `Hvis vurderingsbehov opprettet flere ganger på samme sak, utled reserverTil basert på senest mottatte dokument`() {
        val (behandlingHendelseService, behandling) = settOppFlyt()

        val avklaringsbehovene = mockk<Avklaringsbehovene>()

        every { avklaringsbehovene.alle() } returns listOf(
            Avklaringsbehov(
                1L,
                Definisjon.AVKLAR_LOVVALG_MEDLEMSKAP,
                emptyList(),
                StegType.VURDER_LOVVALG,
                true
            )
        )
        every { avklaringsbehovene.hentÅpneVentebehov() } returns emptyList()

        // To trekk søknad-"dokumenter"
        every {
            mottattDokumentRepository.hentDokumenterAvType(
                any<BehandlingId>(),
                InnsendingType.NY_ÅRSAK_TIL_BEHANDLING
            )
        } returns setOf(
            MottattDokument(
                referanse = InnsendingReferanse(
                    type = InnsendingReferanse.Type.MANUELL_OPPRETTELSE,
                    verdi = "123"
                ),
                sakId = SakId(123),
                behandlingId = behandling.id,
                mottattTidspunkt = LocalDateTime.now().minusDays(1),
                opprettetTid = LocalDateTime.now().minusDays(1),
                type = InnsendingType.NY_ÅRSAK_TIL_BEHANDLING,
                kanal = Kanal.DIGITAL,
                status = Status.MOTTATT,
                strukturertDokument = StrukturertDokument(
                    data = NyÅrsakTilBehandlingV0(
                        årsakerTilBehandling = listOf(Vurderingsbehov.SØKNAD_TRUKKET),
                        behandlingReferanse = behandling.referanse.toString(),
                        reserverTilBruker = "Veileder",
                        beskrivelse = "Søknad skal kanskje trekkes"
                    )
                )
            ),
            MottattDokument(
                referanse = InnsendingReferanse(
                    type = InnsendingReferanse.Type.MANUELL_OPPRETTELSE,
                    verdi = "123"
                ),
                sakId = SakId(123),
                behandlingId = behandling.id,
                mottattTidspunkt = LocalDateTime.now().minusHours(2),
                opprettetTid = LocalDateTime.now().minusHours(2),
                type = InnsendingType.NY_ÅRSAK_TIL_BEHANDLING,
                kanal = Kanal.DIGITAL,
                status = Status.MOTTATT,
                strukturertDokument = StrukturertDokument(
                    data = NyÅrsakTilBehandlingV0(
                        årsakerTilBehandling = listOf(Vurderingsbehov.SØKNAD_TRUKKET),
                        behandlingReferanse = behandling.referanse.toString(),
                        reserverTilBruker = "Veileder2",
                        beskrivelse = "Søknad skal kanskje trekkes"
                    )
                )
            ),
        )

        behandlingHendelseService.stoppet(behandling, avklaringsbehovene)

        val calls = mutableListOf<JobbInput>()
        verify {
            flytJobbRepository.leggTil(capture(calls))
        }

        // reserverTil skal settes til veileder som opprettet avklaringsbehovet sist
        val hendelse = DefaultJsonMapper.fromJson<BehandlingFlytStoppetHendelse>(calls.first().payload())
        assertThat(hendelse.referanse.referanse).isEqualTo(behandling.referanse.referanse)
        assertThat(hendelse.reserverTil).isEqualTo("Veileder2")
        assertThat(hendelse.reserverTilPerAvklaringsbehov).isEqualTo(
            mutableMapOf(
                Pair(
                    Definisjon.VURDER_TREKK_AV_SØKNAD.kode.name,
                    "Veileder2"
                )
            )
        )

    }

    private fun settOppFlyt(): Pair<BehandlingHendelseServiceImpl, Behandling> {
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

        every {
            mottattDokumentRepository.hentDokumenterAvType(
                any<BehandlingId>(),
                InnsendingType.UFØRE_VEDTAK_HENDELSE
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
        return Pair(behandlingHendelseService, behandling)
    }
}