package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.ÅrsakTilSettPåVent
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklaringsbehovLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.PeriodisertAvklaringsbehovLøsning
import no.nav.aap.behandlingsflyt.behandling.mellomlagring.MellomlagretVurderingRepository
import no.nav.aap.behandlingsflyt.flyt.FlytOrkestrator
import no.nav.aap.behandlingsflyt.hendelse.avløp.BehandlingHendelseService
import no.nav.aap.behandlingsflyt.hendelse.avløp.BehandlingHendelseServiceImpl
import no.nav.aap.behandlingsflyt.hendelse.mottak.BehandlingSattPåVent
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.prosessering.ProsesserBehandlingService
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.Period

class AvklaringsbehovOrkestrator(
    private val repositoryProvider: RepositoryProvider,
    private val behandlingHendelseService: BehandlingHendelseService,
    private val flytOrkestrator: FlytOrkestrator,
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val behandlingRepository: BehandlingRepository,
    private val prosesserBehandling: ProsesserBehandlingService,
    private val gatewayProvider: GatewayProvider,
    private val mellomlagretVurderingRepository: MellomlagretVurderingRepository,
) {
    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        repositoryProvider = repositoryProvider,
        behandlingHendelseService = BehandlingHendelseServiceImpl(repositoryProvider),
        flytOrkestrator = FlytOrkestrator(repositoryProvider, gatewayProvider),
        avklaringsbehovRepository = repositoryProvider.provide(),
        behandlingRepository = repositoryProvider.provide(),
        prosesserBehandling = ProsesserBehandlingService(repositoryProvider, gatewayProvider),
        gatewayProvider = gatewayProvider,
        mellomlagretVurderingRepository = repositoryProvider.provide(),
    )

    private val log = LoggerFactory.getLogger(javaClass)

    fun løsAvklaringsbehovOgFortsettProsessering(
        behandlingId: BehandlingId,
        avklaringsbehovLøsning: AvklaringsbehovLøsning,
        bruker: Bruker
    ) {
        val definisjon = avklaringsbehovLøsning.definisjon()
        val behandling = behandlingRepository.hent(behandlingId)
        val kontekst = behandling.flytKontekst()
        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(behandling.id)

        log.info("Forsøker å løse avklaringsbehov[$definisjon] på behandling[${behandling.referanse}]")
        avklaringsbehovene.validerTilstand(behandling, definisjon)
        if (avklaringsbehovLøsning is PeriodisertAvklaringsbehovLøsning<*>) {
            avklaringsbehovene.validerPerioder(avklaringsbehovLøsning, kontekst, repositoryProvider)
        }

        // løses det behov som fremtvinger tilbakehopp?
        flytOrkestrator.forberedLøsingAvBehov(definisjon, behandling, kontekst, bruker)

        // Bør ideelt kalle på
        log.info("Mottok løsning for avklaringsbehov $definisjon.")
        val løsningsResultat = avklaringsbehovLøsning.løs(repositoryProvider, AvklaringsbehovKontekst(bruker, kontekst), gatewayProvider)
        avklaringsbehovene.løsAvklaringsbehov(
            avklaringsbehovLøsning.definisjon(),
            løsningsResultat.begrunnelse,
            bruker.ident,
            løsningsResultat.kreverToTrinn
        )
        log.info("Løste avklaringsbehov[$definisjon] på behandling[${behandling.referanse}]")

        prosesserBehandling.triggProsesserBehandling(behandling)
        mellomlagretVurderingRepository.slett(behandlingId, definisjon.kode)
    }

    fun settBehandlingPåVent(behandlingId: BehandlingId, hendelse: BehandlingSattPåVent) {
        val behandling = behandlingRepository.hent(behandlingId)

        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(behandlingId)
        avklaringsbehovene.validerTilstand(behandling = behandling)

        avklaringsbehovene.leggTil(
            definisjon = Definisjon.MANUELT_SATT_PÅ_VENT,
            funnetISteg = behandling.aktivtSteg(),
            frist = hendelse.frist,
            begrunnelse = hendelse.begrunnelse,
            grunn = hendelse.grunn,
            bruker = hendelse.bruker,
            perioderVedtaketBehøverVurdering = null,
            perioderSomIkkeErTilstrekkeligVurdert = null
        )

        avklaringsbehovene.validerTilstand(behandling = behandling)
        avklaringsbehovene.validerPlassering(behandling = behandling)
        behandlingHendelseService.stoppet(behandling, avklaringsbehovene)
    }

    fun settPåVentMensVentePåMedisinskeOpplysninger(behandlingId: BehandlingId, bruker: Bruker) {
        val behandling = behandlingRepository.hent(behandlingId)

        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(behandlingId)
        avklaringsbehovene.validerTilstand(behandling = behandling)

        avklaringsbehovene.leggTil(
            definisjon = Definisjon.BESTILL_LEGEERKLÆRING,
            funnetISteg = behandling.aktivtSteg(),
            grunn = ÅrsakTilSettPåVent.VENTER_PÅ_MEDISINSKE_OPPLYSNINGER,
            bruker = bruker,
            frist = LocalDate.now() + Period.ofWeeks(4),
            perioderVedtaketBehøverVurdering = null,
            perioderSomIkkeErTilstrekkeligVurdert = null
        )
        avklaringsbehovene.validerTilstand(behandling = behandling)
        avklaringsbehovene.validerPlassering(behandling = behandling)

        behandlingHendelseService.stoppet(behandling, avklaringsbehovene)
    }
}
