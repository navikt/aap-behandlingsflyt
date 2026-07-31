package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.Aktør
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.ForeldrepengerGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.ForeldrepengerRequest
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.Ytelser
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.db.PersonRepository
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory
import java.time.LocalDate
import kotlin.time.measureTimedValue


class ForeldrepengeoppslagService(
    private val foreldrepengerGateway: ForeldrepengerGateway,
    private val personRepository: PersonRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        foreldrepengerGateway = gatewayProvider.provide<ForeldrepengerGateway>(),
        personRepository = repositoryProvider.provide<PersonRepository>(),
    )

    fun hentForeldrepengeperioder(personident: String, oppslagsperiode: Periode): List<ForeldrepengeUtbetaling> {
        val identer = personRepository.finnAlleIdenter(personident)

        val (perioder, duration) = measureTimedValue {
            identer
                .flatMap { ident -> hentForIdent(ident, oppslagsperiode) }
                .distinct()
        }
        log.info("Hentet foreldrepengeperioder for ${identer.size} ident(er). Tok ${duration.inWholeMilliseconds} ms")

        return perioder
    }

    private fun hentForIdent(ident: String, oppslagsperiode: Periode): List<ForeldrepengeUtbetaling> {
        val respons = foreldrepengerGateway.hentVedtakYtelseForPerson(
            ForeldrepengerRequest(Aktør(ident), oppslagsperiode)
        )

        return respons.ytelser
            .filter { it.ytelse == Ytelser.FORELDREPENGER }
            .flatMap { ytelse ->
                ytelse.anvist
                    .filter { oppslagsperiode.overlapper(it.periode) }
                    .map { anvist ->
                        ForeldrepengeUtbetaling(
                            periode = anvist.periode,
                            utbetalingsgrad = anvist.utbetalingsgrad.verdi,
                            beløp = anvist.beløp,
                            saksnummer = ytelse.saksnummer,
                            kildesystem = ytelse.kildesystem,
                            ytelseStatus = ytelse.ytelseStatus,
                            vedtattTidspunkt = ytelse.vedtattTidspunkt,
                        )
                    }
            }
    }
}

data class ForeldrepengeUtbetaling(
    val periode: Periode,
    val utbetalingsgrad: Number,
    val beløp: Number?,
    val saksnummer: String?,
    val kildesystem: String,
    val ytelseStatus: String,
    val vedtattTidspunkt: LocalDate,
)

