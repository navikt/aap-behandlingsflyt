package no.nav.aap.behandlingsflyt.behandling.rettighet

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.response.respondWithStatus
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.HttpStatusCode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.rettighetstype.RettighetstypeRepository
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.flate.SaksnummerParameter
import no.nav.aap.behandlingsflyt.tilgang.relevanteIdenterForSakResolver
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.SakPathParam
import no.nav.aap.tilgang.authorizedGet
import javax.sql.DataSource

fun NormalOpenAPIRoute.rettighetsinfoApi(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry
) {
    route("/api/sak/{saksnummer}/rettighetsinfo") {
        authorizedGet<SaksnummerParameter, RettighetsinfoDto>(
            AuthorizationParamPathConfig(
                relevanteIdenterResolver = relevanteIdenterForSakResolver(repositoryRegistry, dataSource),
                sakPathParam = SakPathParam("saksnummer")
            )
        ) { saksnummer ->
            val respons: RettighetsinfoDto? = dataSource.transaction(readOnly = true) { connection ->
                val repositoryProvider = repositoryRegistry.provider(connection)
                val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                val sakRepository = repositoryProvider.provide<SakRepository>()
                val rettighetstypeRepository = repositoryProvider.provide<RettighetstypeRepository>()


                val sak = sakRepository.hentHvisFinnes(Saksnummer(saksnummer.saksnummer))
                    ?: throw UgyldigForespørselException("Sak med saksnummer ${saksnummer.saksnummer} finnes ikke")

                val sisteVedtatteYtelsesbehandling =
                    behandlingRepository.finnGjeldendeVedtattBehandlingForSak(sak.id) ?: return@transaction null

                val rettighetstypeTidslinje =
                    rettighetstypeRepository.hentHvisEksisterer(sisteVedtatteYtelsesbehandling.behandlingId)?.rettighetstypeTidslinje
                        ?: return@transaction null

                val sisteDagMedRett = rettighetstypeTidslinje.perioder().maxOfOrNull { it.tom }

                RettighetsinfoDto(
                    sisteDagMedRett = sisteDagMedRett,
                    perioderMedRett = rettighetstypeTidslinje.komprimer().segmenter().map {
                        RettighetsperiodeDto(
                            rettighetstype = it.verdi,
                            fom = it.fom(),
                            tom = it.tom(),
                        )
                    })
            }

            if (respons == null) {
                respondWithStatus(HttpStatusCode.NoContent)
            } else {
                respond(respons)
            }
        }
    }
}
