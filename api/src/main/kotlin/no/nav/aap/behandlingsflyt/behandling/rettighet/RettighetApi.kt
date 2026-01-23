package no.nav.aap.behandlingsflyt.behandling.rettighet

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.response.respondWithStatus
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.HttpStatusCode
import no.nav.aap.behandlingsflyt.behandling.rettighetstype.avslagsårsakerVedTapAvRettPåAAP
import no.nav.aap.behandlingsflyt.behandling.underveis.RettighetsperiodeService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagstype
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.tilgang.relevanteIdenterForBehandlingResolver
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.SakPathParam
import no.nav.aap.tilgang.authorizedGet
import javax.sql.DataSource

fun NormalOpenAPIRoute.rettighetApi(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry,
) {
    route("/api/behandling/{saksnummer}/rettighet") {
        authorizedGet<Saksnummer, List<RettighetDto>>(
            AuthorizationParamPathConfig(
                relevanteIdenterResolver = relevanteIdenterForBehandlingResolver(repositoryRegistry, dataSource),
                sakPathParam = SakPathParam("saksnummer"),
            )
        ) { saksnummer ->
            val respons: List<RettighetDto>? = dataSource.transaction(readOnly = true) { connection ->
                val repositoryProvider = repositoryRegistry.provider(connection)
                val sakRepository = repositoryProvider.provide<SakRepository>()
                val sak = sakRepository.hent(saksnummer)
                val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                val behandling = behandlingRepository.finnSisteOpprettedeBehandlingFor(
                    sak.id,
                    listOf(TypeBehandling.Førstegangsbehandling, TypeBehandling.Revurdering)
                )

                if (behandling == null) {
                    null
                }

                val underveisgrunnlagRepository = repositoryProvider.provide<UnderveisRepository>()
                val underveisgrunnlag = underveisgrunnlagRepository.hent(behandling!!.id)
                val vilkårsresultatRepository = repositoryProvider.provide<VilkårsresultatRepository>()
                val vilkårsresultat = vilkårsresultatRepository.hent(behandling.id)
                val avslagForTapAvAAP = avslagsårsakerVedTapAvRettPåAAP(vilkårsresultat)

                val rettighetstyper = underveisgrunnlag.perioder.map { it.rettighetsType }.distinct()

                val rettighetDtoListe = rettighetstyper.map { type ->
                    val rettighetKvoter = underveisgrunnlag.utledKvoterForRettighetstype(type!!)
                    val startdato = underveisgrunnlag.utledStartdatoForRettighet(type)
                    val gjenværendeKvote = rettighetKvoter.gjenværendeKvote
                    val perioderForOpphør = hentPerioderForAvslag(avslagForTapAvAAP, Avslagstype.OPPHØR)
                    val perioderForStans = hentPerioderForAvslag(avslagForTapAvAAP, Avslagstype.STANS)

                    val maksDato =
                        when (type) {
                            RettighetsType.BISTANDSBEHOV, RettighetsType.SYKEPENGEERSTATNING
                                -> underveisgrunnlag.utledMaksdatoForRettighet(type)

                            RettighetsType.STUDENT, RettighetsType.ARBEIDSSØKER, RettighetsType.VURDERES_FOR_UFØRETRYGD
                                -> RettighetsperiodeService().utledMaksdatoForRettighet(type, startdato)
                        }

                    RettighetDto(
                        type = type,
                        kvote = rettighetKvoter.totalKvote,
                        bruktKvote = rettighetKvoter.bruktKvote,
                        gjenværendeKvote = gjenværendeKvote,
                        startdato = startdato,
                        maksDato = maksDato,
                        avslagDato = perioderForOpphør.first().tom,
                        avslagÅrsak = if (perioderForOpphør.isNotEmpty()) Avslagstype.OPPHØR else if (perioderForStans.isNotEmpty()) Avslagstype.STANS else null
                    )
                }
                rettighetDtoListe
            }

            if (respons == null) {
                respondWithStatus(HttpStatusCode.NoContent)
            } else {
                respond(respons)
            }
        }
    }
}

fun hentPerioderForAvslag(tidslinje: Tidslinje<Set<Avslagsårsak>>, avslag: Avslagstype): List<Periode> {
    return tidslinje.filter { it.verdi.any { it.avslagstype == avslag } }.perioder().toList()
}
