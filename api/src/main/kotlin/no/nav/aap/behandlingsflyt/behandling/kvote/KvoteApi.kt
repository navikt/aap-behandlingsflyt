package no.nav.aap.behandlingsflyt.behandling.kvote

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.response.respondWithStatus
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.HttpStatusCode
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.VirkningstidspunktUtleder
import no.nav.aap.behandlingsflyt.behandling.underveis.KvoteService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.getGrunnlag
import java.time.DayOfWeek
import java.time.LocalDate
import javax.sql.DataSource

val HELGEDAGER = arrayOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)

fun NormalOpenAPIRoute.kvoteApi(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry,
) {
    route("/api/kvote") {
        getGrunnlag<BehandlingReferanse, KvoteDto>(
            behandlingPathParam = BehandlingPathParam("referanse"),
            avklaringsbehovKode = Definisjon.AVKLAR_BARNETILLEGG.kode.toString()
        ) { req ->
            val respons: KvoteDto? = dataSource.transaction(readOnly = true) { connection ->
                val repositoryProvider = repositoryRegistry.provider(connection)
                val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                val behandling = BehandlingReferanseService(behandlingRepository).behandling(req)

                val vilkårsresultatRepository = repositoryProvider.provide<VilkårsresultatRepository>()
                val virkningstidspunktUtleder = VirkningstidspunktUtleder(vilkårsresultatRepository)
                val virkningstidspunkt = virkningstidspunktUtleder.utledVirkningsTidspunkt(behandling.id)

                val kvoterForBehandling = KvoteService().beregn(behandling.id)
                val ordinærKvote = kvoterForBehandling.ordinærkvote.asInt
                val bruktOrdinærkvote = virkningstidspunkt?.datesUntil(LocalDate.now())?.filter { it.dayOfWeek !in HELGEDAGER }?.count()?.toInt()

                KvoteDto(
                    OrdinærKvote(
                        kvote = ordinærKvote,
                        bruktKvote = bruktOrdinærkvote,
                        gjenværendeKvote = if (bruktOrdinærkvote != null) ordinærKvote - bruktOrdinærkvote else null,
                        senesteDatoForKvote = utledSenesteDatoForKvote(virkningstidspunkt, ordinærKvote),
                    ),
                    StudentKvote(
                        kvoteStartDato = virkningstidspunkt,
                        kvoteSluttDato = utledSenesteDatoForKvote(virkningstidspunkt, kvoterForBehandling.studentkvote.asInt)
                    )
                )
                null
            }

            if (respons == null) {
                respondWithStatus(HttpStatusCode.NoContent)
            } else {
                respond(respons)
            }
        }
    }
}

private fun utledSenesteDatoForKvote(startDato: LocalDate?, kvote: Int): LocalDate? {
    if (startDato == null) {
        return null
    }
    var gjenværendeDager = kvote
    var sluttDato = startDato
    while (gjenværendeDager > 0) {
        sluttDato = sluttDato?.plusDays(1)
        if (sluttDato?.dayOfWeek !in HELGEDAGER) gjenværendeDager--
    }
    return sluttDato
}
