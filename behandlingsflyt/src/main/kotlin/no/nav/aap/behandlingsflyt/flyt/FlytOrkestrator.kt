package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehov
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravGrunnlagImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.StegOrkestrator
import no.nav.aap.behandlingsflyt.flyt.steg.TilbakeførtFraBeslutter
import no.nav.aap.behandlingsflyt.flyt.steg.TilbakeførtFraKvalitetssikrer
import no.nav.aap.behandlingsflyt.flyt.steg.Transisjon
import no.nav.aap.behandlingsflyt.flyt.ventebehov.VentebehovEvaluererService
import no.nav.aap.behandlingsflyt.flyt.ventebehov.VentebehovEvaluererServiceImpl
import no.nav.aap.behandlingsflyt.hendelse.avløp.BehandlingHendelseService
import no.nav.aap.behandlingsflyt.hendelse.avløp.BehandlingHendelseServiceImpl
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Status.SENDT_TILBAKE_FRA_BESLUTTER
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Status.SENDT_TILBAKE_FRA_KVALITETSSIKRER
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.sak.Status.UTREDES
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.periodisering.FlytKontekstMedPeriodeService
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekst
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.StegStatus
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory


/**
 * Har ansvar for å drive flyten til en gitt behandling. Typen behandling styrer hvilke steg som skal utføres.
 *
 * ## Forbered Behandling
 * Har ansvar for å sette behandlingen i en oppdatert tilstand i form av å innhente opplysninger for stegene man allerede
 * har prosessert og vurdere om man er nødt til å behandle steget på nytt hvis det er oppdaterte opplysninger.
 *
 * ## Prosesser Behandling
 * Har ansvar for å drive prosessen fremover, stoppe opp ved behov for besluttningsstøtte av et menneske og sørge for at
 * at stegene traverseres i den definerte rekkefølgen i flyten. Flytene defineres i typen behandlingen.
 *
 */
class FlytOrkestrator(
    private val flytKontekstMedPeriodeService: FlytKontekstMedPeriodeService,
    private val sakOgBehandlingService: SakOgBehandlingService,
    private val informasjonskravGrunnlag: InformasjonskravGrunnlag,
    private val sakRepository: SakRepository,
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val behandlingRepository: BehandlingRepository,
    private val behandlingHendelseService: BehandlingHendelseService,
    private val ventebehovEvaluererService: VentebehovEvaluererService,
    private val stegOrkestrator: StegOrkestrator,
    private val stoppNårStatus: Set<Status> = emptySet(),
) {
    constructor(
        repositoryProvider: RepositoryProvider,
        gatewayProvider: GatewayProvider,
        stoppNårStatus: Set<Status> = emptySet(),
        markSavepointAt: Set<StegStatus>? = null,
    ) : this(
        ventebehovEvaluererService = VentebehovEvaluererServiceImpl(repositoryProvider),
        behandlingRepository = repositoryProvider.provide(),
        avklaringsbehovRepository = repositoryProvider.provide(),
        informasjonskravGrunnlag = InformasjonskravGrunnlagImpl(repositoryProvider, gatewayProvider),
        sakRepository = repositoryProvider.provide(),
        flytKontekstMedPeriodeService = FlytKontekstMedPeriodeService(repositoryProvider, gatewayProvider),
        sakOgBehandlingService = SakOgBehandlingService(repositoryProvider, gatewayProvider),
        behandlingHendelseService = BehandlingHendelseServiceImpl(repositoryProvider),
        stegOrkestrator = StegOrkestrator(repositoryProvider, gatewayProvider, markSavepointAt),
        stoppNårStatus = stoppNårStatus,
    )

    private val log = LoggerFactory.getLogger(javaClass)

    fun opprettKontekst(sakId: SakId, behandlingId: BehandlingId): FlytKontekst {
        val behandling = behandlingRepository.hent(behandlingId)

        return FlytKontekst(
            sakId = sakId,
            behandlingId = behandlingId,
            behandlingType = behandling.typeBehandling(),
            forrigeBehandlingId = behandling.forrigeBehandlingId
        )
    }

    fun tilbakeførEtterAtomærBehandling(kontekst: FlytKontekst) {
        val behandling = behandlingRepository.hent(kontekst.behandlingId)
        val behandlingFlyt = behandling.flyt()
        behandlingFlyt.forberedFlyt(behandling.aktivtSteg())

        val endredeInformasjonskrav = informasjonskravGrunnlag
            .flettOpplysningerFraAtomærBehandling(kontekst, behandlingFlyt.alleInformasjonskravForÅpneSteg())
        log.info("Endrede informasjonskrav etter atomær behandling: {}", endredeInformasjonskrav)
        tilbakefør(
            kontekst = kontekst,
            behandling = behandling,
            behandlingFlyt = behandlingFlyt.tilbakeflytEtterEndringer(endredeInformasjonskrav),
            avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(behandling.id),
        )
    }


    fun forberedOgProsesserBehandling(
        kontekst: FlytKontekst,
        triggere: List<Vurderingsbehov> = emptyList()
    ) {
        this.forberedBehandling(kontekst, triggere)
        this.prosesserBehandling(kontekst)
    }

    private fun forberedBehandling(kontekst: FlytKontekst, triggere: List<Vurderingsbehov>?) {
        val behandling = behandlingRepository.hent(kontekst.behandlingId)
        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)

        avklaringsbehovene.validateTilstand(behandling = behandling)

        val behandlingFlyt = behandling.flyt()

        if (!behandling.harBehandlingenStartet()) {
            sakRepository.oppdaterSakStatus(kontekst.sakId, UTREDES)
        }

        behandlingFlyt.forberedFlyt(behandling.aktivtSteg())

        // fjerner av ventepunkt med utløpt frist
        if (avklaringsbehovene.erSattPåVent()) {
            val behovSomBleLøst = ventebehovEvaluererService.løsVentebehov(kontekst, avklaringsbehovene)

            // Hvis fortsatt på vent
            if (!avklaringsbehovene.erSattPåVent()) {
                // Behandlingen er tatt av vent og flyten flyttes tilbake til steget hvor den sto på vent
                val tilbakeflyt = behandlingFlyt.tilbakeflyt(behovSomBleLøst)
                if (!tilbakeflyt.erTom()) {
                    log.info(
                        "Tilbakeført etter tatt av vent fra '{}' til '{}'",
                        behandling.aktivtSteg(),
                        tilbakeflyt.stegene().last()
                    )
                }
                tilbakefør(kontekst, behandling, tilbakeflyt, avklaringsbehovene)
            }
        }

        førTilbakeTilTidligsteÅpneAvklaringsbehov(avklaringsbehovene, behandlingFlyt, behandling, kontekst)

        log.info("Oppdaterer faktagrunnlag for kravliste")
        val oppdaterFaktagrunnlagForKravliste =
            informasjonskravGrunnlag.oppdaterFaktagrunnlagForKravliste(
                kravkonstruktører = behandlingFlyt.alleFaktagrunnlagFørGjeldendeSteg(),
                kontekst = flytKontekstMedPeriodeService.utled(kontekst, behandling.aktivtSteg()),
            )

        log.info("Sjekker om noe skal tilbakeføres etter oppdatering av informasjonskrav")
        val tilbakeføringsflyt = behandlingFlyt.tilbakeflytEtterEndringer(oppdaterFaktagrunnlagForKravliste, triggere)

        if (!tilbakeføringsflyt.erTom()) {
            log.info(
                "Tilbakeført etter oppdatering av registeropplysninger fra '{}' til '{}'",
                behandling.aktivtSteg(),
                tilbakeføringsflyt.stegene().last()
            )
        }
        tilbakefør(kontekst, behandling, tilbakeføringsflyt, avklaringsbehovene)
    }

    private fun førTilbakeTilTidligsteÅpneAvklaringsbehov(
        avklaringsbehovene: Avklaringsbehovene,
        behandlingFlyt: BehandlingFlyt,
        behandling: Behandling,
        kontekst: FlytKontekst
    ) {
        val tidligsteÅpneAvklaringsbehov = avklaringsbehovene.åpne()
            .minWithOrNull(compareBy(behandlingFlyt.stegComparator) { it.løsesISteg() })

        if (tidligsteÅpneAvklaringsbehov != null) {
            val sendtTilbakeFraBeslutterNå =
                tidligsteÅpneAvklaringsbehov.status() == SENDT_TILBAKE_FRA_BESLUTTER && behandling.aktivtSteg() == StegType.FATTE_VEDTAK
            val sendtTilbakeFraKvalitetssikrerNå =
                tidligsteÅpneAvklaringsbehov.status() == SENDT_TILBAKE_FRA_KVALITETSSIKRER && behandling.aktivtSteg() == StegType.KVALITETSSIKRING
            if (behandlingFlyt.erStegFør(tidligsteÅpneAvklaringsbehov.løsesISteg(), behandling.aktivtSteg())) {
                if (!sendtTilbakeFraBeslutterNå && !sendtTilbakeFraKvalitetssikrerNå) {
                    log.error(
                        """
                        Behandlingen er i steg ${behandling.aktivtSteg()} og har passert det åpne
                        avklaringsbehovet ${tidligsteÅpneAvklaringsbehov.definisjon} som skal løses i
                        steg ${tidligsteÅpneAvklaringsbehov.løsesISteg()}. Med mindre det har skjedd
                        en endring i rekkefølgen av stegene, så er dette en bug.
                        """.trimIndent().replace("\n", " ")
                    )

                    val tilbakeflyt = behandlingFlyt.tilbakeflyt(tidligsteÅpneAvklaringsbehov)
                    tilbakefør(kontekst, behandling, tilbakeflyt, avklaringsbehovene)
                }
            }
        }
    }

    private fun prosesserBehandling(kontekst: FlytKontekst) {
        val behandling = behandlingRepository.hent(kontekst.behandlingId)
        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)

        avklaringsbehovene.validateTilstand(behandling = behandling)

        val behandlingFlyt = behandling.flyt()
        var gjeldendeSteg = finnGjeldendeSteg(behandling, behandlingFlyt)

        oppdaterBehandlingstatusHvisEndret(behandling, gjeldendeSteg.type().status)

        while (true) {
            if (gjeldendeSteg.type().status in stoppNårStatus) {
                loggStopp(behandling, avklaringsbehovene)
                val oppdatertBehandling = behandlingRepository.hent(behandling.id)
                behandlingHendelseService.stoppet(oppdatertBehandling, avklaringsbehovene)
                return
            }

            val kontekstMedPerioder = flytKontekstMedPeriodeService.utled(kontekst, gjeldendeSteg.type())
            val result = stegOrkestrator.utfør(
                gjeldendeSteg,
                kontekstMedPerioder,
                behandling,
                behandlingFlyt.faktagrunnlagForGjeldendeSteg()
            )

            if (result.erTilbakeføring()) {
                val tilbakeføringsflyt = when (result) {
                    is TilbakeførtFraBeslutter -> behandlingFlyt.tilbakeflyt(avklaringsbehovene.tilbakeførtFraBeslutter())
                    is TilbakeførtFraKvalitetssikrer -> behandlingFlyt.tilbakeflyt(avklaringsbehovene.tilbakeførtFraKvalitetssikrer())
                    else -> {
                        throw IllegalStateException("Uhåndtert transisjon ved tilbakeføring. Faktisk type: ${result.javaClass}.")
                    }
                }
                log.info(
                    "Tilbakeført fra '{}' til '{}'",
                    gjeldendeSteg.type(),
                    tilbakeføringsflyt.stegene().last()
                )
                tilbakefør(kontekst, behandling, tilbakeføringsflyt, avklaringsbehovene, false)
            }

            validerPlassering(behandlingFlyt, avklaringsbehovene.åpne())

            val neste = utledNesteSteg(result, behandlingFlyt)

            oppdaterBehandlingstatusHvisEndret(behandling, neste?.type()?.status)

            if (!result.kanFortsette() || neste == null) {
                if (neste == null) {
                    log.info("Behandlingen har nådd slutten, avslutter behandling")

                    sakOgBehandlingService.lukkBehandling(behandling.id)

                    validerAtAvklaringsBehovErLukkede(avklaringsbehovene)
                } else {
                    // Prosessen har stoppet opp, slipp ut hendelse om at den har stoppet opp og hvorfor?
                    loggStopp(behandling, avklaringsbehovene)
                }
                val oppdatertBehandling = behandlingRepository.hent(behandling.id)

                // Si i fra til behandlingHendelsesService ved stopp i behandlingen.
                behandlingHendelseService.stoppet(oppdatertBehandling, avklaringsbehovene)
                return
            }
            gjeldendeSteg = neste
        }
    }

    private fun finnGjeldendeSteg(
        behandling: Behandling,
        behandlingFlyt: BehandlingFlyt
    ): FlytSteg {
        var sisteStegType = behandling.aktivtSteg()

        if (behandling.aktivtStegTilstand().status() == StegStatus.AVSLUTTER) {
            behandlingFlyt.forberedFlyt(sisteStegType)
            sisteStegType = behandlingFlyt.neste()?.type() ?: sisteStegType
        }

        return behandlingFlyt.forberedFlyt(sisteStegType)
    }

    private fun validerAtAvklaringsBehovErLukkede(avklaringsbehovene: Avklaringsbehovene) {
        check(avklaringsbehovene.åpne().isEmpty()) {
            "Behandlingen er avsluttet, men det finnes åpne avklaringsbehov."
        }
    }

    private fun oppdaterBehandlingstatusHvisEndret(behandling: Behandling, etterStatus: Status?) {
        if (etterStatus != null) {
            val førStatus = behandling.status()
            if (førStatus != etterStatus) {
                behandlingRepository.oppdaterBehandlingStatus(behandlingId = behandling.id, status = etterStatus)
            }
        }
    }

    /**
     * Gitt en flyt, utled neste [FlytSteg]. `null` betyr at det er ikke flere steg, og behandlingen
     * anses som avsluttet.
     */
    private fun utledNesteSteg(
        result: Transisjon,
        behandlingFlyt: BehandlingFlyt
    ): FlytSteg? {
        val neste = if (result.erTilbakeføring() || !result.kanFortsette()) {
            behandlingFlyt.aktivtSteg()
        } else {
            behandlingFlyt.neste()
        }
        return neste
    }

    internal fun forberedLøsingAvBehov(
        behovDefinisjon: Definisjon,
        behandling: Behandling,
        kontekst: FlytKontekst,
        bruker: Bruker
    ) {
        val flyt = behandling.flyt()
        flyt.forberedFlyt(behandling.aktivtSteg())

        opprettAvklaringsbehovHvisMangler(behovDefinisjon, kontekst, bruker)
        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)
        val behovForLøsninger = avklaringsbehovene.hentBehovForDefinisjon(behovDefinisjon)
        val tilbakeføringsflyt = flyt.tilbakeflyt(behovForLøsninger)

        tilbakefør(
            kontekst,
            behandling,
            tilbakeføringsflyt,
            avklaringsbehovene,
            harHeltStoppet = false
        ) // Setter til false for å ikke trigge unødvendig event

        val skulleVærtISteg = flyt.skalTilStegForBehov(behovForLøsninger)
        // Skal få lov å løse ventebehov overalt i flyten
        if (skulleVærtISteg != null && !behovDefinisjon.erVentebehov()) {
            flyt.validerPlassering(skulleVærtISteg)
        }
    }

    private fun opprettAvklaringsbehovHvisMangler(behovDefinisjon: Definisjon, kontekst: FlytKontekst, bruker: Bruker) {
        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)
        if (avklaringsbehovene.hentBehovForDefinisjon(behovDefinisjon) == null) {
            avklaringsbehovene.leggTilFrivilligHvisMangler(behovDefinisjon, bruker)
            avklaringsbehovene.leggTilOverstyringHvisMangler(behovDefinisjon, bruker)
        }
    }

    private fun tilbakefør(
        kontekst: FlytKontekst,
        behandling: Behandling,
        behandlingFlyt: BehandlingFlyt,
        avklaringsbehovene: Avklaringsbehovene,
        harHeltStoppet: Boolean = true
    ) {
        log.info(
            "Tilbakefører {} for behandling {} med flyt {}",
            behandling.aktivtSteg(),
            behandling.referanse,
            behandlingFlyt
        )
        if (behandlingFlyt.erTom()) {
            return
        }

        var neste: FlytSteg? = behandlingFlyt.aktivtSteg()
        while (true) {

            if (neste == null) {
                loggStopp(behandling, avklaringsbehovene)
                if (harHeltStoppet) {
                    behandlingHendelseService.stoppet(behandling, avklaringsbehovene)
                }
                return
            }
            stegOrkestrator.utførTilbakefør(
                aktivtSteg = neste,
                kontekstMedPerioder = flytKontekstMedPeriodeService.utled(kontekst, neste.type()),
                behandling = behandling
            )
            neste = behandlingFlyt.neste()
        }
    }

    private fun loggStopp(
        behandling: Behandling,
        avklaringsbehovene: Avklaringsbehovene
    ) {
        log.info(
            "Stopper opp ved {} med {}",
            behandling.aktivtSteg(),
            avklaringsbehovene.åpne()
        )
    }

    private fun validerPlassering(
        behandlingFlyt: BehandlingFlyt,
        åpneAvklaringsbehov: List<Avklaringsbehov>
    ) {
        val nesteSteg = behandlingFlyt.aktivtStegType()
        val uhåndterteBehov = åpneAvklaringsbehov
            .filter { definisjon ->
                behandlingFlyt.erStegFør(
                    definisjon.løsesISteg(),
                    nesteSteg
                )
            }
        if (uhåndterteBehov.isNotEmpty()) {
            throw IllegalStateException("Har uhåndterte behov som skulle vært håndtert før nåværende steg = '$nesteSteg'. Behov: ${uhåndterteBehov.map { it.definisjon }}")
        }
    }
}
