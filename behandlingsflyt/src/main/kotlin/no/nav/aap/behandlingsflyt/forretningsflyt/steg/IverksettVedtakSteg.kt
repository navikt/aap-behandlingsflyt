package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.ResultatUtleder
import no.nav.aap.behandlingsflyt.behandling.avbrytrevurdering.AvbrytRevurderingService
import no.nav.aap.behandlingsflyt.behandling.gosysoppgave.GosysService
import no.nav.aap.behandlingsflyt.behandling.mellomlagring.MellomlagretVurderingRepository
import no.nav.aap.behandlingsflyt.behandling.søknad.TrukketSøknadService
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.VirkningstidspunktUtleder
import no.nav.aap.behandlingsflyt.behandling.utbetaling.UtbetalingService
import no.nav.aap.behandlingsflyt.behandling.vedtak.VedtakRepository
import no.nav.aap.behandlingsflyt.behandling.vedtak.VedtakService
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.refusjonkrav.NavKontorPeriodeDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.refusjonkrav.RefusjonkravRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.prosessering.IverksettUtbetalingJobbUtfører
import no.nav.aap.behandlingsflyt.prosessering.VarsleVedtakJobbUtfører
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.StegStatus
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelseDto
import org.slf4j.LoggerFactory

class IverksettVedtakSteg private constructor(
    private val sakRepository: SakRepository,
    private val behandlingRepository: BehandlingRepository,
    private val refusjonkravRepository: RefusjonkravRepository,
    private val utbetalingService: UtbetalingService,
    private val vedtakService: VedtakService,
    private val virkningstidspunktUtleder: VirkningstidspunktUtleder,
    private val trukketSøknadService: TrukketSøknadService,
    private val avbrytRevurderingService: AvbrytRevurderingService,
    private val gosysService: GosysService,
    private val flytJobbRepository: FlytJobbRepository,
    private val mellomlagretVurderingRepository: MellomlagretVurderingRepository,
    private val resultatUtleder: ResultatUtleder
) : BehandlingSteg {
    private val log = LoggerFactory.getLogger(javaClass)
    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        if (kontekst.vurderingType == VurderingType.FØRSTEGANGSBEHANDLING && trukketSøknadService.søknadErTrukket(
                kontekst.behandlingId
            )
            || kontekst.vurderingType == VurderingType.REVURDERING && avbrytRevurderingService.revurderingErAvbrutt(
                kontekst.behandlingId
            )
        ) {
            return Fullført
        }

        val stegHistorikk = behandlingRepository.hentStegHistorikk(kontekst.behandlingId)
        val vedtakstidspunkt =
            stegHistorikk
                .find { it.steg() == StegType.FATTE_VEDTAK && it.status() == StegStatus.AVSLUTTER }
                ?.tidspunkt() ?: error("Forventet å finne et avsluttet fatte vedtak steg")

        val virkningstidspunkt = virkningstidspunktUtleder.utledVirkningsTidspunkt(kontekst.behandlingId)
        vedtakService.lagreVedtak(kontekst.behandlingId, vedtakstidspunkt, virkningstidspunkt)

        val tilkjentYtelseDto = utbetalingService.lagTilkjentYtelseForUtbetaling(kontekst.sakId, kontekst.behandlingId)
        if (tilkjentYtelseDto != null) {
            utbetal(kontekst, tilkjentYtelseDto)
        } else {
            log.info("Fant ikke tilkjent ytelse for behandingsref ${kontekst.behandlingId}. Virkningstidspunkt: $virkningstidspunkt.")
        }
        flytJobbRepository.leggTil(
            jobbInput = JobbInput(jobb = VarsleVedtakJobbUtfører).medPayload(kontekst.behandlingId)
                .forSak(kontekst.sakId.id)
        )

        mellomlagretVurderingRepository.slett(kontekst.behandlingId)

        lagGysOppgaveHvisRelevant(kontekst)

        return Fullført
    }

    private fun lagGysOppgaveHvisRelevant(kontekst: FlytKontekstMedPerioder) {
        val behandling = behandlingRepository.hent(behandlingId = kontekst.behandlingId)
        if (!resultatUtleder.erRentAvslag(behandling)) {
            opprettOppfølgingsoppgaveForNavkontorVedSosialRefusjon(kontekst)
        } else {
            log.info("Oppretter ikke gosysoppgave for sak ${kontekst.sakId} og behandling ${kontekst.behandlingId} , da AAP ikke er innvliget ")
        }
    }

    private fun opprettOppfølgingsoppgaveForNavkontorVedSosialRefusjon(kontekst: FlytKontekstMedPerioder) {
        val navkontorSosialRefusjon = refusjonkravRepository.hentHvisEksisterer(kontekst.behandlingId) ?: emptyList()
        if (navkontorSosialRefusjon.isNotEmpty()) {
            val forrigeIverksatteSosialRefusjonsVurderinger =
                kontekst.forrigeBehandlingId?.let { refusjonkravRepository.hentHvisEksisterer(it) } ?: emptyList()

            val gjeldendeSosialRefusjonDtoer = navkontorSosialRefusjon
                .filter { it.harKrav && it.navKontor != null }
                .map { it.tilNavKontorPeriodeDto() }
                .toSet()

            val forrigeSosialRefusjonDtoer = forrigeIverksatteSosialRefusjonsVurderinger
                .filter { it.harKrav && it.navKontor != null }
                .map { it.tilNavKontorPeriodeDto() }
                .toSet()

            val sosialRefusjonerSomManglerOppgave = gjeldendeSosialRefusjonDtoer - forrigeSosialRefusjonDtoer

            log.info("Fant ${sosialRefusjonerSomManglerOppgave.size} refusjonskrav som mangler oppgave")
            val aktivIdent = sakRepository.hent(kontekst.sakId).person.aktivIdent()

            opprettGosysOppgaverForSosialrefusjon(sosialRefusjonerSomManglerOppgave, aktivIdent, kontekst)
        }
    }

    private fun utbetal(
        kontekst: FlytKontekstMedPerioder,
        tilkjentYtelseDto: TilkjentYtelseDto
    ) {
        /**
         * Må opprette jobb med sakId, men uten behandlingId for at disse skal bli kjørt sekvensielt i riktig rekkefølge.
         * Viktig at eldste jobb kjøres først slik at utbetaling blir konsistent med Kelvin
         */
        flytJobbRepository.leggTil(
            jobbInput = JobbInput(jobb = IverksettUtbetalingJobbUtfører)
                .medPayload(kontekst.behandlingId)
                .forSak(sakId = kontekst.sakId.toLong())
        )
    }

    private fun opprettGosysOppgaverForSosialrefusjon(
        navKontorerSomSkalHaOppgave: Set<NavKontorPeriodeDto>,
        aktivIdent: Ident,
        kontekst: FlytKontekstMedPerioder
    ) {
        navKontorerSomSkalHaOppgave.forEach { navKontor ->
            // TODO: Opprett egen jobb-kjøring for å opprette gosysoppgave
            if (navKontor.enhetsNummer.isNotEmpty()) {
                log.info("Oppretter Gosysoppgave for $navKontor")
                gosysService.opprettOppgave(
                    aktivIdent,
                    kontekst.behandlingId.toString(),
                    kontekst.behandlingId,
                    navKontor
                )
            }
        }
    }


    companion object : FlytSteg {
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): BehandlingSteg {
            val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
            val sakRepository = repositoryProvider.provide<SakRepository>()
            val refusjonkravRepository = repositoryProvider.provide<RefusjonkravRepository>()
            val vedtakRepository = repositoryProvider.provide<VedtakRepository>()
            val flytJobbRepository = repositoryProvider.provide<FlytJobbRepository>()
            val gosysService = GosysService(gatewayProvider)
            val virkningstidspunktUtlederService = VirkningstidspunktUtleder(
                vilkårsresultatRepository = repositoryProvider.provide(),
            )
            val mellomlagretVurderingRepository = repositoryProvider.provide<MellomlagretVurderingRepository>()
            val resultatUtleder = ResultatUtleder(repositoryProvider)
            return IverksettVedtakSteg(
                sakRepository = sakRepository,
                refusjonkravRepository = refusjonkravRepository,
                behandlingRepository = behandlingRepository,
                utbetalingService = UtbetalingService(
                    repositoryProvider = repositoryProvider,
                    gatewayProvider = gatewayProvider
                ),
                vedtakService = VedtakService(vedtakRepository, behandlingRepository),
                virkningstidspunktUtleder = virkningstidspunktUtlederService,
                trukketSøknadService = TrukketSøknadService(repositoryProvider),
                avbrytRevurderingService = AvbrytRevurderingService(repositoryProvider),
                flytJobbRepository = flytJobbRepository,
                mellomlagretVurderingRepository = mellomlagretVurderingRepository,
                gosysService = gosysService,
                resultatUtleder = resultatUtleder
            )
        }

        override fun type(): StegType {
            return StegType.IVERKSETT_VEDTAK
        }
    }
}
