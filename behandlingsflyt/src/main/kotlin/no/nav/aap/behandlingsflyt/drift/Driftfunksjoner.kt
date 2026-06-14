package no.nav.aap.behandlingsflyt.drift

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehov
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovOrkestrator
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.SkrivBrevAvklaringsbehovLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.SkrivVedtaksbrevLøsning
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingReferanse
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingRepository
import no.nav.aap.behandlingsflyt.flyt.FlytOrkestrator
import no.nav.aap.behandlingsflyt.hendelse.datadeling.ApiInternGateway
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.prosessering.MeldekortGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.StegTilstand
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.StegStatus
import no.nav.aap.behandlingsflyt.sakogbehandling.lås.TaSkriveLåsRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.IdentGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.db.PersonRepository
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.komponenter.repository.RepositoryProvider
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.komponenter.verdityper.Tid
import org.slf4j.LoggerFactory

/**
 * Klasse for alle driftsfunksjoner. Skal kún brukes av DriftApi.
 * */
class Driftfunksjoner(
    private val behandlingRepository: BehandlingRepository,
    private val sakRepository: SakRepository,
    private val taSkriveLåsRepository: TaSkriveLåsRepository,
    private val flytOrkestrator: FlytOrkestrator,
    private val flytOrkestratorUtenSavepoints: FlytOrkestrator,
    private val brevbestillingRepository: BrevbestillingRepository,
    private val avklaringsbehovOrkestrator: AvklaringsbehovOrkestrator,
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val personRepository: PersonRepository,
    private val identGateway: IdentGateway,
    private val meldekortGateway: MeldekortGateway,
    private val apiInternGateway: ApiInternGateway,
    private val sakService: SakService,
) {
    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        sakRepository = repositoryProvider.provide(),
        behandlingRepository = repositoryProvider.provide(),
        taSkriveLåsRepository = repositoryProvider.provide(),
        flytOrkestrator = FlytOrkestrator(repositoryProvider, gatewayProvider),
        flytOrkestratorUtenSavepoints = FlytOrkestrator(
            repositoryProvider,
            gatewayProvider,
            markSavepointAt = emptySet()
        ),
        brevbestillingRepository = repositoryProvider.provide(),
        avklaringsbehovOrkestrator = AvklaringsbehovOrkestrator(repositoryProvider, gatewayProvider),
        avklaringsbehovRepository = repositoryProvider.provide(),
        personRepository = repositoryProvider.provide(),
        identGateway = gatewayProvider.provide(),
        meldekortGateway = gatewayProvider.provide(),
        apiInternGateway = gatewayProvider.provide(),
        sakService = SakService(repositoryProvider, gatewayProvider),
    )

    private val log = LoggerFactory.getLogger(javaClass)

    fun kjørFraSteg(behandling: Behandling, stegType: StegType) {
        taSkriveLåsRepository.withLåstBehandling(behandling.id) {

            when {
                behandling.status() != Status.UTREDES -> throw UgyldigForespørselException("kan ikke flytte steg i behandling: behandling må ha status ${Status.UTREDES}, men denne behandling har status ${behandling.status()}")

                stegType.status != Status.UTREDES -> throw UgyldigForespørselException("kan ikke flytte behandling til steg $stegType som hører til status ${stegType.status}. Kan kun flytte til steg med status ${Status.UTREDES}")

                stegType !in behandling.flyt()
                    .stegene() -> throw UgyldigForespørselException("kan ikke flytte behandling til steg $stegType, da steget ikke hører til flyten ${behandling.typeBehandling()}")

                behandling.flyt().stegComparator.compare(
                    stegType,
                    behandling.aktivtSteg()
                ) > 0 -> throw UgyldigForespørselException("kan ikke flytte behandling til steg $stegType siden steget er etter aktivt steg ${behandling.aktivtSteg()}")
            }

            log.info("setter aktivt steg fra ${behandling.aktivtSteg()} til $stegType")
            behandlingRepository.leggTilNyttAktivtSteg(
                behandling.id, StegTilstand(
                    stegType = stegType,
                    stegStatus = StegStatus.START,
                    aktiv = true,
                )
            )

            flytOrkestrator.prosesserBehandling(behandling.flytKontekst())
        }
    }

    fun avbrytVedtsaksbrevBestilling(
        bruker: Bruker,
        brevbestillingReferanse: BrevbestillingReferanse,
        begrunnelse: String
    ) {
        val bestilling = brevbestillingRepository.hent(brevbestillingReferanse)
            ?: throw UgyldigForespørselException("Fant ingen brevbestilling med referanse $brevbestillingReferanse")


        val behandling = behandlingRepository.hent(bestilling.behandlingId)
        taSkriveLåsRepository.withLåstBehandling(behandling.id) {
            avklaringsbehovOrkestrator.løsAvklaringsbehovOgFortsettProsessering(
                behandlingId = behandling.id,
                avklaringsbehovLøsning = SkrivVedtaksbrevLøsning(
                    brevbestillingReferanse = brevbestillingReferanse.brevbestillingReferanse,
                    handling = SkrivBrevAvklaringsbehovLøsning.Handling.AVBRYT,
                    mottakere = emptyList(),
                    behovstype = Definisjon.SKRIV_VEDTAKSBREV.kode,
                    begrunnelse = begrunnelse
                ),
                bruker = bruker,
            )
        }
    }

    fun utvidRettghetsperiodeOgKjørFraStart(behandling: Behandling) {
        taSkriveLåsRepository.withLåstBehandling(behandling.id) {
            val stegType = StegType.VURDER_LOVVALG
            val sak = sakRepository.hent(behandling.sakId)
            val avklaringsbehovFørEndring = avklaringsbehovRepository.hentAvklaringsbehovene(behandling.id).åpne()
            validerGyldigTilstandFørUtvidelseAvRettighetsperiode(behandling, stegType)

            log.info("Utvider rettighetsperiode til Tid.MAKS og setter aktivt steg fra ${behandling.aktivtSteg()} til $stegType")
            sakService.overstyrRettighetsperioden(sak, sak.rettighetsperiode.fom, Tid.MAKS)
            behandlingRepository.leggTilNyttAktivtSteg(
                behandling.id, StegTilstand(
                    stegType = stegType,
                    stegStatus = StegStatus.START,
                    aktiv = true,
                )
            )

            flytOrkestratorUtenSavepoints.prosesserBehandling(behandling.flytKontekst())

            val nyttAktivtSteg = behandlingRepository.hent(behandling.id).aktivtSteg()
            val avklaringsbehovEtterEndring = avklaringsbehovRepository.hentAvklaringsbehovene(behandling.id).åpne()
            validerGyldigTilstandEtterUtvidelseAvRettighetsperiode(
                nyttAktivtSteg,
                behandling,
                avklaringsbehovFørEndring,
                avklaringsbehovEtterEndring
            )
        }
    }

    fun oppdaterPersonIdenter(sak: Saksnummer) {
        val sak = sakRepository.hent(sak)
        val identliste = identGateway.hentAlleIdenterForPerson(sak.person.aktivIdent())
        check(identliste.isNotEmpty()) { "Fikk ingen treff på ident i PDL" }

        val nyeIdenter = identliste.filterNot {
            sak.person.identer().map(Ident::identifikator).contains(it.identifikator)
        }.also { nyeIdenter ->
            log.info(" ${nyeIdenter.size} ny(e) identer i PDL for sak.")
        }

        val erNyAktiv = sak.person.identer().find { it.aktivIdent } != identliste.find { it.aktivIdent }

        if (nyeIdenter.isNotEmpty() || erNyAktiv) {
            log.info(
                "Oppdaterer identer for person i sak ${sak.saksnummer} med ${nyeIdenter.size} ny(e) identer fra PDL." +
                        " Er aktiv ident endret: $erNyAktiv."
            )
        } else {
            log.info("Fant ingen nye identer eller ny aktiv ident i PDL for person i sak ${sak.saksnummer}.")
        }

        val person = personRepository.finnEllerOpprett(identliste)
        meldekortGateway.oppdaterIdenter(saksnummer = sak.saksnummer, identer = person.identer())
        apiInternGateway.oppdaterIdenter(sak.saksnummer, person.identer())
    }

    private fun validerGyldigTilstandFørUtvidelseAvRettighetsperiode(
        behandling: Behandling,
        stegType: StegType
    ) {
        when {
            behandling.status() != Status.UTREDES -> throw UgyldigForespørselException("kan ikke flytte steg i behandling: behandling må ha status ${Status.UTREDES}, men denne behandling har status ${behandling.status()}")
            stegType !in behandling.flyt()
                .stegene() -> throw UgyldigForespørselException("kan ikke flytte behandling til steg $stegType, da steget ikke hører til flyten ${behandling.typeBehandling()}")

            behandling.flyt().stegComparator.compare(
                stegType,
                behandling.aktivtSteg()
            ) > 0 -> throw UgyldigForespørselException("kan ikke flytte behandling til steg $stegType siden steget er etter aktivt steg ${behandling.aktivtSteg()}")
        }
    }

    private fun validerGyldigTilstandEtterUtvidelseAvRettighetsperiode(
        nyttAktivtSteg: StegType,
        behandling: Behandling,
        avklaringsbehovFørEndring: List<Avklaringsbehov>,
        avklaringsbehovEtterEndring: List<Avklaringsbehov>
    ) {
        if (nyttAktivtSteg != behandling.aktivtSteg()) {
            throw UgyldigForespørselException("Aktivt steg er ulikt etter utvidelse av rettighetsperiode - rull tilbake. Før=${behandling.aktivtSteg()} etter=$nyttAktivtSteg")
        }
        if (`avklaringsbehovFørEndring`.size != avklaringsbehovEtterEndring.size) {
            throw UgyldigForespørselException("Ulikt antall avklaringsbehov før og etter endring, ruller tilbake. Før=${`avklaringsbehovFørEndring`} etter=${avklaringsbehovEtterEndring}")
        }
        avklaringsbehovEtterEndring.forEach { etter ->
            val før = `avklaringsbehovFørEndring`.find { it.definisjon == etter.definisjon }
                ?: error("Fant ikke avklaringsbehov med definisjon ${etter.definisjon} fra avklaringsbehovene før endringen")
            if (før.status() != etter.status()) {
                throw UgyldigForespørselException("Ulik status på avklaringsbehov før og etter endring for ${etter.definisjon}, ruller tilbake. Før=${`avklaringsbehovFørEndring`} etter=${avklaringsbehovEtterEndring}")
            }
        }
    }
}