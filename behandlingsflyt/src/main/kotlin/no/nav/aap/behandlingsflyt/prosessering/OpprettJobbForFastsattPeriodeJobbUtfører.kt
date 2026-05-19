package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.motor.ProvidersJobbSpesifikasjon
import no.nav.aap.motor.cron.CronExpression

/** Denne jobben kan snart fjernes.
 * Formålet med jobben, var å stanse planlagte utbetalinger (`FREMTIDIG_OPPFYLT`).
 * Men vi har sluttet å planlegge utbetalinger under antagelse om at meldeplikten vil
 * være oppfylt.
 *
 * PR 19. mai finnes det i underkant av 300 gjeldende behandlinger med `FREMTIDIG_OPPFYLT`
 * i en underveisperiode:
 *
 * | fom | count |
 * | :--- | :--- |
 * | 2026-05-18 | 267 |
 *| 2026-05-25 | 5 |
 *| 2026-06-15 | 1 |
 *| 2026-06-22 | 1 |
 *| 2026-09-14 | 1 |
 *| 2026-12-07 | 1 |
 *| 2027-01-18 | 1 |
 *
 * ```postgres
 * with tidligste_antatte_dato as (select min(lower(underveis_periode.periode)) as fom
 *              from gjeldende_vedtatte_behandlinger
 *                       join underveis_grunnlag
 *                            on underveis_grunnlag.behandling_id = gjeldende_vedtatte_behandlinger.behandling_id
 *                       join underveis_perioder on underveis_grunnlag.perioder_id = underveis_perioder.id
 *                       join underveis_periode on underveis_perioder.id = underveis_periode.perioder_id
 *              where underveis_grunnlag.aktiv
 *                and underveis_periode.utfall = 'OPPFYLT'
 *                and underveis_periode.rettighetstype is not null
 *                and underveis_periode.meldeplikt_status = 'FREMTIDIG_OPPFYLT'
 *              group by gjeldende_vedtatte_behandlinger.behandling_id
 *              )
 *
 * select fom, count(*) from tidligste_antatte_dato group by fom
 * ```
 *
 * Det er natt til tirsdagen 8 dager etter fom-datoen over at denne jobben kjører
 * og vil fjerne `FREMTIDIG_OPPFYLT`.
 *
 * Må undersøke om det er trygt å fjerne denne jobben etter at de 267 behandlingene
 * er prosessert 26. mai 2026.
 */
class OpprettJobbForFastsattPeriodeJobbUtfører(
    private val flytJobbRepository: FlytJobbRepository,
    private val sakRepository: SakRepository,
) : JobbUtfører {

    override fun utfør(input: JobbInput) {
        sakRepository
            .finnAlleSakIder()
            .forEach {
                flytJobbRepository.leggTil(JobbInput(OpprettBehandlingFastsattPeriodePassertJobbUtfører).forSak(it.toLong()))
            }
    }


    companion object : ProvidersJobbSpesifikasjon {
        override fun konstruer(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider): JobbUtfører {
            return OpprettJobbForFastsattPeriodeJobbUtfører(
                flytJobbRepository = repositoryProvider.provide(),
                sakRepository = repositoryProvider.provide(),
            )
        }

        override val type = "batch.OpprettJobbForFastsattPeriode"

        override val navn = "Start jobb for å sjekke behov for revurdering pga manglende meldekort"

        override val beskrivelse = """
            Start jobb for å sjekke om fastsatt dager er passert.
            """.trimIndent()

        override val cron = CronExpression.createWithoutSeconds("10 2 * * 2")
    }
}
