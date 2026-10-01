package no.nav.aap.behandlingsflyt.hendelse.mottak

import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottaDokumentService
import no.nav.aap.behandlingsflyt.hendelse.datadeling.ApiInternGateway
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.prosessering.MeldekortGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.IdentGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.db.PersonRepository
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory

class HåndterFolkeregisterIdentHendelseService(
    private val sakRepository: SakRepository,
    private val personRepository: PersonRepository,
    private val behandlingService: BehandlingService,
    private val mottaDokumentService: MottaDokumentService,
    private val identGateway: IdentGateway,
    private val meldekortGateway: MeldekortGateway,
    private val apiInternGateway: ApiInternGateway,
) {

    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        sakRepository = repositoryProvider.provide(),
        personRepository = repositoryProvider.provide(),
        behandlingService = BehandlingService(repositoryProvider, gatewayProvider),
        mottaDokumentService = MottaDokumentService(repositoryProvider),
        identGateway = gatewayProvider.provide(),
        meldekortGateway = gatewayProvider.provide(),
        apiInternGateway = gatewayProvider.provide(),
    )

    private val log = LoggerFactory.getLogger(javaClass)

    fun håndterOppdatertFolkeregisterIdentForSak(
        sakId: SakId,
        referanse: InnsendingReferanse,
    ) {
        val sak = sakRepository.hent(sakId)
        val identliste = identGateway.hentAlleIdenterForPerson(sak.person.aktivIdent())
        val nyAktivIdent = identliste.find { it.aktivIdent }
        val harOppdatertIdent = sak.person.aktivIdent() != nyAktivIdent
        val oppdatertPerson = if (harOppdatertIdent) {
            log.info("Oppdaterer person med id ${sak.person.id}")
            personRepository.oppdaterIdenter(sak.person, identliste)
        } else {
            sak.person
        }

        log.info("Oppdaterer meldekort og api-intern med nye identer for sak ${sak.saksnummer}")
        meldekortGateway.oppdaterIdenter(saksnummer = sak.saksnummer, identer = oppdatertPerson.identer())
        apiInternGateway.oppdaterIdenter(sak.saksnummer, oppdatertPerson.identer())

        behandlingService.finnSisteYtelsesbehandlingFor(sakId)?.let {
            mottaDokumentService.markerSomBehandlet(sakId, it.id, referanse)
        }

    }
}


