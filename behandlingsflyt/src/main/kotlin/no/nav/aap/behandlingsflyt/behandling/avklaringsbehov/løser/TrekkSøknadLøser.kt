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
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.verdityper.dokument.JournalpostId
import org.slf4j.LoggerFactory
import java.time.Instant

class TrekkSøknadLøser(
    private val mottattDokumentRepository: MottattDokumentRepository,
    private val trekkSøknadRepository: TrukketSøknadRepository,
    private val behandlingRepository: BehandlingRepository,
) : AvklaringsbehovsLøser<TrekkSøknadLøsning> {

    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        mottattDokumentRepository = repositoryProvider.provide(),
        trekkSøknadRepository = repositoryProvider.provide(),
        behandlingRepository = repositoryProvider.provide(),
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
        log.info(
            "Startet trekk av søknad (sakId=${kontekst.sakId()}, behandlingId=${kontekst.behandlingId()}). " +
                    "Fant ${søknader.size} søknader knyttet til behandlingen."
        )

        if (søknader.isEmpty()) {
            forsøkTrekkLegeerklæring(kontekst, løsning, behandling)
        } else {
            søknader.forEach { søknad ->
                log.info("Trekker søknad (sakId=${søknad.sakId}, behandlingId=${søknad.behandlingId})")
                trekkVurdering(søknad.referanse.asJournalpostId, kontekst, løsning)
            }
        }
        return LøsningsResultat(løsning.begrunnelse)
    }

    /**
     * Midlertidig løsning for å tillate trekk av søknader som har blitt feilaktig opprettet pga. mottatt legeerklæring.
     **/
    private fun forsøkTrekkLegeerklæring(
        kontekst: AvklaringsbehovKontekst,
        løsning: TrekkSøknadLøsning,
        behandling: Behandling
    ) {
        log.info("Ingen søknad funnet for sak ${kontekst.kontekst.sakId.id}. Forsøker å trekke legeerklæring i stedet for søknad.")

        val kanTrekkeLegeerklæring =
            behandling.årsakTilOpprettelse in listOf(
                ÅrsakTilOpprettelse.HELSEOPPLYSNINGER,
                ÅrsakTilOpprettelse.ANNET_RELEVANT_DOKUMENT
            )
                    && Vurderingsbehov.MOTTATT_LEGEERKLÆRING in behandling.vurderingsbehov().map { it.type }

        if (!kanTrekkeLegeerklæring) {
            log.error(
                "Kan ikke trekke søknad for (${kontekst.sakId()}). Kan kun trekke søknad hvis den er " +
                        "opprettet pga. helseopplysninger og har mottatt legeerklæring som vurderingsbehov"
            )
            return
        }

        val legeerklæringer =
            mottattDokumentRepository.hentDokumenterAvType(kontekst.behandlingId(), InnsendingType.LEGEERKLÆRING)

        if (legeerklæringer.isEmpty()) {
            log.error("Ingen legeerklæring funnet for søknad som skal trekkes (${kontekst.sakId()}, ${kontekst.behandlingId()})")
            return
        } else {
            // Plukker første mottatte legeerklæring for å trekke søknad
            val legeerklæring = legeerklæringer.minByOrNull { it.opprettetTid }!!

            log.info("Trekker søknad/legeerklæring (sakId=${legeerklæring.sakId}, behandlingId=${legeerklæring.behandlingId})")
            trekkVurdering(legeerklæring.referanse.asJournalpostId, kontekst, løsning)
        }
    }

    private fun trekkVurdering(
        journalpostId: JournalpostId,
        kontekst: AvklaringsbehovKontekst,
        løsning: TrekkSøknadLøsning,
    ) {
        trekkSøknadRepository.lagreTrukketSøknadVurdering(
            kontekst.behandlingId(),
            TrukketSøknadVurdering(
                journalpostId = journalpostId,
                begrunnelse = løsning.begrunnelse,
                vurdertAv = kontekst.bruker,
                skalTrekkes = løsning.skalTrekkes,
                vurdert = Instant.now(),
            )
        )
    }

    override fun forBehov(): Definisjon {
        return Definisjon.VURDER_TREKK_AV_SØKNAD
    }
}
