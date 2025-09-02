package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid

import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepository
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.IdentGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.db.PersonRepository
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.repository.RepositoryProvider
import org.slf4j.LoggerFactory
import java.time.LocalDate

class BehandlingOgMeldekortServiceImpl(
    private val behandlingRepository: BehandlingRepository,
    private val meldekortRepository: MeldekortRepository,
    private val mottattDokumentRepository: MottattDokumentRepository,
    private val personOgSakService: PersonOgSakService
) : BehandlingOgMeldekortService {
    @Suppress("unused")
    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        behandlingRepository = repositoryProvider.provide<BehandlingRepository>(),
        meldekortRepository = repositoryProvider.provide<MeldekortRepository>(),
        mottattDokumentRepository = repositoryProvider.provide<MottattDokumentRepository>(),
        personOgSakService = PersonOgSakService(
            gatewayProvider.provide(IdentGateway::class),
            repositoryProvider.provide<PersonRepository>(),
            repositoryProvider.provide<SakRepository>(),
        )
    )

    companion object {
        private val log = LoggerFactory.getLogger(BehandlingOgMeldekortServiceImpl::class.java)
    }

    override fun hentAlle(ident: Ident, fraOgMed: LocalDate?): List<Pair<Behandling, List<Meldekort>>> {
        val alleSakerForIdent = try {
            personOgSakService.finnSakerFor(ident)
        } catch (e: IllegalStateException) {
            log.warn("Fant ikke person med ident='$ident' i PDL", e)
            emptyList()
        }
        return alleSakerForIdent.maxByOrNull { sak -> sak.opprettetTidspunkt }?.let { nyesteSak ->
                hentAlle(nyesteSak, fraOgMed)
            } ?: emptyList() // ingen saker finnes for denne ident
    }

    override fun hentAlle(sak: Sak, fraOgMed: LocalDate?): List<Pair<Behandling, List<Meldekort>>> {
        val detSisteHalvåret = LocalDate.now().minusMonths(6)
        val fraOgMedDato = fraOgMed ?: detSisteHalvåret

        val behandlingerMedMotatteMeldekort =
            mottattDokumentRepository.hentDokumenterAvType(sak.id, InnsendingType.MELDEKORT)
                .filter { it.mottattTidspunkt.isAfter(fraOgMedDato.atStartOfDay()) }
                // TODO Meldekort er initielt uten behandlingId når de først lagres i db.
                // Skal vi ignorere disse, og regne dem som ikke fullstendig mottatt?
                .mapNotNull { it.behandlingId }.toSet() // Det kan være flere meldekort på samme behandlingId?
                .map { id -> behandlingRepository.hent(id) }
                .sortedByDescending { behandling -> behandling.opprettetTidspunkt } // nyeste behandling først

        // TODO MottattDokument har også en status (Mottatt | Behandlet), skal vi bruke den til noe?
        // Vi kan også få tak i når meldeperiode_grunnlag ble opprettet, skal vi bruke den til noe?
        // Er dette samme tidspunkt som når meldekortet ble tilgjengelig for bruker?
        // Er det kanskje bare det samme som når behandlingen ble opprettet i databasen vår?

        return behandlingerMedMotatteMeldekort.map { behandling ->
                val meldekortGrunnlag = meldekortRepository.hent(behandling.id) // må finnes
                Pair(behandling, meldekortGrunnlag.meldekort())
            }

    }
}


