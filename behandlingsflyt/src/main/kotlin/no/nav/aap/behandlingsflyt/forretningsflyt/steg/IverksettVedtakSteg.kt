package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.ResultatUtleder
import no.nav.aap.behandlingsflyt.behandling.avbrytrevurdering.AvbrytRevurderingService
import no.nav.aap.behandlingsflyt.behandling.gosysoppgave.GosysService
import no.nav.aap.behandlingsflyt.behandling.mellomlagring.MellomlagretVurderingRepository
import no.nav.aap.behandlingsflyt.behandling.søknad.TrukketSøknadService
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.VirkningstidspunktUtleder
import no.nav.aap.behandlingsflyt.behandling.utbetaling.UtbetalingService
import no.nav.aap.behandlingsflyt.behandling.vedtak.Vedtak
import no.nav.aap.behandlingsflyt.behandling.vedtak.VedtakRepository
import no.nav.aap.behandlingsflyt.behandling.vedtak.VedtakService
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.refusjonkrav.NavKontorPeriodeDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.refusjonkrav.RefusjonkravRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.prosessering.IverksettUtbetalingJobbUtfører
import no.nav.aap.behandlingsflyt.prosessering.VarsleVedtakJobbUtfører
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.StegStatus
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.math.min

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
    private val resultatUtleder: ResultatUtleder,
    private val vedtakRepository: VedtakRepository,
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

        /** Denne lagringen skjer nå i `FatteVedtakSteg`, siden det er i det steget
         * at vedtaket fattes. Det kan i prinsippet være åpne behandlinger som
         * er forbi `FatteVedtakSteg` men som ikke har fullført `IverksettVedtakSteg`,
         * så vi lagrer her også til vi er sikre på at ingen behandlinger faller mellom to stoler.
         */

        val stegHistorikk = behandlingRepository.hentStegHistorikk(kontekst.behandlingId)
        val vedtakstidspunkt = stegHistorikk
            .find { it.steg() == StegType.FATTE_VEDTAK && it.status() == StegStatus.AVSLUTTER }
            ?.tidspunkt()
            ?: error("Forventet å finne et avsluttet fatte vedtak steg")
        lagreVedtak(kontekst ,vedtakstidspunkt )

        val virkningstidspunkt = virkningstidspunktUtleder.utledVirkningsTidspunkt(kontekst.behandlingId)


        val tilkjentYtelseDto = utbetalingService.lagTilkjentYtelseForUtbetaling(kontekst.sakId, kontekst.behandlingId)
        if (tilkjentYtelseDto != null) {
            utbetal(kontekst)
        } else {
            log.info("Fant ikke tilkjent ytelse for behandingsref ${kontekst.behandlingId}. Virkningstidspunkt: $virkningstidspunkt.")
        }
        flytJobbRepository.leggTil(
            jobbInput = JobbInput(jobb = VarsleVedtakJobbUtfører).medPayload(kontekst.behandlingId)
                .forSak(kontekst.sakId.id)
        )

        mellomlagretVurderingRepository.slett(kontekst.behandlingId)

        lagGysOppgaveHvisRelevant(kontekst, vedtaksDato = vedtakstidspunkt.toLocalDate(), virkningsDato = virkningstidspunkt)

        return Fullført
    }

    private fun lagreVedtak(kontekst: FlytKontekstMedPerioder , vedtakstidspunkt: LocalDateTime) {
        if (vedtakRepository.hent(kontekst.behandlingId) != null) {
            /* Vedtak lagret i `FatteVedtakSteg`, så ikke noe å gjøre her. */
            return
        }
        val virkningstidspunkt = virkningstidspunktUtleder.utledVirkningsTidspunkt(kontekst.behandlingId)
        vedtakService.lagreVedtak(kontekst.behandlingId, vedtakstidspunkt, virkningstidspunkt)
    }

    private fun lagGysOppgaveHvisRelevant(kontekst: FlytKontekstMedPerioder,vedtaksDato: LocalDate, virkningsDato: LocalDate?) {
        val behandling = behandlingRepository.hent(behandlingId = kontekst.behandlingId)
        if (!resultatUtleder.erRentAvslag(behandling)) {
            opprettOppfølgingsoppgaveForNavkontorVedSosialRefusjon(kontekst, behandling , vedtaksDato, virkningsDato)
        } else {
            log.info("Oppretter ikke gosysoppgave for sak ${kontekst.sakId} og behandling ${kontekst.behandlingId} , da AAP ikke er innvliget ")

        }
    }


    private fun harInnvilgelseFraEnTidligereBehandling(behandling: Behandling, ): Boolean {
         val forrigeBehandlingId = behandling.forrigeBehandlingId
        if (forrigeBehandlingId == null) return false
        val vedtak = vedtakService.hentVedtakForYtelsesbehandling(forrigeBehandlingId) ?: error("Skal eksistere vedtak for behandling ${forrigeBehandlingId}")
        if (vedtak.virkningstidspunkt != null ) return true
        else {
            val forrigeBehandling = behandlingRepository.hent(behandlingId = forrigeBehandlingId)
            return harInnvilgelseFraEnTidligereBehandling(forrigeBehandling)
        }

    }



    private fun finnTidligesteVedtakstidspunktFraTidligereBehandlinger(behandling: Behandling, vedtakstidspunkt: LocalDate): LocalDate {
        val forrigeBehandlingId = behandling.forrigeBehandlingId
            ?: return vedtakstidspunkt

        val vedtak = vedtakService.hentVedtakForYtelsesbehandling(forrigeBehandlingId)
        val forrigeVedtakstidspunkt = vedtak?.vedtakstidspunkt
        val nyTidligsteedtakstidspunkt =  if(vedtak?.vedtakstidspunkt != null) {
            minOf(forrigeVedtakstidspunkt.toLocalDate(), vedtakstidspunkt)
        } else vedtakstidspunkt
        val forrigeBehandling = behandlingRepository.hent(behandlingId = forrigeBehandlingId)
        return finnTidligesteVedtakstidspunktFraTidligereBehandlinger(forrigeBehandling,nyTidligsteedtakstidspunkt)
    }




    private fun finnVedtakMedTidligsteVirkningstidspunkt(behandling: Behandling): Vedtak? {
        val alleBehandling = behandlingRepository.hentAlleFor(behandling.sakId, TypeBehandling.ytelseBehandlingstyper())
        val vedtakPåBehandling = alleBehandling.mapNotNull {
            vedtakService.hentVedtakForYtelsesbehandling(it.id)
        }

        val vedtakMedVirkningstidspunkt = vedtakPåBehandling
            .filter { it.virkningstidspunkt != null }

        val tidligsteVirkningstidspunkt = vedtakMedVirkningstidspunkt
            .minByOrNull { it.virkningstidspunkt!!}?.virkningstidspunkt ?: error("Ingen vedtak med virkningstidspunkt funnet")

        val alleVedtakMedTidligsteVirkningstidspunkt = vedtakPåBehandling
            .filter { it.virkningstidspunkt == tidligsteVirkningstidspunkt }

        if (alleVedtakMedTidligsteVirkningstidspunkt.size == 1 && alleVedtakMedTidligsteVirkningstidspunkt.first().behandlingId == behandling.id) {
            return alleVedtakMedTidligsteVirkningstidspunkt.first()
        }

        return null

    }



    private fun finnTidligesteVirkningstidspunktFraTidligereBehandlinger(behandling: Behandling, VedtakMedTidligsteVirkningstidspunkt: Vedtak): Vedtak? {

        val forrigeBehandlingId = behandling.forrigeBehandlingId
            ?: return VedtakMedTidligsteVirkningstidspunkt

        val forrigevedtak = vedtakService.hentVedtakForYtelsesbehandling(forrigeBehandlingId)
        val nyTidligsteVirkingsTidspunkt = listOfNotNull(forrigevedtak, VedtakMedTidligsteVirkningstidspunkt)
            .filter { it.virkningstidspunkt != null }
            .minByOrNull { it.virkningstidspunkt!! }
            ?: VedtakMedTidligsteVirkningstidspunkt
        val forrigeBehandling = behandlingRepository.hent(behandlingId = forrigeBehandlingId)
        return finnTidligesteVirkningstidspunktFraTidligereBehandlinger(forrigeBehandling,nyTidligsteVirkingsTidspunkt)
    }

     fun opprettOppfølgingsoppgaveForNavkontorVedSosialRefusjon(kontekst: FlytKontekstMedPerioder,behandling: Behandling , vedtaksDato: LocalDate, virkningsDato: LocalDate?) {

        val navkontorSosialRefusjon = refusjonkravRepository.hentHvisEksisterer(kontekst.behandlingId) ?: emptyList()

        if (navkontorSosialRefusjon.isNotEmpty()) {
            /**
             * Sjekk om det er vedtaksdato er etter virkningsdato, hvis ikke return
             * Sjekk om det er en tidligere vedtatt virkningsdato en nåværende, hvis ikke, bruk nåværende
             * Hvis ingen
             *
             * Hvis førstegangs invilgelse med etterbetaling, opprett oppgave
             * Hvis ikke førstegangs invilgelse, sjekk om det er en tidligere invilgelse med refusjonskrav, hvis ja og virkningstidspunkted er tidligere enn det forrige, opprett oppgave
             * Bruk vedtakstidspunkt som er på første invilgelse
             */


            val erInnvilgetMedEtterbetaling = virkningsDato != null && virkningsDato < vedtaksDato

            if (!erInnvilgetMedEtterbetaling) {
                log.info("")
                return

            }

           //hvis det tidligste virkingstidpunktet i denne behandlignen, så må den lage refusjonskrav


            val vedtakMedTidligsteVirkingsdato = finnVedtakMedTidligsteVirkningstidspunkt(behandling)


            if (vedtakMedTidligsteVirkingsdato?.virkningstidspunkt == null)
            {
                log.info("Tidligste virkningsdato er i en tidligere eller sametiding behandling enn ${kontekst.behandlingId}, så ingen oppgave opprettes")
                return
            }


            val tidligsteVedtaksTidspunkt = minOf( finnTidligesteVedtakstidspunktFraTidligereBehandlinger(behandling, vedtaksDato),vedtaksDato )

            val gjeldendeSosialRefusjonDtoer = navkontorSosialRefusjon
                .filter { it.harKrav && it.navKontor != null }
                .map { it.tilNavKontorPeriodeDto(virkningsdato = vedtakMedTidligsteVirkingsdato.virkningstidspunkt, vedtaksdato = tidligsteVedtaksTidspunkt) }
                .toSet()

            log.info("Fant ${gjeldendeSosialRefusjonDtoer.size} refusjonskrav som skal få oppgave")
            val aktivIdent = sakRepository.hent(kontekst.sakId).person.aktivIdent()

            opprettGosysOppgaverForSosialrefusjon(gjeldendeSosialRefusjonDtoer, aktivIdent, kontekst)
        }
    }

    private fun utbetal(kontekst: FlytKontekstMedPerioder) {
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
                resultatUtleder = resultatUtleder,
                vedtakRepository = repositoryProvider.provide(),
            )
        }

        override fun type(): StegType {
            return StegType.IVERKSETT_VEDTAK
        }
    }
}
