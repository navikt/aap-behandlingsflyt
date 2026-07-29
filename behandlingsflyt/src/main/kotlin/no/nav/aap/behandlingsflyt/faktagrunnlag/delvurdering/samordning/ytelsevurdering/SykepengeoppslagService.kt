package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.SykepengerGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.UtbetaltePerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.db.PersonRepository
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory
import kotlin.time.measureTimedValue

/**
 * Slår opp utbetalte sykepengeperioder for en person, inkludert historiske identer.
 *
 * Tjenesten gjør ingen vurdering av om perioder er "nære nok" i tid - den returnerer rådata
 * innenfor oppslagsperioden, slik at konsumenten selv kan avgjøre hva som er relevant.
 */
class SykepengeoppslagService(
    private val sykepengerGateway: SykepengerGateway,
    private val personRepository: PersonRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        sykepengerGateway = gatewayProvider.provide<SykepengerGateway>(),
        personRepository = repositoryProvider.provide<PersonRepository>(),
    )

    fun hentSykepengeperioder(personident: String, oppslagsperiode: Periode): List<UtbetaltePerioder> {
        // Sykepenger-gatewayen tar imot alle identene til personen i ett kall.
        val identer = personRepository.finnAlleIdenter(personident)

        val (perioder, duration) = measureTimedValue {
            sykepengerGateway.hentYtelseSykepenger(identer, oppslagsperiode.fom, oppslagsperiode.tom)
                .filter { oppslagsperiode.overlapper(Periode(it.fom, it.tom)) }
        }
        log.info("Hentet sykepengeperioder for ${identer.size} ident(er). Tok ${duration.inWholeMilliseconds} ms")

        return perioder
    }
}

