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

fun NormalOpenAPIRoute.kvoteApi(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry,
) {
    route("/api/seneste-kvotedato") {
        getGrunnlag<BehandlingReferanse, KvoteDto>(
            behandlingPathParam = BehandlingPathParam("referanse"),
            avklaringsbehovKode = Definisjon.AVKLAR_BARNETILLEGG.kode.toString()
        ) { req ->
            val respons: KvoteDto? = dataSource.transaction(readOnly = true) { connection ->
                val repositoryProvider = repositoryRegistry.provider(connection)
                val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                val behandling = BehandlingReferanseService(behandlingRepository).behandling(req)

                if (behandling.status().erAvsluttet()) {
                    val vilkårsresultatRepository = repositoryProvider.provide<VilkårsresultatRepository>()
                    val virkningstidspunktUtleder = VirkningstidspunktUtleder(vilkårsresultatRepository)

                    val kvoterForBehandling = KvoteService().beregn(behandling.id)
                    val antallHverdagerIKvote = kvoterForBehandling.ordinærkvote.asInt
                    val startdatoForYtelse = virkningstidspunktUtleder.utledVirkningsTidspunkt(behandling.id)

                    if (startdatoForYtelse != null) {
                        KvoteDto(utledDatoEtterHverdager(startdatoForYtelse, antallHverdagerIKvote))
                    }
                }
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

private fun utledDatoEtterHverdager(dato: LocalDate, antallHverdager: Int): LocalDate {
    val helgedager = arrayOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
    var gjenværendeDager = antallHverdager
    var sluttDato = dato
    while (gjenværendeDager > 0) {
        sluttDato = sluttDato.plusDays(1)
        if (sluttDato.dayOfWeek !in helgedager) gjenværendeDager--
    }
    return sluttDato
}
