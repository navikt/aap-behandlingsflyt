package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.SYSTEMBRUKER
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehov
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravGrunnlagImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.StegKonstruktør
import no.nav.aap.behandlingsflyt.flyt.steg.StegOrkestrator
import no.nav.aap.behandlingsflyt.flyt.steg.TilbakeførtFraBeslutter
import no.nav.aap.behandlingsflyt.flyt.steg.TilbakeførtFraKvalitetssikrer
import no.nav.aap.behandlingsflyt.flyt.steg.Transisjon
import no.nav.aap.behandlingsflyt.flyt.steg.internal.StegKonstruktørImpl
import no.nav.aap.behandlingsflyt.flyt.ventebehov.VentebehovEvaluererService
import no.nav.aap.behandlingsflyt.flyt.ventebehov.VentebehovEvaluererServiceImpl
import no.nav.aap.behandlingsflyt.hendelse.avløp.BehandlingHendelseService
import no.nav.aap.behandlingsflyt.hendelse.avløp.BehandlingHendelseServiceImpl
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.sak.Status.UTREDES
import no.nav.aap.behandlingsflyt.periodisering.PerioderTilVurderingService
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekst
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.komponenter.httpklient.auth.Bruker
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
    private val stegKonstruktør: StegKonstruktør,
    private val perioderTilVurderingService: PerioderTilVurderingService,
    private val sakOgBehandlingService: SakOgBehandlingService,
    private val informasjonskravGrunnlag: InformasjonskravGrunnlag,
    private val sakRepository: SakRepository,
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val behandlingRepository: BehandlingRepository,
    private val behandlingHendelseService: BehandlingHendelseService,
    private val ventebehovEvaluererService: VentebehovEvaluererService
) {
    constructor(repositoryProvider: RepositoryProvider): this(
        stegKonstruktør = StegKonstruktørImpl(repositoryProvider),
        ventebehovEvaluererService = VentebehovEvaluererServiceImpl(repositoryProvider),
        behandlingRepository = repositoryProvider.provide(),
        avklaringsbehovRepository = repositoryProvider.provide(),
        informasjonskravGrunnlag = InformasjonskravGrunnlagImpl(repositoryProvider),
        sakRepository = repositoryProvider.provide(),
        perioderTilVurderingService = PerioderTilVurderingService(repositoryProvider),
        sakOgBehandlingService = SakOgBehandlingService(repositoryProvider),
        behandlingHendelseService = BehandlingHendelseServiceImpl(repositoryProvider),
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

    fun forberedOgProsesserBehandling(kontekst: FlytKontekst) {
        this.forberedBehandling(kontekst)
        this.prosesserBehandling(kontekst)
    }

    private fun forberedBehandling(kontekst: FlytKontekst) {
        val behandling = behandlingRepository.hent(kontekst.behandlingId)
        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)

        avklaringsbehovene.validateTilstand(behandling = behandling)

        val behandlingFlyt = utledFlytFra(behandling)

        if (!behandling.harBehandlingenStartet()) {
            sakRepository.oppdaterSakStatus(kontekst.sakId, UTREDES)
        }

        behandlingFlyt.forberedFlyt(behandling.aktivtSteg())

        // fjerner av ventepunkt med utløpt frist
        if (avklaringsbehovene.erSattPåVent()) {
            // TODO: Vurdere om det hendelser som trigger prosesserBehandling
            //  (f.eks ankommet dokument) skal ta behandling av vent
            val kandidatBehov = avklaringsbehovene.hentÅpneVentebehov()

            val behovSomErLøst =
                kandidatBehov.filter { behov ->
                    ventebehovEvaluererService.ansesSomLøst(
                        behandling.id,
                        behov,
                        kontekst.sakId
                    )
                }
            behovSomErLøst.forEach { avklaringsbehovene.løsAvklaringsbehov(
                definisjon = it.definisjon,
                begrunnelse = "Ventebehov løst.",
                endretAv = SYSTEMBRUKER.ident
            ) }

            // Hvis fortsatt på vent
            if (avklaringsbehovene.erSattPåVent()) {
                return // Bail out
            } else {
                // Behandlingen er tatt av vent pga frist og flyten flyttes tilbake til steget hvor den sto på vent
                val tilbakeflyt = behandlingFlyt.tilbakeflyt(behovSomErLøst)
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

        val oppdaterFaktagrunnlagForKravliste =
            informasjonskravGrunnlag.oppdaterFaktagrunnlagForKravliste(
                kravkonstruktører = behandlingFlyt.alleFaktagrunnlagFørGjeldendeSteg(),
                kontekst = FlytKontekstMedPerioder(
                    sakId = kontekst.sakId,
                    behandlingId = kontekst.behandlingId,
                    forrigeBehandlingId = kontekst.forrigeBehandlingId,
                    behandlingType = kontekst.behandlingType,
                    vurdering = perioderTilVurderingService.utled(
                        kontekst = kontekst,
                        stegType = behandling.aktivtSteg()
                    )
                )
            )

        val tilbakeføringsflyt = behandlingFlyt.tilbakeflytEtterEndringer(oppdaterFaktagrunnlagForKravliste)

        if (!tilbakeføringsflyt.erTom()) {
            log.info(
                "Tilbakeført etter oppdatering av registeropplysninger fra '{}' til '{}'",
                behandling.aktivtSteg(),
                tilbakeføringsflyt.stegene().last()
            )
        }
        tilbakefør(kontekst, behandling, tilbakeføringsflyt, avklaringsbehovene)
    }

    private fun prosesserBehandling(kontekst: FlytKontekst) {
        val behandling = behandlingRepository.hent(kontekst.behandlingId)
        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)

        avklaringsbehovene.validateTilstand(behandling = behandling)

        // Hvis fortsatt på vent
        if (avklaringsbehovene.erSattPåVent()) {
            return // Bail out
        }

        val behandlingFlyt = utledFlytFra(behandling)

        var gjeldendeSteg = behandlingFlyt.forberedFlyt(behandling.aktivtSteg())

        while (true) {

            val result = StegOrkestrator(
                aktivtSteg = gjeldendeSteg,
                informasjonskravGrunnlag = informasjonskravGrunnlag,
                behandlingRepository = behandlingRepository,
                avklaringsbehovRepository = avklaringsbehovRepository,
                perioderTilVurderingService = perioderTilVurderingService,
                stegKonstruktør = stegKonstruktør
            ).utfør(
                kontekst,
                behandling,
                behandlingFlyt.faktagrunnlagForGjeldendeSteg()
            )

            val avklaringsbehov = avklaringsbehovene.åpne()
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
            validerPlassering(behandlingFlyt, avklaringsbehov)

            val neste = utledNesteSteg(result, behandlingFlyt)

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

    private fun validerAtAvklaringsBehovErLukkede(avklaringsbehovene: Avklaringsbehovene) {
        check(avklaringsbehovene.åpne().isEmpty()) {
            "Behandlingen er avsluttet, men det finnes åpne avklaringsbehov."
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
        val flyt = utledFlytFra(behandling)
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
        if (skulleVærtISteg != null) {
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
            StegOrkestrator(
                aktivtSteg = neste, informasjonskravGrunnlag = informasjonskravGrunnlag,
                behandlingRepository = behandlingRepository,
                avklaringsbehovRepository = avklaringsbehovRepository,
                perioderTilVurderingService = perioderTilVurderingService,
                stegKonstruktør = stegKonstruktør
            ).utførTilbakefør(
                kontekst = kontekst,
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

    private fun utledFlytFra(behandling: Behandling) = utledType(behandling.typeBehandling()).flyt()

}
