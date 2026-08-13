package no.nav.aap.behandlingsflyt.sakogbehandling.sak

import no.nav.aap.behandlingsflyt.hendelse.datadeling.ApiInternGateway
import no.nav.aap.behandlingsflyt.hendelse.datadeling.ArenaSakOppsummering
import no.nav.aap.behandlingsflyt.hendelse.datadeling.ArenaStatusResponse
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.db.PersonRepository
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalDateTime

class PersonOgSakService(
    private val pdlGateway: IdentGateway,
    private val apiInternGateway: ApiInternGateway,
    private val personRepository: PersonRepository,
    private val sakRepository: SakRepository,
    private val arenaMigreringRepository: ArenaMigreringRepository,
) {
    constructor(
        gatewayProvider: GatewayProvider,
        repositoryProvider: RepositoryProvider
    ) : this(
        gatewayProvider.provide<IdentGateway>(),
        gatewayProvider.provide<ApiInternGateway>(),
        repositoryProvider.provide<PersonRepository>(),
        repositoryProvider.provide<SakRepository>(),
        repositoryProvider.provide<ArenaMigreringRepository>()
    )

    private val log = LoggerFactory.getLogger(javaClass)

    fun finnEllerOpprett(ident: Ident, søknadsdato: LocalDate): Sak {
        val identliste = pdlGateway.hentAlleIdenterForPerson(ident)
        require(identliste.isNotEmpty()) { "Fikk ingen treff på ident i PDL" }

        rapporterHvisOppretterPersonSomFinnesIArena(identliste)
        val person = personRepository.finnEllerOpprett(identliste)

        return sakRepository.finnEllerOpprett(person, søknadsdato)
    }

    fun opprettSakMedArenaMigrering(ident: Ident, søknadsdato: LocalDate, saksnummerArena: String): Sak {
        val sak = finnEllerOpprett(ident, søknadsdato)
        arenaMigreringRepository.lagre(
            ArenaMigrering(
                sakId = sak.id,
                saksnummerArena = saksnummerArena,
                ident = ident.identifikator,
                migrertTidspunkt = LocalDateTime.now(),
            )
        )
        return sak
    }

    private fun rapporterHvisOppretterPersonSomFinnesIArena(identliste: List<Ident>) {
        val personFinnesIKelvin = personRepository.finn(identliste) != null
        val arenaStatus: ArenaStatusResponse? = apiInternGateway.hentArenaStatus(
            identliste.map { it.identifikator }.toSet()
        ).getOrNull()
        val personFinnesIArena = arenaStatus?.harArenaHistorikk == true
        if (!personFinnesIKelvin && personFinnesIArena) {
            log.info("Oppretter person som har historikk i AAP-Arena i Kelvin")
        }
    }

    fun finnArenasakForBruker(ident: Ident, saksnummerArena: String): ArenaSakOppsummering? {
        val saker = apiInternGateway.hentSakerForPerson(ident.identifikator).saker
        return saker.find { "${it.aar}-${it.lopenummer}" == saksnummerArena }
    }

    fun finnSakerFor(ident: Ident): List<Sak> {
        val identliste = pdlGateway.hentAlleIdenterForPerson(ident)
        require(identliste.isNotEmpty()) { "Fikk ingen treff på ident i PDL" }

        val person = personRepository.finnEllerOpprett(identliste)

        return sakRepository.finnSakerFor(person.id)
    }
}