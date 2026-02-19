package no.nav.aap.behandlingsflyt.behandling.rettighet

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.response.respondWithStatus
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.*
import no.nav.aap.behandlingsflyt.behandling.rettighetstype.utledStansEllerOpphør
import no.nav.aap.behandlingsflyt.behandling.underveis.RettighetsperiodeService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagstype
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.stansopphør.Opphør
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.stansopphør.Stans
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.flate.SaksnummerParameter
import no.nav.aap.behandlingsflyt.tilgang.relevanteIdenterForSakResolver
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.httpklient.exception.VerdiIkkeFunnetException
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.SakPathParam
import no.nav.aap.tilgang.authorizedGet
import java.time.LocalDate
import javax.sql.DataSource

fun NormalOpenAPIRoute.rettighetApi(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry
) {
    route("/api/sak/{saksnummer}/rettighet") {
        authorizedGet<SaksnummerParameter, List<RettighetDto>>(
            AuthorizationParamPathConfig(
                relevanteIdenterResolver = relevanteIdenterForSakResolver(repositoryRegistry, dataSource),
                sakPathParam = SakPathParam("saksnummer")
            )
        ) { saksnummer ->
            val respons: List<RettighetDto>? = dataSource.transaction(readOnly = true) { connection ->
                val repositoryProvider = repositoryRegistry.provider(connection)
                val sakRepository = repositoryProvider.provide<SakRepository>()
                val sak = sakRepository.hentHvisFinnes(Saksnummer(saksnummer.saksnummer))
                    ?: throw VerdiIkkeFunnetException("Sak med saksnummer ${saksnummer.saksnummer} finnes ikke")
                val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                val sisteVedtatteYtelsesbehandling = behandlingRepository.finnGjeldendeVedtattBehandlingForSak(sak.id)

                if (sisteVedtatteYtelsesbehandling == null) {
                    return@transaction null
                }

                val underveisgrunnlagRepository = repositoryProvider.provide<UnderveisRepository>()
                val underveisgrunnlag = underveisgrunnlagRepository.hentHvisEksisterer(sisteVedtatteYtelsesbehandling.behandlingId)

                if (underveisgrunnlag == null) {
                    return@transaction null
                }

                val vilkårsresultatRepository = repositoryProvider.provide<VilkårsresultatRepository>()
                val vilkårsresultat = vilkårsresultatRepository.hent(sisteVedtatteYtelsesbehandling.behandlingId)
                val now = LocalDate.now()
                val stansEllerOpphør = utledStansEllerOpphør(vilkårsresultat, rettighetsperiode = sak.rettighetsperiode)
                    .filterKeys { it <= now }
                    .maxByOrNull { it.key }
                val rettighetstyper = underveisgrunnlag.perioder.mapNotNull { it.rettighetsType }.distinct()

                val rettighetDtoListe = rettighetstyper.map { rettighet ->
                    val rettighetKvoter = underveisgrunnlag.utledKvoterForRettighetstype(rettighet)
                    val startdato = underveisgrunnlag.utledStartdatoForRettighet(rettighet)
                    val gjenværendeKvote = rettighetKvoter.gjenværendeKvote

                    val maksDato =
                        when (rettighet) {
                            RettighetsType.BISTANDSBEHOV, RettighetsType.SYKEPENGEERSTATNING
                                -> underveisgrunnlag.utledMaksdatoForRettighet(rettighet)

                            RettighetsType.STUDENT, RettighetsType.ARBEIDSSØKER, RettighetsType.VURDERES_FOR_UFØRETRYGD
                                -> RettighetsperiodeService().utledMaksdatoForRettighet(rettighet, startdato)
                        }

                    val avslagÅrsak = when (stansEllerOpphør?.value) {
                        null -> null
                        is Opphør -> Avslagstype.OPPHØR
                        is Stans -> Avslagstype.STANS
                    }

                    RettighetDto(
                        type = rettighet,
                        kvote = rettighetKvoter.totalKvote,
                        bruktKvote = rettighetKvoter.bruktKvote,
                        gjenværendeKvote = gjenværendeKvote,
                        periodeKvoter = rettighetKvoter.periodeKvoter,
                        startDato = startdato,
                        maksDato = maksDato,
                        avslagDato = stansEllerOpphør?.key,
                        avslagÅrsak = avslagÅrsak
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
