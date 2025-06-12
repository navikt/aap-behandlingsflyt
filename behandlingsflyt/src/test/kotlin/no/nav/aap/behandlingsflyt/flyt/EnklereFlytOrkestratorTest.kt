package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.flyt.steg.StegOrkestrator
import no.nav.aap.behandlingsflyt.flyt.testutil.DummyBehandlingHendelseService
import no.nav.aap.behandlingsflyt.flyt.testutil.DummyInformasjonskravGrunnlag
import no.nav.aap.behandlingsflyt.flyt.testutil.DummyStegKonstruktør
import no.nav.aap.behandlingsflyt.flyt.testutil.DummyVentebehovEvaluererService
import no.nav.aap.behandlingsflyt.forretningsflyt.behandlingstyper.Førstegangsbehandling
import no.nav.aap.behandlingsflyt.hendelse.avløp.BehandlingHendelseService
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.periodisering.FlytKontekstMedPeriodeService
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.StegTilstand
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.StegStatus
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.behandlingsflyt.test.FakeUnleash
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryAvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryBehandlingRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemorySakRepository
import no.nav.aap.behandlingsflyt.test.inmemoryservice.InMemorySakOgBehandlingService
import no.nav.aap.behandlingsflyt.test.modell.genererIdent
import no.nav.aap.komponenter.httpklient.auth.Bruker
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

class EnklereFlytOrkestratorTest {

    private val sakRepository = InMemorySakRepository
    private val sakService = SakService(sakRepository)
    private val behandlingRepository = InMemoryBehandlingRepository
    private val avklaringsbehovRepository = InMemoryAvklaringsbehovRepository

    private val flytOrkestrator = FlytOrkestrator(
        flytKontekstMedPeriodeService = FlytKontekstMedPeriodeService(
            sakService = sakService,
            behandlingRepository = behandlingRepository,
            unleashGateway = FakeUnleash,
        ),
        sakOgBehandlingService = InMemorySakOgBehandlingService,
        informasjonskravGrunnlag = DummyInformasjonskravGrunnlag(),
        sakRepository = sakRepository,
        avklaringsbehovRepository = avklaringsbehovRepository,
        behandlingRepository = behandlingRepository,
        ventebehovEvaluererService = DummyVentebehovEvaluererService(),
        behandlingHendelseService = DummyBehandlingHendelseService,
        stegOrkestrator = StegOrkestrator(
            informasjonskravGrunnlag = DummyInformasjonskravGrunnlag(),
            behandlingRepository = behandlingRepository,
            avklaringsbehovRepository = avklaringsbehovRepository,
            stegKonstruktør = DummyStegKonstruktør(),
        )
    )

    @Test
    fun `skal gå gjennom alle stegene definert i beandlings`() {
        val person = Person(1, UUID.randomUUID(), listOf(genererIdent(LocalDate.now().minusYears(23))))

        val sak = sakRepository.finnEllerOpprett(person, Periode(LocalDate.now(), LocalDate.now().plusYears(1)))
        val behandling =
            behandlingRepository.opprettBehandling(sak.id, listOf(), TypeBehandling.Førstegangsbehandling, null)

        val flytKontekst = flytOrkestrator.opprettKontekst(behandling.sakId, behandling.id)

        flytOrkestrator.forberedOgProsesserBehandling(flytKontekst)

        assertThat(behandlingRepository.hentStegHistorikk(behandling.id)).isNotEmpty()
        assertThat(behandlingRepository.hentStegHistorikk(behandling.id).map { tilstand -> tilstand.steg() }
            .distinct()).containsExactlyElementsOf(
            Førstegangsbehandling.flyt().stegene()
        )

        assertThat(behandling.status()).isEqualTo(Status.AVSLUTTET)
    }

    @Test
    fun `hendelse blir avgitt ved en automatisk lukket behandling`() {
        val person = Person(1, UUID.randomUUID(), listOf(genererIdent(LocalDate.now().minusYears(23))))
        val sak = sakRepository.finnEllerOpprett(person, Periode(LocalDate.now(), LocalDate.now().plusYears(1)))
        val behandling =
            behandlingRepository.opprettBehandling(sak.id, listOf(), TypeBehandling.Førstegangsbehandling, null)

        val behandlingHendelseService = object : BehandlingHendelseService {
            val hendelser = mutableListOf<Pair<Behandling, Avklaringsbehovene>>()
            override fun stoppet(behandling: Behandling, avklaringsbehovene: Avklaringsbehovene) {
                hendelser.add(Pair(behandling, avklaringsbehovene))
            }
        }
        val flytOrkestrator = FlytOrkestrator(
            flytKontekstMedPeriodeService = FlytKontekstMedPeriodeService(
                sakService = sakService,
                behandlingRepository = behandlingRepository,
                unleashGateway = FakeUnleash,
            ),
            informasjonskravGrunnlag = DummyInformasjonskravGrunnlag(),
            behandlingRepository = behandlingRepository,
            ventebehovEvaluererService = DummyVentebehovEvaluererService(),
            sakRepository = sakRepository,
            avklaringsbehovRepository = avklaringsbehovRepository,
            behandlingHendelseService = behandlingHendelseService,
            sakOgBehandlingService = InMemorySakOgBehandlingService,
            stegOrkestrator = StegOrkestrator(
                informasjonskravGrunnlag = DummyInformasjonskravGrunnlag(),
                behandlingRepository = behandlingRepository,
                avklaringsbehovRepository = avklaringsbehovRepository,
                stegKonstruktør = DummyStegKonstruktør(),
            )
        )

        val flytKontekst = flytOrkestrator.opprettKontekst(behandling.sakId, behandling.id)

        flytOrkestrator.forberedOgProsesserBehandling(flytKontekst)

        assertThat(behandlingHendelseService.hendelser).hasSize(1)
        assertThat(behandlingHendelseService.hendelser.first().first.status()).isEqualTo(Status.AVSLUTTET)
    }

    @Test
    fun `skal ikke kunne gå forbi et åpent avklaringsbehov`() {
        val person = Person(1, UUID.randomUUID(), listOf(genererIdent(LocalDate.now().minusYears(23))))

        val sak = sakRepository.finnEllerOpprett(person, Periode(LocalDate.now(), LocalDate.now().plusYears(1)))
        val behandling =
            behandlingRepository.opprettBehandling(sak.id, listOf(), TypeBehandling.Førstegangsbehandling, null)
        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(behandling.id)
        avklaringsbehovene.leggTil(
            definisjoner = listOf(Definisjon.AVKLAR_SYKDOM), funnetISteg = StegType
                .AVKLAR_SYKDOM
        )

        val flytKontekst = flytOrkestrator.opprettKontekst(behandling.sakId, behandling.id)
        flytOrkestrator.forberedOgProsesserBehandling(flytKontekst)

        assertThat(behandling.status()).isEqualTo(Status.UTREDES)
        assertThat(behandling.aktivtSteg()).isEqualTo(StegType.AVKLAR_SYKDOM)
        assertThat(behandlingRepository.hentStegHistorikk(behandling.id)).isNotEmpty()
        assertThat(behandlingRepository.hentStegHistorikk(behandling.id).map { tilstand -> tilstand.steg() }
            .distinct()).containsExactlyElementsOf(
            listOf(
                StegType.START_BEHANDLING,
                StegType.SEND_FORVALTNINGSMELDING,
                StegType.SØKNAD,
                StegType.VURDER_RETTIGHETSPERIODE,
                StegType.VURDER_LOVVALG,
                StegType.FASTSETT_MELDEPERIODER,
                StegType.VURDER_ALDER,
                StegType.AVKLAR_STUDENT,
                StegType.AVKLAR_SYKDOM
            )
        )

        val flytKontekst2 = flytOrkestrator.opprettKontekst(behandling.sakId, behandling.id)
        flytOrkestrator.forberedOgProsesserBehandling(flytKontekst)


        assertThat(behandling.status()).isEqualTo(Status.UTREDES)
        assertThat(behandling.aktivtSteg()).isEqualTo(StegType.AVKLAR_SYKDOM)
        assertThat(behandlingRepository.hentStegHistorikk(behandling.id)).isNotEmpty()
        assertThat(behandlingRepository.hentStegHistorikk(behandling.id).map { tilstand -> tilstand.steg() }
            .distinct()).containsExactlyElementsOf(
            listOf(
                StegType.START_BEHANDLING,
                StegType.SEND_FORVALTNINGSMELDING,
                StegType.SØKNAD,
                StegType.VURDER_RETTIGHETSPERIODE,
                StegType.VURDER_LOVVALG,
                StegType.FASTSETT_MELDEPERIODER,
                StegType.VURDER_ALDER,
                StegType.AVKLAR_STUDENT,
                StegType.AVKLAR_SYKDOM
            )
        )

        val flytKontekst3 = flytOrkestrator.opprettKontekst(behandling.sakId, behandling.id)
        flytOrkestrator.forberedLøsingAvBehov(
            behovDefinisjon = Definisjon.AVKLAR_SYKDOM,
            behandling = behandling,
            kontekst = flytKontekst3,
            bruker = Bruker("Z123456")
        )
        avklaringsbehovene.løsAvklaringsbehov(Definisjon.AVKLAR_SYKDOM, "asdf", "TESTEN")

        flytOrkestrator.forberedOgProsesserBehandling(flytKontekst3)

        assertThat(behandlingRepository.hentStegHistorikk(behandling.id)).isNotEmpty()
        assertThat(behandlingRepository.hentStegHistorikk(behandling.id).map { tilstand -> tilstand.steg() }
            .distinct()).containsExactlyElementsOf(
            Førstegangsbehandling.flyt().stegene()
        )

        assertThat(behandling.status()).isEqualTo(Status.AVSLUTTET)
    }

    @Test
    fun `skal hoppe tilbake til steget behovet finnes i når det løses i UTFØRT status og står i senere steg`() {
        val person = Person(1, UUID.randomUUID(), listOf(genererIdent(LocalDate.now().minusYears(23))))

        val sak = sakRepository.finnEllerOpprett(person, Periode(LocalDate.now(), LocalDate.now().plusYears(1)))
        val behandling =
            behandlingRepository.opprettBehandling(sak.id, listOf(), TypeBehandling.Førstegangsbehandling, null)
        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(behandling.id)
        avklaringsbehovene.leggTil(
            definisjoner = listOf(Definisjon.AVKLAR_STUDENT), funnetISteg = StegType
                .AVKLAR_STUDENT
        )
        avklaringsbehovene.løsAvklaringsbehov(Definisjon.AVKLAR_STUDENT, "asdf", "TESTEN")
        avklaringsbehovene.leggTil(
            definisjoner = listOf(Definisjon.AVKLAR_SYKDOM), funnetISteg = StegType
                .AVKLAR_SYKDOM
        )

        val flytKontekst = flytOrkestrator.opprettKontekst(behandling.sakId, behandling.id)
        flytOrkestrator.forberedOgProsesserBehandling(flytKontekst)

        assertThat(behandling.status()).isEqualTo(Status.UTREDES)
        assertThat(behandling.aktivtSteg()).isEqualTo(StegType.AVKLAR_SYKDOM)
        assertThat(behandlingRepository.hentStegHistorikk(behandling.id)).isNotEmpty()
        assertThat(behandlingRepository.hentStegHistorikk(behandling.id)).containsExactlyElementsOf(
            listOf(
                StegTilstand(stegType = StegType.START_BEHANDLING, stegStatus = StegStatus.START, aktiv = false),
                StegTilstand(
                    stegType = StegType.START_BEHANDLING,
                    stegStatus = StegStatus.OPPDATER_FAKTAGRUNNLAG,
                    aktiv = false
                ),
                StegTilstand(stegType = StegType.START_BEHANDLING, stegStatus = StegStatus.UTFØRER, aktiv = false),
                StegTilstand(
                    stegType = StegType.START_BEHANDLING,
                    stegStatus = StegStatus.AVKLARINGSPUNKT,
                    aktiv = false
                ),
                StegTilstand(stegType = StegType.START_BEHANDLING, stegStatus = StegStatus.AVSLUTTER, aktiv = false),
                StegTilstand(stegType = StegType.SEND_FORVALTNINGSMELDING, stegStatus = StegStatus.START, aktiv = false),
                StegTilstand(stegType = StegType.SEND_FORVALTNINGSMELDING, stegStatus = StegStatus.OPPDATER_FAKTAGRUNNLAG, aktiv = false),
                StegTilstand(stegType = StegType.SEND_FORVALTNINGSMELDING, stegStatus = StegStatus.UTFØRER, aktiv = false),
                StegTilstand(stegType = StegType.SEND_FORVALTNINGSMELDING, stegStatus = StegStatus.AVKLARINGSPUNKT, aktiv = false),
                StegTilstand(stegType = StegType.SEND_FORVALTNINGSMELDING, stegStatus = StegStatus.AVSLUTTER, aktiv = false),
                StegTilstand(stegType = StegType.SØKNAD, stegStatus = StegStatus.START, aktiv = false),
                StegTilstand(stegType = StegType.SØKNAD, stegStatus = StegStatus.OPPDATER_FAKTAGRUNNLAG, aktiv = false),
                StegTilstand(stegType = StegType.SØKNAD, stegStatus = StegStatus.UTFØRER, aktiv = false),
                StegTilstand(stegType = StegType.SØKNAD, stegStatus = StegStatus.AVKLARINGSPUNKT, aktiv = false),
                StegTilstand(stegType = StegType.SØKNAD, stegStatus = StegStatus.AVSLUTTER, aktiv = false),
                StegTilstand(stegType = StegType.VURDER_RETTIGHETSPERIODE, stegStatus = StegStatus.START, aktiv = false),
                StegTilstand(
                    stegType = StegType.VURDER_RETTIGHETSPERIODE,
                    stegStatus = StegStatus.OPPDATER_FAKTAGRUNNLAG,
                    aktiv = false
                ),
                StegTilstand(stegType = StegType.VURDER_RETTIGHETSPERIODE, stegStatus = StegStatus.UTFØRER, aktiv = false),
                StegTilstand(
                    stegType = StegType.VURDER_RETTIGHETSPERIODE,
                    stegStatus = StegStatus.AVKLARINGSPUNKT,
                    aktiv = false
                ),
                StegTilstand(stegType = StegType.VURDER_RETTIGHETSPERIODE, stegStatus = StegStatus.AVSLUTTER, aktiv = false),
                StegTilstand(stegType = StegType.VURDER_LOVVALG, stegStatus = StegStatus.START, aktiv = false),
                StegTilstand(
                    stegType = StegType.VURDER_LOVVALG,
                    stegStatus = StegStatus.OPPDATER_FAKTAGRUNNLAG,
                    aktiv = false
                ),
                StegTilstand(stegType = StegType.VURDER_LOVVALG, stegStatus = StegStatus.UTFØRER, aktiv = false),
                StegTilstand(
                    stegType = StegType.VURDER_LOVVALG,
                    stegStatus = StegStatus.AVKLARINGSPUNKT,
                    aktiv = false
                ),
                StegTilstand(stegType = StegType.VURDER_LOVVALG, stegStatus = StegStatus.AVSLUTTER, aktiv = false),
                StegTilstand(stegType = StegType.FASTSETT_MELDEPERIODER, stegStatus = StegStatus.START, aktiv = false),
                StegTilstand(
                    stegType = StegType.FASTSETT_MELDEPERIODER,
                    stegStatus = StegStatus.OPPDATER_FAKTAGRUNNLAG,
                    aktiv = false
                ),
                StegTilstand(
                    stegType = StegType.FASTSETT_MELDEPERIODER,
                    stegStatus = StegStatus.UTFØRER,
                    aktiv = false
                ),
                StegTilstand(
                    stegType = StegType.FASTSETT_MELDEPERIODER,
                    stegStatus = StegStatus.AVKLARINGSPUNKT,
                    aktiv = false
                ),
                StegTilstand(
                    stegType = StegType.FASTSETT_MELDEPERIODER,
                    stegStatus = StegStatus.AVSLUTTER,
                    aktiv = false
                ),
                StegTilstand(stegType = StegType.VURDER_ALDER, stegStatus = StegStatus.START, aktiv = false),
                StegTilstand(
                    stegType = StegType.VURDER_ALDER,
                    stegStatus = StegStatus.OPPDATER_FAKTAGRUNNLAG,
                    aktiv = false
                ),
                StegTilstand(stegType = StegType.VURDER_ALDER, stegStatus = StegStatus.UTFØRER, aktiv = false),
                StegTilstand(stegType = StegType.VURDER_ALDER, stegStatus = StegStatus.AVKLARINGSPUNKT, aktiv = false),
                StegTilstand(stegType = StegType.VURDER_ALDER, stegStatus = StegStatus.AVSLUTTER, aktiv = false),
                StegTilstand(stegType = StegType.AVKLAR_STUDENT, stegStatus = StegStatus.START, aktiv = false),
                StegTilstand(
                    stegType = StegType.AVKLAR_STUDENT,
                    stegStatus = StegStatus.OPPDATER_FAKTAGRUNNLAG,
                    aktiv = false
                ),
                StegTilstand(stegType = StegType.AVKLAR_STUDENT, stegStatus = StegStatus.UTFØRER, aktiv = false),
                StegTilstand(
                    stegType = StegType.AVKLAR_STUDENT,
                    stegStatus = StegStatus.AVKLARINGSPUNKT,
                    aktiv = false
                ),
                StegTilstand(stegType = StegType.AVKLAR_STUDENT, stegStatus = StegStatus.AVSLUTTER, aktiv = false),
                StegTilstand(stegType = StegType.AVKLAR_SYKDOM, stegStatus = StegStatus.START, aktiv = false),
                StegTilstand(
                    stegType = StegType.AVKLAR_SYKDOM,
                    stegStatus = StegStatus.OPPDATER_FAKTAGRUNNLAG,
                    aktiv = false
                ),
                StegTilstand(stegType = StegType.AVKLAR_SYKDOM, stegStatus = StegStatus.UTFØRER, aktiv = false),
                StegTilstand(stegType = StegType.AVKLAR_SYKDOM, stegStatus = StegStatus.AVKLARINGSPUNKT, aktiv = true),
            )
        )

        val flytKontekst3 = flytOrkestrator.opprettKontekst(behandling.sakId, behandling.id)
        flytOrkestrator.forberedLøsingAvBehov(
            behovDefinisjon = Definisjon.AVKLAR_STUDENT,
            behandling = behandling,
            kontekst = flytKontekst3,
            bruker = Bruker("Z123456")
        )
        avklaringsbehovene.løsAvklaringsbehov(Definisjon.AVKLAR_STUDENT, "asdf", "TESTEN")

        flytOrkestrator.forberedOgProsesserBehandling(flytKontekst3)

        assertThat(behandlingRepository.hentStegHistorikk(behandling.id)).isNotEmpty()
        assertThat(behandlingRepository.hentStegHistorikk(behandling.id)).containsExactlyElementsOf(
            listOf(
                StegTilstand(stegType = StegType.START_BEHANDLING, stegStatus = StegStatus.START, aktiv = false),
                StegTilstand(
                    stegType = StegType.START_BEHANDLING,
                    stegStatus = StegStatus.OPPDATER_FAKTAGRUNNLAG,
                    aktiv = false
                ),
                StegTilstand(stegType = StegType.START_BEHANDLING, stegStatus = StegStatus.UTFØRER, aktiv = false),
                StegTilstand(
                    stegType = StegType.START_BEHANDLING,
                    stegStatus = StegStatus.AVKLARINGSPUNKT,
                    aktiv = false
                ),
                StegTilstand(stegType = StegType.START_BEHANDLING, stegStatus = StegStatus.AVSLUTTER, aktiv = false),
                StegTilstand(stegType = StegType.SEND_FORVALTNINGSMELDING, stegStatus = StegStatus.START, aktiv = false),
                StegTilstand(stegType = StegType.SEND_FORVALTNINGSMELDING, stegStatus = StegStatus.OPPDATER_FAKTAGRUNNLAG, aktiv = false),
                StegTilstand(stegType = StegType.SEND_FORVALTNINGSMELDING, stegStatus = StegStatus.UTFØRER, aktiv = false),
                StegTilstand(stegType = StegType.SEND_FORVALTNINGSMELDING, stegStatus = StegStatus.AVKLARINGSPUNKT, aktiv = false),
                StegTilstand(stegType = StegType.SEND_FORVALTNINGSMELDING, stegStatus = StegStatus.AVSLUTTER, aktiv = false),
                StegTilstand(stegType = StegType.SØKNAD, stegStatus = StegStatus.START, aktiv = false),
                StegTilstand(stegType = StegType.SØKNAD, stegStatus = StegStatus.OPPDATER_FAKTAGRUNNLAG, aktiv = false),
                StegTilstand(stegType = StegType.SØKNAD, stegStatus = StegStatus.UTFØRER, aktiv = false),
                StegTilstand(stegType = StegType.SØKNAD, stegStatus = StegStatus.AVKLARINGSPUNKT, aktiv = false),
                StegTilstand(stegType = StegType.SØKNAD, stegStatus = StegStatus.AVSLUTTER, aktiv = false),
                StegTilstand(stegType = StegType.VURDER_RETTIGHETSPERIODE, stegStatus = StegStatus.START, aktiv = false),
                StegTilstand(
                    stegType = StegType.VURDER_RETTIGHETSPERIODE,
                    stegStatus = StegStatus.OPPDATER_FAKTAGRUNNLAG,
                    aktiv = false
                ),
                StegTilstand(stegType = StegType.VURDER_RETTIGHETSPERIODE, stegStatus = StegStatus.UTFØRER, aktiv = false),
                StegTilstand(
                    stegType = StegType.VURDER_RETTIGHETSPERIODE,
                    stegStatus = StegStatus.AVKLARINGSPUNKT,
                    aktiv = false
                ),
                StegTilstand(stegType = StegType.VURDER_RETTIGHETSPERIODE, stegStatus = StegStatus.AVSLUTTER, aktiv = false),
                StegTilstand(stegType = StegType.VURDER_LOVVALG, stegStatus = StegStatus.START, aktiv = false),
                StegTilstand(
                    stegType = StegType.VURDER_LOVVALG,
                    stegStatus = StegStatus.OPPDATER_FAKTAGRUNNLAG,
                    aktiv = false
                ),
                StegTilstand(stegType = StegType.VURDER_LOVVALG, stegStatus = StegStatus.UTFØRER, aktiv = false),
                StegTilstand(
                    stegType = StegType.VURDER_LOVVALG,
                    stegStatus = StegStatus.AVKLARINGSPUNKT,
                    aktiv = false
                ),
                StegTilstand(stegType = StegType.VURDER_LOVVALG, stegStatus = StegStatus.AVSLUTTER, aktiv = false),
                StegTilstand(stegType = StegType.FASTSETT_MELDEPERIODER, stegStatus = StegStatus.START, aktiv = false),
                StegTilstand(
                    stegType = StegType.FASTSETT_MELDEPERIODER,
                    stegStatus = StegStatus.OPPDATER_FAKTAGRUNNLAG,
                    aktiv = false
                ),
                StegTilstand(
                    stegType = StegType.FASTSETT_MELDEPERIODER,
                    stegStatus = StegStatus.UTFØRER,
                    aktiv = false
                ),
                StegTilstand(
                    stegType = StegType.FASTSETT_MELDEPERIODER,
                    stegStatus = StegStatus.AVKLARINGSPUNKT,
                    aktiv = false
                ),
                StegTilstand(
                    stegType = StegType.FASTSETT_MELDEPERIODER,
                    stegStatus = StegStatus.AVSLUTTER,
                    aktiv = false
                ),
                StegTilstand(stegType = StegType.VURDER_ALDER, stegStatus = StegStatus.START, aktiv = false),
                StegTilstand(
                    stegType = StegType.VURDER_ALDER,
                    stegStatus = StegStatus.OPPDATER_FAKTAGRUNNLAG,
                    aktiv = false
                ),
                StegTilstand(stegType = StegType.VURDER_ALDER, stegStatus = StegStatus.UTFØRER, aktiv = false),
                StegTilstand(stegType = StegType.VURDER_ALDER, stegStatus = StegStatus.AVKLARINGSPUNKT, aktiv = false),
                StegTilstand(stegType = StegType.VURDER_ALDER, stegStatus = StegStatus.AVSLUTTER, aktiv = false),
                StegTilstand(stegType = StegType.AVKLAR_STUDENT, stegStatus = StegStatus.START, aktiv = false),
                StegTilstand(
                    stegType = StegType.AVKLAR_STUDENT,
                    stegStatus = StegStatus.OPPDATER_FAKTAGRUNNLAG,
                    aktiv = false
                ),
                StegTilstand(stegType = StegType.AVKLAR_STUDENT, stegStatus = StegStatus.UTFØRER, aktiv = false),
                StegTilstand(
                    stegType = StegType.AVKLAR_STUDENT,
                    stegStatus = StegStatus.AVKLARINGSPUNKT,
                    aktiv = false
                ),
                StegTilstand(stegType = StegType.AVKLAR_STUDENT, stegStatus = StegStatus.AVSLUTTER, aktiv = false),
                StegTilstand(stegType = StegType.AVKLAR_SYKDOM, stegStatus = StegStatus.START, aktiv = false),
                StegTilstand(
                    stegType = StegType.AVKLAR_SYKDOM,
                    stegStatus = StegStatus.OPPDATER_FAKTAGRUNNLAG,
                    aktiv = false
                ),
                StegTilstand(stegType = StegType.AVKLAR_SYKDOM, stegStatus = StegStatus.UTFØRER, aktiv = false),
                StegTilstand(stegType = StegType.AVKLAR_SYKDOM, stegStatus = StegStatus.AVKLARINGSPUNKT, aktiv = false),
                StegTilstand(stegType = StegType.AVKLAR_SYKDOM, stegStatus = StegStatus.TILBAKEFØRT, aktiv = false),
                StegTilstand(stegType = StegType.AVKLAR_STUDENT, stegStatus = StegStatus.TILBAKEFØRT, aktiv = false),
                StegTilstand(stegType = StegType.AVKLAR_STUDENT, stegStatus = StegStatus.START, aktiv = false),
                StegTilstand(
                    stegType = StegType.AVKLAR_STUDENT,
                    stegStatus = StegStatus.OPPDATER_FAKTAGRUNNLAG,
                    aktiv = false
                ),
                StegTilstand(stegType = StegType.AVKLAR_STUDENT, stegStatus = StegStatus.UTFØRER, aktiv = false),
                StegTilstand(
                    stegType = StegType.AVKLAR_STUDENT,
                    stegStatus = StegStatus.AVKLARINGSPUNKT,
                    aktiv = false
                ),
                StegTilstand(stegType = StegType.AVKLAR_STUDENT, stegStatus = StegStatus.AVSLUTTER, aktiv = false),
                StegTilstand(stegType = StegType.AVKLAR_SYKDOM, stegStatus = StegStatus.START, aktiv = false),
                StegTilstand(
                    stegType = StegType.AVKLAR_SYKDOM,
                    stegStatus = StegStatus.OPPDATER_FAKTAGRUNNLAG,
                    aktiv = false
                ),
                StegTilstand(stegType = StegType.AVKLAR_SYKDOM, stegStatus = StegStatus.UTFØRER, aktiv = false),
                StegTilstand(stegType = StegType.AVKLAR_SYKDOM, stegStatus = StegStatus.AVKLARINGSPUNKT, aktiv = true),
            )
        )
        assertThat(behandling.status()).isEqualTo(Status.UTREDES)
    }
}