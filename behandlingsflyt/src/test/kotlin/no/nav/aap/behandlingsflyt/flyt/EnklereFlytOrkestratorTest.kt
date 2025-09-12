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
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType.AVKLAR_STUDENT
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType.AVKLAR_SYKDOM
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType.FASTSETT_MELDEPERIODER
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType.FATTE_VEDTAK
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType.IVERKSETT_VEDTAK
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType.KANSELLER_REVURDERING
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType.OPPRETT_REVURDERING
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType.SEND_FORVALTNINGSMELDING
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType.START_BEHANDLING
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType.SØKNAD
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType.VURDER_ALDER
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType.VURDER_LOVVALG
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType.VURDER_RETTIGHETSPERIODE
import no.nav.aap.behandlingsflyt.periodisering.FlytKontekstMedPeriodeService
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.StegTilstand
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.StegStatus
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryAvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryBehandlingRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemorySakRepository
import no.nav.aap.behandlingsflyt.test.inmemoryservice.InMemorySakOgBehandlingService
import no.nav.aap.behandlingsflyt.test.modell.genererIdent
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Bruker
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

    private val stopperTidligereFlytOrkestrator = FlytOrkestrator(
        flytKontekstMedPeriodeService = FlytKontekstMedPeriodeService(
            sakService = sakService,
            behandlingRepository = behandlingRepository,
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
        ),
        stoppNårStatus = setOf(Status.IVERKSETTES),
    )

    @Test
    fun `starter og stopper behandling mellom fatte vedtak og iverksett vedtak`() {
        val person = Person(UUID.randomUUID(), listOf(genererIdent(LocalDate.now().minusYears(23))))

        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(1))
        val sak = sakRepository.finnEllerOpprett(person, periode)
        val behandling = behandlingRepository.opprettBehandling(
            sakId = sak.id,
            vurderingsbehovOgÅrsak = VurderingsbehovOgÅrsak(
                vurderingsbehov = listOf(
                    VurderingsbehovMedPeriode(
                        type = Vurderingsbehov.MOTTATT_MELDEKORT,
                        periode = periode,
                    )
                ),
                årsak = ÅrsakTilOpprettelse.MELDEKORT
            ),
            typeBehandling = TypeBehandling.Førstegangsbehandling,
            forrigeBehandlingId = null
        )

        stopperTidligereFlytOrkestrator.opprettKontekst(behandling.sakId, behandling.id).also {
            stopperTidligereFlytOrkestrator.forberedOgProsesserBehandling(it)
        }

        assertThat(behandling.aktivtSteg()).isEqualTo(FATTE_VEDTAK)
        assertThat(behandling.status()).isEqualTo(Status.IVERKSETTES)
        behandlingRepository.hent(behandling.id).also {
            assertThat(it.aktivtSteg()).isEqualTo(FATTE_VEDTAK)
            assertThat(it.status()).isEqualTo(Status.IVERKSETTES)
        }

        assertThat(
            behandlingRepository.hentStegHistorikk(behandling.id)
                .filter {
                    it.steg() in listOf(START_BEHANDLING, FATTE_VEDTAK, IVERKSETT_VEDTAK, OPPRETT_REVURDERING)
                }
                .filter { it.status() in listOf(StegStatus.START, StegStatus.AVSLUTTER) }
        ).containsExactlyElementsOf(
            listOf(
                StegTilstand(stegType = START_BEHANDLING, stegStatus = StegStatus.START, aktiv = false),
                StegTilstand(stegType = START_BEHANDLING, stegStatus = StegStatus.AVSLUTTER, aktiv = false),
                StegTilstand(stegType = FATTE_VEDTAK, stegStatus = StegStatus.START, aktiv = false),
                StegTilstand(stegType = FATTE_VEDTAK, stegStatus = StegStatus.AVSLUTTER, aktiv = true),
            )
        )


        flytOrkestrator.opprettKontekst(behandling.sakId, behandling.id).also {
            flytOrkestrator.forberedOgProsesserBehandling(it)
        }

        assertThat(behandling.aktivtSteg()).isEqualTo(OPPRETT_REVURDERING)
        assertThat(behandling.status()).isEqualTo(Status.AVSLUTTET)
        behandlingRepository.hent(behandling.id).also {
            assertThat(it.aktivtSteg()).isEqualTo(OPPRETT_REVURDERING)
            assertThat(it.status()).isEqualTo(Status.AVSLUTTET)
        }

        assertThat(
            behandlingRepository.hentStegHistorikk(behandling.id)
                .filter {
                    it.steg() in listOf(START_BEHANDLING, FATTE_VEDTAK, IVERKSETT_VEDTAK, OPPRETT_REVURDERING)
                }
                .filter { it.status() in listOf(StegStatus.START, StegStatus.AVSLUTTER) }
        ).containsExactlyElementsOf(
            listOf(
                StegTilstand(stegType = START_BEHANDLING, stegStatus = StegStatus.START, aktiv = false),
                StegTilstand(stegType = START_BEHANDLING, stegStatus = StegStatus.AVSLUTTER, aktiv = false),
                StegTilstand(stegType = FATTE_VEDTAK, stegStatus = StegStatus.START, aktiv = false),
                StegTilstand(stegType = FATTE_VEDTAK, stegStatus = StegStatus.AVSLUTTER, aktiv = false),
                StegTilstand(stegType = IVERKSETT_VEDTAK, stegStatus = StegStatus.START, aktiv = false),
                StegTilstand(stegType = IVERKSETT_VEDTAK, stegStatus = StegStatus.AVSLUTTER, aktiv = false),
                StegTilstand(stegType = OPPRETT_REVURDERING, stegStatus = StegStatus.START, aktiv = false),
                StegTilstand(stegType = OPPRETT_REVURDERING, stegStatus = StegStatus.AVSLUTTER, aktiv = true),
            )
        )
    }

    @Test
    fun `skal gå gjennom alle stegene definert i beandlings`() {
        val person = Person(UUID.randomUUID(), listOf(genererIdent(LocalDate.now().minusYears(23))))

        val sak = sakRepository.finnEllerOpprett(person, Periode(LocalDate.now(), LocalDate.now().plusYears(1)))
        val behandling =
            behandlingRepository.opprettBehandling(
                sakId = sak.id,
                typeBehandling = TypeBehandling.Førstegangsbehandling,
                forrigeBehandlingId = null,
                vurderingsbehovOgÅrsak = VurderingsbehovOgÅrsak(
                    vurderingsbehov = listOf(VurderingsbehovMedPeriode(Vurderingsbehov.MOTTATT_SØKNAD)),
                    årsak = ÅrsakTilOpprettelse.SØKNAD
                )
            )

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
        val person = Person(UUID.randomUUID(), listOf(genererIdent(LocalDate.now().minusYears(23))))
        val sak = sakRepository.finnEllerOpprett(person, Periode(LocalDate.now(), LocalDate.now().plusYears(1)))
        val behandling =
            behandlingRepository.opprettBehandling(
                sakId = sak.id,
                typeBehandling = TypeBehandling.Førstegangsbehandling,
                forrigeBehandlingId = null,
                vurderingsbehovOgÅrsak = VurderingsbehovOgÅrsak(
                    vurderingsbehov = listOf(VurderingsbehovMedPeriode(Vurderingsbehov.MOTTATT_SØKNAD)),
                    årsak = ÅrsakTilOpprettelse.SØKNAD
                )
            )

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
        val person = Person(UUID.randomUUID(), listOf(genererIdent(LocalDate.now().minusYears(23))))

        val sak = sakRepository.finnEllerOpprett(person, Periode(LocalDate.now(), LocalDate.now().plusYears(1)))
        val behandling =
            behandlingRepository.opprettBehandling(
                sakId = sak.id,
                typeBehandling = TypeBehandling.Førstegangsbehandling,
                forrigeBehandlingId = null,
                vurderingsbehovOgÅrsak = VurderingsbehovOgÅrsak(
                    vurderingsbehov = listOf(VurderingsbehovMedPeriode(Vurderingsbehov.MOTTATT_SØKNAD)),
                    årsak = ÅrsakTilOpprettelse.SØKNAD
                )
            )
        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(behandling.id)
        avklaringsbehovene.leggTil(
            definisjoner = listOf(Definisjon.AVKLAR_SYKDOM), funnetISteg = AVKLAR_SYKDOM
        )

        val flytKontekst = flytOrkestrator.opprettKontekst(behandling.sakId, behandling.id)
        flytOrkestrator.forberedOgProsesserBehandling(flytKontekst)

        assertThat(behandling.status()).isEqualTo(Status.UTREDES)
        assertThat(behandling.aktivtSteg()).isEqualTo(AVKLAR_SYKDOM)
        assertThat(behandlingRepository.hentStegHistorikk(behandling.id)).isNotEmpty()
        assertThat(behandlingRepository.hentStegHistorikk(behandling.id).map { tilstand -> tilstand.steg() }
            .distinct()).containsExactlyElementsOf(
            listOf(
                START_BEHANDLING,
                SEND_FORVALTNINGSMELDING,
                KANSELLER_REVURDERING,
                SØKNAD,
                VURDER_RETTIGHETSPERIODE,
                VURDER_LOVVALG,
                FASTSETT_MELDEPERIODER,
                VURDER_ALDER,
                AVKLAR_STUDENT,
                AVKLAR_SYKDOM
            )
        )

        val flytKontekst2 = flytOrkestrator.opprettKontekst(behandling.sakId, behandling.id)
        flytOrkestrator.forberedOgProsesserBehandling(flytKontekst)


        assertThat(behandling.status()).isEqualTo(Status.UTREDES)
        assertThat(behandling.aktivtSteg()).isEqualTo(AVKLAR_SYKDOM)
        assertThat(behandlingRepository.hentStegHistorikk(behandling.id)).isNotEmpty()
        assertThat(behandlingRepository.hentStegHistorikk(behandling.id).map { tilstand -> tilstand.steg() }
            .distinct()).containsExactlyElementsOf(
            listOf(
                START_BEHANDLING,
                SEND_FORVALTNINGSMELDING,
                KANSELLER_REVURDERING,
                SØKNAD,
                VURDER_RETTIGHETSPERIODE,
                VURDER_LOVVALG,
                FASTSETT_MELDEPERIODER,
                VURDER_ALDER,
                AVKLAR_STUDENT,
                AVKLAR_SYKDOM
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
        val person = Person(UUID.randomUUID(), listOf(genererIdent(LocalDate.now().minusYears(23))))

        val sak = sakRepository.finnEllerOpprett(person, Periode(LocalDate.now(), LocalDate.now().plusYears(1)))
        val behandling =
            behandlingRepository.opprettBehandling(
                sakId = sak.id,
                typeBehandling = TypeBehandling.Førstegangsbehandling,
                forrigeBehandlingId = null,
                vurderingsbehovOgÅrsak = VurderingsbehovOgÅrsak(
                    vurderingsbehov = listOf(VurderingsbehovMedPeriode(Vurderingsbehov.MOTTATT_SØKNAD)),
                    årsak = ÅrsakTilOpprettelse.SØKNAD
                )
            )
        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(behandling.id)
        avklaringsbehovene.leggTil(
            definisjoner = listOf(Definisjon.AVKLAR_STUDENT), funnetISteg = AVKLAR_STUDENT
        )
        avklaringsbehovene.løsAvklaringsbehov(Definisjon.AVKLAR_STUDENT, "asdf", "TESTEN")
        avklaringsbehovene.leggTil(
            definisjoner = listOf(Definisjon.AVKLAR_SYKDOM), funnetISteg = AVKLAR_SYKDOM
        )

        val flytKontekst = flytOrkestrator.opprettKontekst(behandling.sakId, behandling.id)
        flytOrkestrator.forberedOgProsesserBehandling(flytKontekst)

        assertThat(behandling.status()).isEqualTo(Status.UTREDES)
        assertThat(behandling.aktivtSteg()).isEqualTo(AVKLAR_SYKDOM)
        assertThat(behandlingRepository.hentStegHistorikk(behandling.id)).isNotEmpty()
        assertThat(behandlingRepository.hentStegHistorikk(behandling.id)).containsExactlyElementsOf(
            listOf(
                StegTilstand(stegType = START_BEHANDLING, stegStatus = StegStatus.START, aktiv = false),
                StegTilstand(
                    stegType = START_BEHANDLING,
                    stegStatus = StegStatus.OPPDATER_FAKTAGRUNNLAG,
                    aktiv = false
                ),
                StegTilstand(stegType = START_BEHANDLING, stegStatus = StegStatus.UTFØRER, aktiv = false),
                StegTilstand(
                    stegType = START_BEHANDLING,
                    stegStatus = StegStatus.AVKLARINGSPUNKT,
                    aktiv = false
                ),
                StegTilstand(stegType = START_BEHANDLING, stegStatus = StegStatus.AVSLUTTER, aktiv = false),
                StegTilstand(
                    stegType = SEND_FORVALTNINGSMELDING,
                    stegStatus = StegStatus.START,
                    aktiv = false
                ),
                StegTilstand(
                    stegType = SEND_FORVALTNINGSMELDING,
                    stegStatus = StegStatus.OPPDATER_FAKTAGRUNNLAG,
                    aktiv = false
                ),
                StegTilstand(
                    stegType = SEND_FORVALTNINGSMELDING,
                    stegStatus = StegStatus.UTFØRER,
                    aktiv = false
                ),
                StegTilstand(
                    stegType = SEND_FORVALTNINGSMELDING,
                    stegStatus = StegStatus.AVKLARINGSPUNKT,
                    aktiv = false
                ),
                StegTilstand(
                    stegType = SEND_FORVALTNINGSMELDING,
                    stegStatus = StegStatus.AVSLUTTER,
                    aktiv = false
                ),StegTilstand(
                    stegType = KANSELLER_REVURDERING,
                    stegStatus = StegStatus.START,
                    aktiv = false
                ),
                StegTilstand(
                    stegType = KANSELLER_REVURDERING,
                    stegStatus = StegStatus.OPPDATER_FAKTAGRUNNLAG,
                    aktiv = false
                ),
                StegTilstand(
                    stegType = KANSELLER_REVURDERING,
                    stegStatus = StegStatus.UTFØRER,
                    aktiv = false
                ),
                StegTilstand(
                    stegType = KANSELLER_REVURDERING,
                    stegStatus = StegStatus.AVKLARINGSPUNKT,
                    aktiv = false
                ),
                StegTilstand(
                    stegType = KANSELLER_REVURDERING,
                    stegStatus = StegStatus.AVSLUTTER,
                    aktiv = false
                ),
                StegTilstand(stegType = SØKNAD, stegStatus = StegStatus.START, aktiv = false),
                StegTilstand(stegType = SØKNAD, stegStatus = StegStatus.OPPDATER_FAKTAGRUNNLAG, aktiv = false),
                StegTilstand(stegType = SØKNAD, stegStatus = StegStatus.UTFØRER, aktiv = false),
                StegTilstand(stegType = SØKNAD, stegStatus = StegStatus.AVKLARINGSPUNKT, aktiv = false),
                StegTilstand(stegType = SØKNAD, stegStatus = StegStatus.AVSLUTTER, aktiv = false),
                StegTilstand(
                    stegType = VURDER_RETTIGHETSPERIODE,
                    stegStatus = StegStatus.START,
                    aktiv = false
                ),
                StegTilstand(
                    stegType = VURDER_RETTIGHETSPERIODE,
                    stegStatus = StegStatus.OPPDATER_FAKTAGRUNNLAG,
                    aktiv = false
                ),
                StegTilstand(
                    stegType = VURDER_RETTIGHETSPERIODE,
                    stegStatus = StegStatus.UTFØRER,
                    aktiv = false
                ),
                StegTilstand(
                    stegType = VURDER_RETTIGHETSPERIODE,
                    stegStatus = StegStatus.AVKLARINGSPUNKT,
                    aktiv = false
                ),
                StegTilstand(
                    stegType = VURDER_RETTIGHETSPERIODE,
                    stegStatus = StegStatus.AVSLUTTER,
                    aktiv = false
                ),
                StegTilstand(stegType = VURDER_LOVVALG, stegStatus = StegStatus.START, aktiv = false),
                StegTilstand(
                    stegType = VURDER_LOVVALG,
                    stegStatus = StegStatus.OPPDATER_FAKTAGRUNNLAG,
                    aktiv = false
                ),
                StegTilstand(stegType = VURDER_LOVVALG, stegStatus = StegStatus.UTFØRER, aktiv = false),
                StegTilstand(
                    stegType = VURDER_LOVVALG,
                    stegStatus = StegStatus.AVKLARINGSPUNKT,
                    aktiv = false
                ),
                StegTilstand(stegType = VURDER_LOVVALG, stegStatus = StegStatus.AVSLUTTER, aktiv = false),
                StegTilstand(stegType = FASTSETT_MELDEPERIODER, stegStatus = StegStatus.START, aktiv = false),
                StegTilstand(
                    stegType = FASTSETT_MELDEPERIODER,
                    stegStatus = StegStatus.OPPDATER_FAKTAGRUNNLAG,
                    aktiv = false
                ),
                StegTilstand(
                    stegType = FASTSETT_MELDEPERIODER,
                    stegStatus = StegStatus.UTFØRER,
                    aktiv = false
                ),
                StegTilstand(
                    stegType = FASTSETT_MELDEPERIODER,
                    stegStatus = StegStatus.AVKLARINGSPUNKT,
                    aktiv = false
                ),
                StegTilstand(
                    stegType = FASTSETT_MELDEPERIODER,
                    stegStatus = StegStatus.AVSLUTTER,
                    aktiv = false
                ),
                StegTilstand(stegType = VURDER_ALDER, stegStatus = StegStatus.START, aktiv = false),
                StegTilstand(
                    stegType = VURDER_ALDER,
                    stegStatus = StegStatus.OPPDATER_FAKTAGRUNNLAG,
                    aktiv = false
                ),
                StegTilstand(stegType = VURDER_ALDER, stegStatus = StegStatus.UTFØRER, aktiv = false),
                StegTilstand(stegType = VURDER_ALDER, stegStatus = StegStatus.AVKLARINGSPUNKT, aktiv = false),
                StegTilstand(stegType = VURDER_ALDER, stegStatus = StegStatus.AVSLUTTER, aktiv = false),
                StegTilstand(stegType = AVKLAR_STUDENT, stegStatus = StegStatus.START, aktiv = false),
                StegTilstand(
                    stegType = AVKLAR_STUDENT,
                    stegStatus = StegStatus.OPPDATER_FAKTAGRUNNLAG,
                    aktiv = false
                ),
                StegTilstand(stegType = AVKLAR_STUDENT, stegStatus = StegStatus.UTFØRER, aktiv = false),
                StegTilstand(
                    stegType = AVKLAR_STUDENT,
                    stegStatus = StegStatus.AVKLARINGSPUNKT,
                    aktiv = false
                ),
                StegTilstand(stegType = AVKLAR_STUDENT, stegStatus = StegStatus.AVSLUTTER, aktiv = false),
                StegTilstand(stegType = AVKLAR_SYKDOM, stegStatus = StegStatus.START, aktiv = false),
                StegTilstand(
                    stegType = AVKLAR_SYKDOM,
                    stegStatus = StegStatus.OPPDATER_FAKTAGRUNNLAG,
                    aktiv = false
                ),
                StegTilstand(stegType = AVKLAR_SYKDOM, stegStatus = StegStatus.UTFØRER, aktiv = false),
                StegTilstand(stegType = AVKLAR_SYKDOM, stegStatus = StegStatus.AVKLARINGSPUNKT, aktiv = true),
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
                StegTilstand(stegType = START_BEHANDLING, stegStatus = StegStatus.START, aktiv = false),
                StegTilstand(
                    stegType = START_BEHANDLING,
                    stegStatus = StegStatus.OPPDATER_FAKTAGRUNNLAG,
                    aktiv = false
                ),
                StegTilstand(stegType = START_BEHANDLING, stegStatus = StegStatus.UTFØRER, aktiv = false),
                StegTilstand(
                    stegType = START_BEHANDLING,
                    stegStatus = StegStatus.AVKLARINGSPUNKT,
                    aktiv = false
                ),
                StegTilstand(stegType = START_BEHANDLING, stegStatus = StegStatus.AVSLUTTER, aktiv = false),
                StegTilstand(
                    stegType = SEND_FORVALTNINGSMELDING,
                    stegStatus = StegStatus.START,
                    aktiv = false
                ),
                StegTilstand(
                    stegType = SEND_FORVALTNINGSMELDING,
                    stegStatus = StegStatus.OPPDATER_FAKTAGRUNNLAG,
                    aktiv = false
                ),
                StegTilstand(
                    stegType = SEND_FORVALTNINGSMELDING,
                    stegStatus = StegStatus.UTFØRER,
                    aktiv = false
                ),
                StegTilstand(
                    stegType = SEND_FORVALTNINGSMELDING,
                    stegStatus = StegStatus.AVKLARINGSPUNKT,
                    aktiv = false
                ),
                StegTilstand(
                    stegType = SEND_FORVALTNINGSMELDING,
                    stegStatus = StegStatus.AVSLUTTER,
                    aktiv = false
                ),
                StegTilstand(
                    stegType = KANSELLER_REVURDERING,
                    stegStatus = StegStatus.START,
                    aktiv = false
                ),
                StegTilstand(
                    stegType = KANSELLER_REVURDERING,
                    stegStatus = StegStatus.OPPDATER_FAKTAGRUNNLAG,
                    aktiv = false
                ),
                StegTilstand(
                    stegType = KANSELLER_REVURDERING,
                    stegStatus = StegStatus.UTFØRER,
                    aktiv = false
                ),
                StegTilstand(
                    stegType = KANSELLER_REVURDERING,
                    stegStatus = StegStatus.AVKLARINGSPUNKT,
                    aktiv = false
                ),
                StegTilstand(
                    stegType = KANSELLER_REVURDERING,
                    stegStatus = StegStatus.AVSLUTTER,
                    aktiv = false
                ),
                StegTilstand(stegType = SØKNAD, stegStatus = StegStatus.START, aktiv = false),
                StegTilstand(stegType = SØKNAD, stegStatus = StegStatus.OPPDATER_FAKTAGRUNNLAG, aktiv = false),
                StegTilstand(stegType = SØKNAD, stegStatus = StegStatus.UTFØRER, aktiv = false),
                StegTilstand(stegType = SØKNAD, stegStatus = StegStatus.AVKLARINGSPUNKT, aktiv = false),
                StegTilstand(stegType = SØKNAD, stegStatus = StegStatus.AVSLUTTER, aktiv = false),
                StegTilstand(
                    stegType = VURDER_RETTIGHETSPERIODE,
                    stegStatus = StegStatus.START,
                    aktiv = false
                ),
                StegTilstand(
                    stegType = VURDER_RETTIGHETSPERIODE,
                    stegStatus = StegStatus.OPPDATER_FAKTAGRUNNLAG,
                    aktiv = false
                ),
                StegTilstand(
                    stegType = VURDER_RETTIGHETSPERIODE,
                    stegStatus = StegStatus.UTFØRER,
                    aktiv = false
                ),
                StegTilstand(
                    stegType = VURDER_RETTIGHETSPERIODE,
                    stegStatus = StegStatus.AVKLARINGSPUNKT,
                    aktiv = false
                ),
                StegTilstand(
                    stegType = VURDER_RETTIGHETSPERIODE,
                    stegStatus = StegStatus.AVSLUTTER,
                    aktiv = false
                ),
                StegTilstand(stegType = VURDER_LOVVALG, stegStatus = StegStatus.START, aktiv = false),
                StegTilstand(
                    stegType = VURDER_LOVVALG,
                    stegStatus = StegStatus.OPPDATER_FAKTAGRUNNLAG,
                    aktiv = false
                ),
                StegTilstand(stegType = VURDER_LOVVALG, stegStatus = StegStatus.UTFØRER, aktiv = false),
                StegTilstand(
                    stegType = VURDER_LOVVALG,
                    stegStatus = StegStatus.AVKLARINGSPUNKT,
                    aktiv = false
                ),
                StegTilstand(stegType = VURDER_LOVVALG, stegStatus = StegStatus.AVSLUTTER, aktiv = false),
                StegTilstand(stegType = FASTSETT_MELDEPERIODER, stegStatus = StegStatus.START, aktiv = false),
                StegTilstand(
                    stegType = FASTSETT_MELDEPERIODER,
                    stegStatus = StegStatus.OPPDATER_FAKTAGRUNNLAG,
                    aktiv = false
                ),
                StegTilstand(
                    stegType = FASTSETT_MELDEPERIODER,
                    stegStatus = StegStatus.UTFØRER,
                    aktiv = false
                ),
                StegTilstand(
                    stegType = FASTSETT_MELDEPERIODER,
                    stegStatus = StegStatus.AVKLARINGSPUNKT,
                    aktiv = false
                ),
                StegTilstand(
                    stegType = FASTSETT_MELDEPERIODER,
                    stegStatus = StegStatus.AVSLUTTER,
                    aktiv = false
                ),
                StegTilstand(stegType = VURDER_ALDER, stegStatus = StegStatus.START, aktiv = false),
                StegTilstand(
                    stegType = VURDER_ALDER,
                    stegStatus = StegStatus.OPPDATER_FAKTAGRUNNLAG,
                    aktiv = false
                ),
                StegTilstand(stegType = VURDER_ALDER, stegStatus = StegStatus.UTFØRER, aktiv = false),
                StegTilstand(stegType = VURDER_ALDER, stegStatus = StegStatus.AVKLARINGSPUNKT, aktiv = false),
                StegTilstand(stegType = VURDER_ALDER, stegStatus = StegStatus.AVSLUTTER, aktiv = false),
                StegTilstand(stegType = AVKLAR_STUDENT, stegStatus = StegStatus.START, aktiv = false),
                StegTilstand(
                    stegType = AVKLAR_STUDENT,
                    stegStatus = StegStatus.OPPDATER_FAKTAGRUNNLAG,
                    aktiv = false
                ),
                StegTilstand(stegType = AVKLAR_STUDENT, stegStatus = StegStatus.UTFØRER, aktiv = false),
                StegTilstand(
                    stegType = AVKLAR_STUDENT,
                    stegStatus = StegStatus.AVKLARINGSPUNKT,
                    aktiv = false
                ),
                StegTilstand(stegType = AVKLAR_STUDENT, stegStatus = StegStatus.AVSLUTTER, aktiv = false),
                StegTilstand(stegType = AVKLAR_SYKDOM, stegStatus = StegStatus.START, aktiv = false),
                StegTilstand(
                    stegType = AVKLAR_SYKDOM,
                    stegStatus = StegStatus.OPPDATER_FAKTAGRUNNLAG,
                    aktiv = false
                ),
                StegTilstand(stegType = AVKLAR_SYKDOM, stegStatus = StegStatus.UTFØRER, aktiv = false),
                StegTilstand(stegType = AVKLAR_SYKDOM, stegStatus = StegStatus.AVKLARINGSPUNKT, aktiv = false),
                StegTilstand(stegType = AVKLAR_SYKDOM, stegStatus = StegStatus.TILBAKEFØRT, aktiv = false),
                StegTilstand(stegType = AVKLAR_STUDENT, stegStatus = StegStatus.TILBAKEFØRT, aktiv = false),
                StegTilstand(stegType = AVKLAR_STUDENT, stegStatus = StegStatus.START, aktiv = false),
                StegTilstand(
                    stegType = AVKLAR_STUDENT,
                    stegStatus = StegStatus.OPPDATER_FAKTAGRUNNLAG,
                    aktiv = false
                ),
                StegTilstand(stegType = AVKLAR_STUDENT, stegStatus = StegStatus.UTFØRER, aktiv = false),
                StegTilstand(
                    stegType = AVKLAR_STUDENT,
                    stegStatus = StegStatus.AVKLARINGSPUNKT,
                    aktiv = false
                ),
                StegTilstand(stegType = AVKLAR_STUDENT, stegStatus = StegStatus.AVSLUTTER, aktiv = false),
                StegTilstand(stegType = AVKLAR_SYKDOM, stegStatus = StegStatus.START, aktiv = false),
                StegTilstand(
                    stegType = AVKLAR_SYKDOM,
                    stegStatus = StegStatus.OPPDATER_FAKTAGRUNNLAG,
                    aktiv = false
                ),
                StegTilstand(stegType = AVKLAR_SYKDOM, stegStatus = StegStatus.UTFØRER, aktiv = false),
                StegTilstand(stegType = AVKLAR_SYKDOM, stegStatus = StegStatus.AVKLARINGSPUNKT, aktiv = true),
            )
        )
        assertThat(behandling.status()).isEqualTo(Status.UTREDES)
    }
}