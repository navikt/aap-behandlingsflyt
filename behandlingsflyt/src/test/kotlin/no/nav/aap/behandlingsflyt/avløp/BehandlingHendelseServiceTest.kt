package no.nav.aap.behandlingsflyt.avløp

import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.aap.behandlingsflyt.SYSTEMBRUKER
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.GjenståendeSykedager
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.GjenståendeSykedagerGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokument
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.StrukturertDokument
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Status
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.arena.ArenaMaksdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.arena.ArenaVedtakStatus
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.Kravreferanse
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.RelevantSøknadKrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.Søknadsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.SøknadsdatoÅrsak
import no.nav.aap.behandlingsflyt.help.person
import no.nav.aap.behandlingsflyt.hendelse.avløp.BehandlingHendelseServiceImpl
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.BehandlingFlytStoppetHendelse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.GjenståendeSykedagerDto
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.GjenståendeSykedagerGrunnlagDto
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.NySakEtterOpphørArenaDto
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
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryArenaMaksdatoRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryAvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryGjenståendeSykedagerRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryKravRepository
import no.nav.aap.behandlingsflyt.test.februar
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.behandlingsflyt.test.mars
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import no.nav.aap.verdityper.dokument.JournalpostId
import no.nav.aap.verdityper.dokument.Kanal
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.random.Random

class BehandlingHendelseServiceTest {
    private val sakService = mockk<SakService>()
    private val flytJobbRepository = mockk<FlytJobbRepository>()
    private val mottattDokumentRepository = mockk<MottattDokumentRepository>()
    private val pipRepository = mockk<PipService>()
    private val behandlingService = mockk<BehandlingService>()
    private val avklaringsbehovService = mockk<AvklaringsbehovService>(relaxed = true)

    @BeforeEach
    fun beforeEach() {
        InMemoryGjenståendeSykedagerRepository.slett(BehandlingId(0))
        InMemoryKravRepository.slett(BehandlingId(0))
        InMemoryArenaMaksdatoRepository.slett(BehandlingId(0))
        every { avklaringsbehovService.finnAktivtAvklaringsbehov(any()) } returns null
    }

    @AfterEach
    fun afterEach() {
        checkUnnecessaryStub(
            sakService,
            flytJobbRepository,
            mottattDokumentRepository,
            pipRepository,
            behandlingService,
        )
    }

    @Test
    fun `verifiser at FlytJobbRepository blir kalt med riktige argumenter`() {
        // SETUP

        val (behandlingHendelseService, behandling) = settOppFlyt()

        val avklaringsbehovene = avklaringsbehoveneMedÉttÅpent()

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

    @ParameterizedTest
    @EnumSource(
        GjenståendeSykedagerGrunnlag::class,
        names = ["VEDTAK_I_NY_LØSNING", "SISTE_UTBETALING_I_INFOTRYGD", "MAKSDATO_I_INFOTRYGD"]
    )
    fun `sender gjenstående sykedager og maksdato til oppgave for førstegangsbehandling`(
        grunnlag: GjenståendeSykedagerGrunnlag
    ) {
        val (behandlingHendelseService, behandling) = settOppFlyt()
        InMemoryGjenståendeSykedagerRepository.lagre(
            behandling.id,
            GjenståendeSykedager(grunnlag, 42, 1 januar 2026)
        )

        behandlingHendelseService.stoppet(behandling, avklaringsbehoveneMedÉttÅpent())

        assertThat(oppgaveHendelse().gjenståendeSykedager).isEqualTo(
            GjenståendeSykedagerDto(
                GjenståendeSykedagerGrunnlagDto.valueOf(grunnlag.name), 42, 1 januar 2026, 2 mars 2026
            )
        )
    }

    @Test
    fun `sender ikke gjenstående sykedager til oppgave for revurdering`() {
        val (behandlingHendelseService, behandling) = settOppFlyt(TypeBehandling.Revurdering)
        InMemoryGjenståendeSykedagerRepository.lagre(
            behandling.id,
            GjenståendeSykedager(GjenståendeSykedagerGrunnlag.VEDTAK_I_NY_LØSNING, 42, 1 januar 2026)
        )

        behandlingHendelseService.stoppet(behandling, avklaringsbehoveneMedÉttÅpent())

        assertThat(oppgaveHendelse().gjenståendeSykedager).isNull()
    }

    @Test
    fun `sender null til oppgave når førstegangsbehandling mangler gjenstående sykedager`() {
        val (behandlingHendelseService, behandling) = settOppFlyt()

        behandlingHendelseService.stoppet(behandling, avklaringsbehoveneMedÉttÅpent())

        assertThat(oppgaveHendelse().gjenståendeSykedager).isNull()
    }

    @Test
    fun `sender kant i kant med maksdato og kravdato til oppgave når kravet dekker overgangen fra Arena`() {
        val (behandlingHendelseService, behandling) = settOppFlyt()
        lagreArenaMaksdato(behandling.id, MAKSDATO_FREDAG)
        lagreSøknadKrav(behandling.id, MANDAG_ETTER_MAKSDATO)

        behandlingHendelseService.stoppet(behandling, avklaringsbehoveneMedÉttÅpent())

        assertThat(oppgaveHendelse().nySakEtterOpphørArena).isEqualTo(
            NySakEtterOpphørArenaDto(
                erKantIKant = true,
                maksdato = MAKSDATO_FREDAG,
                kravdato = MANDAG_ETTER_MAKSDATO
            )
        )
    }

    @Test
    fun `sender ikke kant i kant til oppgave når det er opphold etter maksdato i Arena`() {
        val (behandlingHendelseService, behandling) = settOppFlyt()
        lagreArenaMaksdato(behandling.id, MAKSDATO_FREDAG)
        lagreSøknadKrav(behandling.id, MANDAG_ETTER_MAKSDATO.plusDays(1))

        behandlingHendelseService.stoppet(behandling, avklaringsbehoveneMedÉttÅpent())

        assertThat(oppgaveHendelse().nySakEtterOpphørArena).isEqualTo(
            NySakEtterOpphørArenaDto(
                erKantIKant = false,
                maksdato = MAKSDATO_FREDAG,
                kravdato = MANDAG_ETTER_MAKSDATO.plusDays(1)
            )
        )
    }

    @Test
    fun `bruker tidligste søknadskrav som kravdato når ingen krav er kant i kant`() {
        val (behandlingHendelseService, behandling) = settOppFlyt()
        lagreArenaMaksdato(behandling.id, MAKSDATO_FREDAG)
        lagreSøknadKrav(behandling.id, 1 februar 2026, 15 januar 2026)

        behandlingHendelseService.stoppet(behandling, avklaringsbehoveneMedÉttÅpent())

        assertThat(oppgaveHendelse().nySakEtterOpphørArena?.kravdato).isEqualTo(15 januar 2026)
    }

    @Test
    fun `sender ikke ny sak etter opphør til oppgave når Arena-saken mangler maksdato`() {
        val (behandlingHendelseService, behandling) = settOppFlyt()
        lagreArenaMaksdato(behandling.id, maksdato = null)
        lagreSøknadKrav(behandling.id, MANDAG_ETTER_MAKSDATO)

        behandlingHendelseService.stoppet(behandling, avklaringsbehoveneMedÉttÅpent())

        assertThat(oppgaveHendelse().nySakEtterOpphørArena).isNull()
    }

    @Test
    fun `sender ikke ny sak etter opphør til oppgave uten Arena-sak`() {
        val (behandlingHendelseService, behandling) = settOppFlyt()
        lagreSøknadKrav(behandling.id, MANDAG_ETTER_MAKSDATO)

        behandlingHendelseService.stoppet(behandling, avklaringsbehoveneMedÉttÅpent())

        assertThat(oppgaveHendelse().nySakEtterOpphørArena).isNull()
    }

    @Test
    fun `sender ikke ny sak etter opphør til oppgave uten søknadskrav`() {
        val (behandlingHendelseService, behandling) = settOppFlyt()
        lagreArenaMaksdato(behandling.id, MAKSDATO_FREDAG)

        behandlingHendelseService.stoppet(behandling, avklaringsbehoveneMedÉttÅpent())

        assertThat(oppgaveHendelse().nySakEtterOpphørArena).isNull()
    }

    @Test
    fun `sender ikke ny sak etter opphør til oppgave for revurdering`() {
        val (behandlingHendelseService, behandling) = settOppFlyt(TypeBehandling.Revurdering)
        lagreArenaMaksdato(behandling.id, MAKSDATO_FREDAG)
        lagreSøknadKrav(behandling.id, MANDAG_ETTER_MAKSDATO)

        behandlingHendelseService.stoppet(behandling, avklaringsbehoveneMedÉttÅpent())

        assertThat(oppgaveHendelse().nySakEtterOpphørArena).isNull()
    }

    @Test
    fun `skal ikke opprette oppgaveHendelse dersom avklaringsbehovene er tomme`() {
        // SETUP

        val (behandlingHendelseService, behandling) = settOppFlyt()

        val avklaringsbehovene = Avklaringsbehovene(InMemoryAvklaringsbehovRepository, nyBehandlingId())

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

        val avklaringsbehovene = avklaringsbehoveneMedÉttÅpent()

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

    private fun avklaringsbehoveneMedÉttÅpent(): Avklaringsbehovene {
        val avklaringsbehovene = Avklaringsbehovene(InMemoryAvklaringsbehovRepository, nyBehandlingId())
        avklaringsbehovene.leggTil(Definisjon.AVKLAR_LOVVALG_MEDLEMSKAP, StegType.VURDER_LOVVALG, null, null, null)
        return avklaringsbehovene
    }

    private fun nyBehandlingId() = BehandlingId(Random.nextLong(1_000_000, Long.MAX_VALUE))

    private fun lagreArenaMaksdato(behandlingId: BehandlingId, maksdato: LocalDate?) {
        InMemoryArenaMaksdatoRepository.lagre(
            behandlingId,
            ArenaMaksdato(
                arenaSaksnummer = "2023-5",
                vedtakStatus = ArenaVedtakStatus.AVSLUTTET,
                maksdato = maksdato,
                vedtakTil = maksdato,
                vedtakId = 1,
                harForlengelseEtter11_12 = false
            )
        )
    }

    private fun lagreSøknadKrav(behandlingId: BehandlingId, vararg muligRettFra: LocalDate) {
        InMemoryKravRepository.lagre(
            behandlingId,
            muligRettFra.map { dato ->
                RelevantSøknadKrav(
                    referanse = Kravreferanse.ny(),
                    journalpostId = JournalpostId("123"),
                    vurdertAv = SYSTEMBRUKER,
                    begrunnelse = "Test",
                    vurdertIBehandling = behandlingId,
                    opprettet = Instant.now(),
                    søknadsdato = Søknadsdato(dato, SøknadsdatoÅrsak.SøknadMottatt, begrunnelse = "Test"),
                    overstyrMuligRettFra = null,
                    muligRettFra = dato
                )
            }.toSet()
        )
    }

    private fun oppgaveHendelse(): BehandlingFlytStoppetHendelse {
        val calls = mutableListOf<JobbInput>()
        verify { flytJobbRepository.leggTil(capture(calls)) }
        val jobb = calls.first { it.type() == VarsleOppgaveOmHendelseJobbUtFører.type }
        return DefaultJsonMapper.fromJson<BehandlingFlytStoppetHendelse>(jobb.payload())
    }

    private fun settOppFlyt(
        typeBehandling: TypeBehandling = TypeBehandling.Førstegangsbehandling,
        faktiskBehandlingstype: TypeBehandling = typeBehandling,
    ): Pair<BehandlingHendelseServiceImpl, Behandling> {
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
                avklaringsbehovService,
                InMemoryGjenståendeSykedagerRepository,
                InMemoryKravRepository,
                InMemoryArenaMaksdatoRepository,
            )

        val behandling = Behandling(
            BehandlingId(0),
            sakId = SakId(1),
            typeBehandling = typeBehandling,
            årsakTilOpprettelse = ÅrsakTilOpprettelse.SØKNAD,
            forrigeBehandlingId = null,
            versjon = 1
        )

        every { behandlingService.utledFaktiskBehandlingstype(behandling) }.returns(faktiskBehandlingstype)

        every { sakService.hent(SakId(1)) } returns Sak(
            id = SakId(1),
            saksnummer = Saksnummer("1"),
            person = person(),
            rettighetsperiode = Periode(LocalDate.now(), LocalDate.now())
        )
        return Pair(behandlingHendelseService, behandling)
    }

    private companion object {
        val MAKSDATO_FREDAG: LocalDate = 2 januar 2026
        val MANDAG_ETTER_MAKSDATO: LocalDate = 5 januar 2026
    }
}