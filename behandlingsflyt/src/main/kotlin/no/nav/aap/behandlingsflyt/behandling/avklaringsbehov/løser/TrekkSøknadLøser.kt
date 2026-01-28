package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.TrekkSøknadLøsning
import no.nav.aap.behandlingsflyt.behandling.søknad.TrukketSøknadRepository
import no.nav.aap.behandlingsflyt.behandling.søknad.TrukketSøknadVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory
import java.time.Instant

class TrekkSøknadLøser(
    private val mottattDokumentRepository: MottattDokumentRepository,
    private val trekkSøknadRepository: TrukketSøknadRepository,
    private val behandlingRepository: BehandlingRepository,
    private val unleashGateway: UnleashGateway,
) : AvklaringsbehovsLøser<TrekkSøknadLøsning> {

    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        mottattDokumentRepository = repositoryProvider.provide(),
        trekkSøknadRepository = repositoryProvider.provide(),
        behandlingRepository = repositoryProvider.provide(),
        unleashGateway = gatewayProvider.provide(),
    )

    private val log = LoggerFactory.getLogger(javaClass)

    override fun løs(
        kontekst: AvklaringsbehovKontekst,
        løsning: TrekkSøknadLøsning
    ): LøsningsResultat {
        val behandling = behandlingRepository.hent(kontekst.behandlingId())

        require(behandling.typeBehandling() == TypeBehandling.Førstegangsbehandling) {
            "kan kun trekke søknader i førstegangsbehandling"
        }
        require(behandling.status() in listOf(Status.OPPRETTET, Status.UTREDES)) {
            "kan kun trekke søknader som utredes"
        }

        val søknader = mottattDokumentRepository.hentDokumenterAvType(kontekst.behandlingId(), InnsendingType.SØKNAD)
        if (søknader.isEmpty()) {
            if (unleashGateway.isEnabled(
                    BehandlingsflytFeature.TrekkSoeknadOpprettetFraLegeerklaering,
                    kontekst.bruker.ident
                )
            ) {
                forsøkTrekkLegeerklæring(kontekst, løsning)
            } else {
                log.error(
                    "Prøver å trekke søknad, men det finnes ingen søknad knyttet til behandlingen. " +
                            "Sak=${kontekst.kontekst.sakId.id}, behandling=${kontekst.behandlingId().id}"
                )
            }
        } else {
            søknader.forEach { søknad ->
                trekkSøknadRepository.lagreTrukketSøknadVurdering(
                    kontekst.behandlingId(),
                    TrukketSøknadVurdering(
                        journalpostId = søknad.referanse.asJournalpostId,
                        begrunnelse = løsning.begrunnelse,
                        vurdertAv = kontekst.bruker,
                        skalTrekkes = løsning.skalTrekkes,
                        vurdert = Instant.now(),
                    )
                )
            }
        }
        return LøsningsResultat(løsning.begrunnelse)
    }

    /**
     * Midlertidig løsning for å tillate trekk av søknader som har blitt feilaktig opprettet pga. mottatt legeerklæring.
     **/
    private fun forsøkTrekkLegeerklæring(
        kontekst: AvklaringsbehovKontekst,
        løsning: TrekkSøknadLøsning
    ) {
        log.info("Ingen søknad funnet for sak ${kontekst.kontekst.sakId.id}. Forsøker å trekke legeerklæring i stedet for søknad.")

        val legeerklæring =
            mottattDokumentRepository.hentDokumenterAvType(kontekst.behandlingId(), InnsendingType.LEGEERKLÆRING)

        if (legeerklæring.isEmpty()) {
            log.error("Ingen legeerklæring funnet for søknad som skal trekkes. Sak=${kontekst.kontekst.sakId.id}, behandling=${kontekst.behandlingId().id}")
            return
        } else if (legeerklæring.size == 1) {
            val dokument = legeerklæring.single()

            trekkSøknadRepository.lagreTrukketSøknadVurdering(
                kontekst.behandlingId(),
                TrukketSøknadVurdering(
                    journalpostId = dokument.referanse.asJournalpostId,
                    begrunnelse = løsning.begrunnelse,
                    vurdertAv = kontekst.bruker,
                    skalTrekkes = løsning.skalTrekkes,
                    vurdert = Instant.now(),
                )
            )
        } else {
            log.error("Flere legeerklæringer funnet for søknad som skal trekkes. Kan ikke bestemme hvilken som skal trekkes. Sak=${kontekst.kontekst.sakId.id}, behandling=${kontekst.behandlingId().id}")
        }
    }

    override fun forBehov(): Definisjon {
        return Definisjon.VURDER_TREKK_AV_SØKNAD
    }
}
