package no.nav.aap.behandlingsflyt.sakogbehandling.sak

import no.nav.aap.behandlingsflyt.hendelse.datadeling.ApiInternGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.db.PersonRepository
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory
import java.time.LocalDate

class PersonOgSakService(
    private val pdlGateway: IdentGateway,
    private val apiInternGateway: ApiInternGateway,
    private val personRepository: PersonRepository,
    private val sakRepository: SakRepository,
) {
    constructor(
        gatewayProvider: GatewayProvider,
        repositoryProvider: RepositoryProvider
    ) : this(
        gatewayProvider.provide<IdentGateway>(),
        gatewayProvider.provide<ApiInternGateway>(),
        repositoryProvider.provide<PersonRepository>(),
        repositoryProvider.provide<SakRepository>()
    )
    private val log = LoggerFactory.getLogger(javaClass)

    fun finnEllerOpprett(ident: Ident, søknadsdato: LocalDate): Sak {
        val identliste = pdlGateway.hentAlleIdenterForPerson(ident)
        require(identliste.isNotEmpty()) { "Fikk ingen treff på ident i PDL" }

        rapporterHvisOppretterPersonSomFinnesIArena(identliste)
        val person = personRepository.finnEllerOpprett(identliste)

        return sakRepository.finnEllerOpprett(person, søknadsdato)
    }

    @Deprecated("Sluttdato for rettighetesperiode er alltid Tid.MAKS for nye/migrerte saker. Send kun med søknadsdato, med mindre du tester koden din for ikke-migrerte saker.")
    fun finnEllerOpprett(ident: Ident, periode: Periode): Sak {
        return finnEllerOpprett(ident, periode.fom)
    }

    private fun rapporterHvisOppretterPersonSomFinnesIArena(identliste: List<Ident>) {
        val personFinnesIKelvin = personRepository.finn(identliste) != null
        val personFinnesIArena = apiInternGateway.hentArenaStatus(
            identliste.map { it.identifikator }.toSet()
        ).harArenaHistorikk
        if (!personFinnesIKelvin && personFinnesIArena) {
            log.info("Oppretter person som har historikk i AAP-Arena i Kelvin")
        }
    }

    fun finnSakerFor(ident: Ident): List<Sak> {
        val identliste = pdlGateway.hentAlleIdenterForPerson(ident)
        require(identliste.isNotEmpty()) { "Fikk ingen treff på ident i PDL" }

        val person = personRepository.finnEllerOpprett(identliste)

        return sakRepository.finnSakerFor(person)
    }
}