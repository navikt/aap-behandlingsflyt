package no.nav.aap.behandlingsflyt.behandling.klage.andreinstans

import no.nav.aap.behandlingsflyt.behandling.ansattinfo.AnsattInfoService
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.dokument.KlagedokumentInformasjonUtleder
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.fullmektig.FullmektigRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.klagebehandling.nay.KlagebehandlingNayRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.resultat.KlageresultatUtleder
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Status
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.miljo.Miljø
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory

class AndreinstansService(
    private val klageresultatUtleder: KlageresultatUtleder,
    private val andreinstansGateway: AndreinstansGateway,
    private val behandlingRepository: BehandlingRepository,
    private val sakRepository: SakRepository,
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val klagebehandlingNayRepository: KlagebehandlingNayRepository,
    private val klagebehandlingKontorRepository: KlagebehandlingNayRepository,
    private val ansattInfoService: AnsattInfoService,
    private val klagedokumentInformasjonUtleder: KlagedokumentInformasjonUtleder,
    private val fullmektigRepository: FullmektigRepository
) {

    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        klageresultatUtleder = KlageresultatUtleder(repositoryProvider),
        andreinstansGateway = gatewayProvider.provide(),
        behandlingRepository = repositoryProvider.provide(),
        sakRepository = repositoryProvider.provide(),
        avklaringsbehovRepository = repositoryProvider.provide(),
        klagebehandlingNayRepository = repositoryProvider.provide(),
        klagebehandlingKontorRepository = repositoryProvider.provide(),
        ansattInfoService = AnsattInfoService(gatewayProvider),
        klagedokumentInformasjonUtleder = KlagedokumentInformasjonUtleder(repositoryProvider),
        fullmektigRepository = repositoryProvider.provide()
    )

    private val log = LoggerFactory.getLogger(javaClass)

    fun oversendTilAndreinstans(klageBehandlingId: BehandlingId) {
        val klageBehandling = behandlingRepository.hent(klageBehandlingId)
        val sak = sakRepository.hent(klageBehandling.sakId)
        val klageresultat = klageresultatUtleder.utledKlagebehandlingResultat(klageBehandlingId)
        val avklarinsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(klageBehandlingId)
        val sisteSaksbehandlersEnhet = utledEnhetForSisteSaksbehandler(avklarinsbehovene)
        requireNotNull(sisteSaksbehandlersEnhet) {
            "Fant ikke beslutters enhet"
        }

        val kravdato = klagedokumentInformasjonUtleder.utledKravMottattDatoForKlageBehandling(klageBehandlingId)
            ?: throw IllegalStateException("Kravdato for klagebehandling $klageBehandlingId er ikke definert")
        
        
        val fullmektig = fullmektigRepository.hentHvisEksisterer(klageBehandlingId)

        val klagebehandlingNay = klagebehandlingNayRepository.hentHvisEksisterer(klageBehandlingId)
        val klagebehandlingKontor = klagebehandlingKontorRepository.hentHvisEksisterer(klageBehandlingId)

        val kommentarBuilder = StringBuilder()
        klagebehandlingKontor?.vurdering?.notat?.let {
            kommentarBuilder.append("Kommentar fra kontor:\n$it")
            if (klagebehandlingNay?.vurdering?.notat != null) {
                kommentarBuilder.append("\n\n")
            }
        }
        klagebehandlingNay?.vurdering?.notat?.let { kommentarBuilder.append("Kommentar fra NAY:\n$it") }

        andreinstansGateway.oversendTilAndreinstans(
            saksnummer = sak.saksnummer,
            behandlingsreferanse = klageBehandling.referanse,
            kravDato = kravdato,
            klagenGjelder = sak.person,
            klageresultat = klageresultat,
            saksbehandlersEnhet = sisteSaksbehandlersEnhet,
            kommentar = kommentarBuilder.toString(),
            fullmektig = fullmektig?.vurdering
        )
    }

    private fun utledEnhetForSisteSaksbehandler(avklarinsbehovene: Avklaringsbehovene): String? {
        if (Miljø.erLokal()) {
            return "0300"
        }
        val sisteSaksbehandler = utledSisteSaksbehandler(avklarinsbehovene)
        val enhet = if (Miljø.erDev()) {
            log.info("Siste saksbehandler i saken er $sisteSaksbehandler")
            "0300" // Det finnes ikke testdata i NOM - bruker hardkodet enhet i dev
        } else {
            ansattInfoService.hentAnsattEnhet(sisteSaksbehandler)
        }
        return enhet
    }

    private fun utledSisteSaksbehandler(avklaringsbehovene: Avklaringsbehovene): String {
        return avklaringsbehovene.alle().mapNotNull { avklaringsbehov ->
            avklaringsbehov.historikk.filter { it.endretAv.erNavIdent() && it.status == Status.AVSLUTTET }.maxOrNull()
        }.max().endretAv
    }

    private fun String.erNavIdent(): Boolean {
        return this.matches(Regex("\\w\\d{6}"))
    }

}