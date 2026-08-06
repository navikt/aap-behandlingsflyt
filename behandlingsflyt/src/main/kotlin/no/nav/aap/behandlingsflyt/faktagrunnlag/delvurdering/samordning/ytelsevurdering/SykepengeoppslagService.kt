package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.SykepengerGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.UtbetaltePerioder
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.type.Periode
import org.slf4j.LoggerFactory
import kotlin.time.measureTimedValue


class SykepengeoppslagService(
    private val sykepengerGateway: SykepengerGateway,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    constructor(gatewayProvider: GatewayProvider) : this(
        sykepengerGateway = gatewayProvider.provide<SykepengerGateway>(),
    )

    fun hentSykepengeperioder(identer: Set<String>, oppslagsperiode: Periode): List<UtbetaltePerioder> {
        require(identer.isNotEmpty()) { "Må ha minst én ident å slå opp på" }

        val (perioder, duration) = measureTimedValue {
            sykepengerGateway.hentYtelseSykepenger(identer, oppslagsperiode.fom, oppslagsperiode.tom)
                .filter { oppslagsperiode.overlapper(Periode(it.fom, it.tom)) }
        }
        log.info("Hentet sykepengeperioder for ${identer.size} ident(er). Tok ${duration.inWholeMilliseconds} ms")

        return perioder
    }
}
