package no.nav.aap.behandlingsflyt.sakogbehandling.sak

import no.nav.aap.behandlingsflyt.behandling.Resultat
import no.nav.aap.behandlingsflyt.behandling.søknad.TrukketSøknadService
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.db.PersonRepository
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.repository.RepositoryProvider

class PersonOgSakService(
    private val pdlGateway: IdentGateway,
    private val personRepository: PersonRepository,
    private val sakRepository: SakRepository,
    private val behandlingRepository: BehandlingRepository,
    private val trukketSøknadService: TrukketSøknadService
) {


    constructor(repositoryProvider: RepositoryProvider): this (
        pdlGateway =  GatewayProvider.provide(IdentGateway::class),
        personRepository = repositoryProvider.provide(),
        sakRepository = repositoryProvider.provide(),
        behandlingRepository = repositoryProvider.provide(),
        trukketSøknadService = TrukketSøknadService(repositoryProvider)
    )

    fun finnEllerOpprett(ident: Ident, periode: Periode): Sak {

        val identliste = pdlGateway.hentAlleIdenterForPerson(ident)
        if (identliste.isEmpty()) {
            throw IllegalStateException("Fikk ingen treff på ident i PDL")
        }

        val person = personRepository.finnEllerOpprett(identliste)
        val saker = sakRepository.finnSakerFor(person, periode)
        saker.map()
        { sak ->

            val behandling = behandlingRepository.finnSisteBehandlingFor(
                sak.id,
                listOf(TypeBehandling.Førstegangsbehandling, TypeBehandling.Revurdering)
            )
            if (behandling != null) {
                if (trukketSøknadService.søknadErTrukket(behandling.id)) {
                    return sakRepository.finnEllerOpprett(person, periode)
                }
            }
        }

        return sakRepository.finnEllerOpprett(person, periode)
    }
}