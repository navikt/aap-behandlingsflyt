package no.nav.aap.behandlingsflyt.behandling.kvote

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.behandling.underveis.KvoteService
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Hverdager.Companion.antallHverdager
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.Underveisperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisÅrsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.getGrunnlag
import java.time.LocalDate
import javax.sql.DataSource

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
                val repositoryProvider = repositoryRegistry.provider(connection)
                val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                val behandling = BehandlingReferanseService(behandlingRepository).behandling(req)
                val kvoterForBehandling = KvoteService().beregn(behandling.id)

                val underveisgrunnlagRepository = repositoryProvider.provide<UnderveisRepository>()
                val underveisgrunnlag = underveisgrunnlagRepository.hent(behandling.id)
                val perioder = underveisgrunnlag.perioder
                val rettighetstypePerioderMap = mutableMapOf<RettighetsType, List<Underveisperiode>>()
                val kvoteDtoListe = mutableListOf<KvoteDto>()

                RettighetsType.entries.forEach { type ->
                    rettighetstypePerioderMap[type] = perioder.filter { it.rettighetsType == type }
                }

                rettighetstypePerioderMap.forEach { (type, perioder) ->
                    val innfriddePerioder = perioder.filter { it.avslagsårsak == null }
                    val antallDagerIKvote = kvoterForBehandling.hentKvoteForRettighetstype(type).asInt
                    val bruktKvote = innfriddePerioder
                        .filter { it.periode.tom <= LocalDate.now() }
                        .sumOf { it.periode.antallHverdager().asInt }

                    kvoteDtoListe.add(
                        KvoteDto(
                            kvote = antallDagerIKvote,
                            bruktKvote,
                            gjenværendeKvote = antallDagerIKvote.minus(bruktKvote),
                            startdatoForKvote = perioder.first().periode.tom,
                            sluttDatoForKvote = innfriddePerioder.last().periode.tom,
                            opphørsdato = perioder.first { it.avslagsårsak == UnderveisÅrsak.VARIGHETSKVOTE_BRUKT_OPP }.periode.tom,
                            stansdato = perioder.first { it.avslagsårsak == UnderveisÅrsak.BRUDD_PÅ_AKTIVITETSPLIKT_11_7_STANS }.periode.tom // TODO Erstatt med ikke-deprecated
                        )
                    )
                }
                kvoteDtoListe
            }
            respond(respons)
        }
    }
}
