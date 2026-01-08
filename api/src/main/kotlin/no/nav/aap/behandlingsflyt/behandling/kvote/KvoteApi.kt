package no.nav.aap.behandlingsflyt.behandling.kvote

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.VirkningstidspunktUtleder
import no.nav.aap.behandlingsflyt.behandling.underveis.KvoteService
import no.nav.aap.behandlingsflyt.behandling.underveis.Kvoter
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Hverdager
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Kvote
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.Underveisperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
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
        getGrunnlag<BehandlingReferanse, List<KvoteDto>>(
            behandlingPathParam = BehandlingPathParam("referanse"),
            avklaringsbehovKode = Definisjon.AVKLAR_BARNETILLEGG.kode.toString()
        ) { req ->
            val respons: List<KvoteDto> = dataSource.transaction(readOnly = true) { connection ->
                val kvoteDtoListe = mutableListOf<KvoteDto>()
                val repositoryProvider = repositoryRegistry.provider(connection)
                val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                val behandling = BehandlingReferanseService(behandlingRepository).behandling(req)

                val underveisgrunnlagRepository = repositoryProvider.provide<UnderveisRepository>()
                val underveisgrunnlag = underveisgrunnlagRepository.hent(behandling.id)
                val perioder = underveisgrunnlag.perioder
                val rettighetstypePerioderMap = mutableMapOf<RettighetsType, List<Underveisperiode>>()
                val kvoterForBehandling = KvoteService().beregn(behandling.id)

                RettighetsType.entries.forEach { type ->
                    rettighetstypePerioderMap[type] = perioder.filter { it.rettighetsType == type }
                }

                rettighetstypePerioderMap.forEach { (type, perioder) ->
                    val antallHverdagerIKvote = utledAntallKvotedager(type, kvoterForBehandling)
                    val bruktKvote = 0
                }

                val vilkårsresultatRepository = repositoryProvider.provide<VilkårsresultatRepository>()
                val virkningstidspunktUtleder = VirkningstidspunktUtleder(vilkårsresultatRepository)
                val virkningstidspunkt = virkningstidspunktUtleder.utledVirkningsTidspunkt(behandling.id)

                val kvoterForBehandling = KvoteService().beregn(behandling.id)
                kvoteDtoListe.add(utledKvoteDto(virkningstidspunkt, kvoterForBehandling.ordinærkvote, Kvote.ORDINÆR))
                kvoteDtoListe.add(utledKvoteDto(virkningstidspunkt, kvoterForBehandling.studentkvote, Kvote.STUDENT))
                kvoteDtoListe.add(utledKvoteDto(virkningstidspunkt, kvoterForBehandling.sykepengeerstatningkvote, Kvote.SYKEPENGEERSTATNING))

                kvoteDtoListe
            }
            respond(respons)
        }
    }
}

private fun utledKvoteDto(virkningstidspunkt: LocalDate?, kvote: Hverdager, type: Kvote): KvoteDto {
    val antallHverdagerIKvote = kvote.asInt
    val senesteDatoForKvote = utledSenesteDatoForKvote(virkningstidspunkt, antallHverdagerIKvote)

    if (type == Kvote.STUDENT) {
        return KvoteDto(
            rettighetStartDato = virkningstidspunkt,
            rettighetEndDato = senesteDatoForKvote
        )
    }

    val bruktKvote = virkningstidspunkt?.datesUntil(LocalDate.now())?.filter { it.dayOfWeek !in HELGEDAGER }?.count()?.toInt()
    val stansdato = LocalDate.now() // TODO
    val opphørsdato = LocalDate.now() // TODO

    return KvoteDto(
        kvote = antallHverdagerIKvote,
        bruktKvote,
        gjenværendeKvote = if (bruktKvote != null) antallHverdagerIKvote - bruktKvote else null,
        senesteDatoForKvote,
        stansdato,
        opphørsdato
    )
}

private fun utledSenesteDatoForKvote(startDato: LocalDate?, antallHverdagerIKvote: Int): LocalDate? {
    if (startDato == null) {
        return null
    }
    var gjenværendeDager = antallHverdagerIKvote
    var sluttDato = startDato
    while (gjenværendeDager > 0) {
        sluttDato = sluttDato?.plusDays(1)
        if (sluttDato?.dayOfWeek !in HELGEDAGER) gjenværendeDager--
    }
    return sluttDato
}

private fun utledAntallKvotedager(type: RettighetsType, kvoter: Kvoter): Int? {
    when (type) {
        RettighetsType.BISTANDSBEHOV -> kvoter.ordinærkvote.asInt
        RettighetsType.STUDENT -> kvoter.studentkvote.asInt
        RettighetsType.SYKEPENGEERSTATNING -> kvoter.sykepengeerstatningkvote.asInt
        else -> null
    }
}
