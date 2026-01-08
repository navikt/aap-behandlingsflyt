package no.nav.aap.behandlingsflyt.behandling.kvote

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.response.respondWithStatus
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.HttpStatusCode
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.VirkningstidspunktUtleder
import no.nav.aap.behandlingsflyt.behandling.underveis.KvoteService
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Kvote
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Telleverk
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.StoppetBehandling
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
        getGrunnlag<BehandlingReferanse, List<KvoteDto>>(
            behandlingPathParam = BehandlingPathParam("referanse"),
            avklaringsbehovKode = Definisjon.AVKLAR_BARNETILLEGG.kode.toString()
        ) { req ->
            val respons: List<KvoteDto> = dataSource.transaction(readOnly = true) { connection ->
                val kvoteListe = mutableListOf<KvoteDto>()
                val repositoryProvider = repositoryRegistry.provider(connection)
                val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                val behandling = BehandlingReferanseService(behandlingRepository).behandling(req)

                val vilkårsresultatRepository = repositoryProvider.provide<VilkårsresultatRepository>()
                val virkningstidspunktUtleder = VirkningstidspunktUtleder(vilkårsresultatRepository)
                val virkningstidspunkt = virkningstidspunktUtleder.utledVirkningsTidspunkt(behandling.id)

                val kvoterForBehandling = KvoteService().beregn(behandling.id)
                kvoteListe.add(utledKvoteDto(virkningstidspunkt, kvoterForBehandling.ordinærkvote.asInt, Kvote.ORDINÆR))
                kvoteListe.add(utledKvoteDto(virkningstidspunkt, kvoterForBehandling.studentkvote.asInt, Kvote.STUDENT))
                kvoteListe.add(utledKvoteDto(virkningstidspunkt, kvoterForBehandling.sykepengeerstatningkvote.asInt, Kvote.SYKEPENGEERSTATNING))

                kvoteListe
            }
            respond(respons)
        }
    }
}

private fun utledKvoteDto(virkningstidspunkt: LocalDate?, antallHverdagerIKvote: Int, type: Kvote): KvoteDto {
    val senesteDatoForKvote = utledSenesteDatoForKvote(virkningstidspunkt, antallHverdagerIKvote)

    if (type == Kvote.STUDENT) {
        return KvoteDto(
            rettighetStartDato = virkningstidspunkt,
            rettighetEndDato = senesteDatoForKvote
        )
    }

    val bruktKvote = virkningstidspunkt?.datesUntil(LocalDate.now())?.filter { it.dayOfWeek !in HELGEDAGER }?.count()?.toInt()
    val stansdato = LocalDate.now()
    val opphørsdato = LocalDate.now()

    return KvoteDto(
        kvote = antallHverdagerIKvote,
        bruktKvote,
        gjenværendeKvote = if (bruktKvote != null) antallHverdagerIKvote - bruktKvote else null,
        senesteDatoForKvote,
        stansdato,
        opphørsdato
    )
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
