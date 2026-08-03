package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.Aktør
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.ForeldrepengerGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.ForeldrepengerRequest
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.Ytelser
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.type.Periode
import org.slf4j.LoggerFactory
import java.time.LocalDate
import kotlin.time.measureTimedValue


class ForeldrepengeoppslagService(
    private val foreldrepengerGateway: ForeldrepengerGateway,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    constructor(gatewayProvider: GatewayProvider) : this(
        foreldrepengerGateway = gatewayProvider.provide<ForeldrepengerGateway>(),
    )

    /**
     * Fpabakus slår selv opp aktøren i PDL og finner data på tvers av alle identene
     * til personen, så det er nok å sende én ident.
     */
    fun hentForeldrepengeperioder(personident: String, oppslagsperiode: Periode): List<ForeldrepengeUtbetaling> {
        val (respons, duration) = measureTimedValue {
            foreldrepengerGateway.hentVedtakYtelseForPerson(
                ForeldrepengerRequest(Aktør(personident), oppslagsperiode)
            )
        }
        log.info("Hentet foreldrepengeperioder. Tok ${duration.inWholeMilliseconds} ms")

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
