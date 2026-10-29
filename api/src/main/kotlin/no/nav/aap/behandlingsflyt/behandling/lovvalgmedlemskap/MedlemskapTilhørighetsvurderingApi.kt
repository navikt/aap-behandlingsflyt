package no.nav.aap.behandlingsflyt.behandling.lovvalgmedlemskap

import com.papsign.ktor.openapigen.route.TagModule
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.Tags
import no.nav.aap.behandlingsflyt.behandling.lovvalg.ForutgåendeMedlemskapFaktaGrunnlag
import no.nav.aap.behandlingsflyt.behandling.lovvalg.MedlemskapLovvalgRegisterGrunnlag
import no.nav.aap.behandlingsflyt.behandling.vilkår.medlemskap.ForutgåendeMedlemskapVurderingService
import no.nav.aap.behandlingsflyt.behandling.vilkår.medlemskap.KanBehandlesAutomatiskVurdering
import no.nav.aap.behandlingsflyt.behandling.vilkår.medlemskap.LovvalgDelvurderingRepository
import no.nav.aap.behandlingsflyt.behandling.vilkår.medlemskap.LovvalgMedlemskapVurderingService
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.ForutgåendeMedlemskapRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.LovvalgMedlemskapRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonopplysningForutgåendeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonopplysningRepository
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.tilgang.relevanteIdenterForBehandlingResolver
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.repository.RepositoryProvider
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.authorizedGet
import javax.sql.DataSource

fun NormalOpenAPIRoute.medlemskapTilhørighetsvurderingApi(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry,
    gatewayProvider: GatewayProvider
) {
    val unleashGateway = gatewayProvider.provide<UnleashGateway>()
    // TODO: Flytte dette inn i grunnlag-apiene
    route("/api/lovvalgmedlemskap/") {
        route("/vurdering/{referanse}") {
            authorizedGet<BehandlingReferanse, KanBehandlesAutomatiskVurdering>(
                AuthorizationParamPathConfig(
                    behandlingPathParam = BehandlingPathParam("referanse"),
                    relevanteIdenterResolver = relevanteIdenterForBehandlingResolver(repositoryRegistry, dataSource),
                ),
                null, modules = arrayOf(TagModule(listOf(Tags.Behandling)))
            ) { req ->
                val vurdering = dataSource.transaction { connection ->
                    val repositoryProvider = repositoryRegistry.provider(connection)
                    val behandling =
                        repositoryProvider.provide<BehandlingRepository>().hent(BehandlingReferanse(req.referanse))
                    val lagretDelvurdering = repositoryProvider.provide<LovvalgDelvurderingRepository>()
                        .hentHvisEksisterer(behandling.id)
                    lagretDelvurdering?.vurdering()
                        ?: utledFallbackKanBehandlesAutomatiskVurdering(repositoryProvider, behandling, unleashGateway)
                }
                respond(vurdering)
            }
        }
        route("/forutgaaendevurdering/{referanse}") {
            authorizedGet<BehandlingReferanse, KanBehandlesAutomatiskVurdering>(
                AuthorizationParamPathConfig(
                    behandlingPathParam = BehandlingPathParam("referanse"),
                    relevanteIdenterResolver = relevanteIdenterForBehandlingResolver(repositoryRegistry, dataSource),
                ),
                null, modules = arrayOf(TagModule(listOf(Tags.Behandling)))
            ) { req ->
                val vurdering = dataSource.transaction { connection ->
                    val repositoryProvider = repositoryRegistry.provider(connection)
                    val forutgåendeMedlemskapRepository =
                        repositoryProvider.provide<ForutgåendeMedlemskapRepository>()
                    val lovvalgMedlemskapRepository =
                        repositoryProvider.provide<LovvalgMedlemskapRepository>()
                    val behandling =
                        repositoryProvider.provide<BehandlingRepository>().hent(BehandlingReferanse(req.referanse))
                    val sak = repositoryProvider.provide<SakRepository>().hent(behandling.sakId)

                    val personopplysningGrunnlag = repositoryProvider.provide<PersonopplysningForutgåendeRepository>()
                        .hentHvisEksisterer(behandling.id)
                    if (personopplysningGrunnlag == null) {
                        // Steget har ikke fått kjørt enda, så
                        return@transaction KanBehandlesAutomatiskVurdering(false, emptyList())
                    }

                    val medlemskapArbeidInntektGrunnlag =
                        forutgåendeMedlemskapRepository.hentHvisEksisterer(behandling.id)

                    val oppgittUtenlandsOppholdGrunnlag =
                        lovvalgMedlemskapRepository.hentOppgittUtenlandsOppholdHvisEksisterer(behandling.id)
                            ?: lovvalgMedlemskapRepository.hentSistRelevanteOppgitteUtenlandsOppholdHvisEksisterer(
                                sak.id
                            )

                    ForutgåendeMedlemskapVurderingService().vurderTilhørighet(
                        ForutgåendeMedlemskapFaktaGrunnlag(
                            medlemskapArbeidInntektGrunnlag, personopplysningGrunnlag, oppgittUtenlandsOppholdGrunnlag
                        ),
                        sak.rettighetsperiode
                    )
                }
                respond(vurdering)
            }
        }
    }
}

private fun utledFallbackKanBehandlesAutomatiskVurdering(
    repositoryProvider: RepositoryProvider,
    behandling: Behandling,
    unleashGateway: UnleashGateway
): KanBehandlesAutomatiskVurdering {
    val lovvalgMedlemskapRepository =
        repositoryProvider.provide<LovvalgMedlemskapRepository>()
    val sak = repositoryProvider.provide<SakRepository>().hent(behandling.sakId)

    val brukerPersonopplysning =
        repositoryProvider.provide<PersonopplysningRepository>()
            .hentBrukerPersonOpplysningHvisEksisterer(behandling.id)
    if (brukerPersonopplysning == null) {
        // Steget har ikke fått kjørt enda, så ingen vurdering kan gis ut
        return KanBehandlesAutomatiskVurdering(false, emptyList())
    }
    val medlemskapArbeidInntektGrunnlag =
        repositoryProvider.provide<LovvalgMedlemskapRepository>()
            .hentHvisEksisterer(behandling.id)

    val oppgittUtenlandsOppholdGrunnlag =
        lovvalgMedlemskapRepository.hentOppgittUtenlandsOppholdHvisEksisterer(behandling.id)
            ?: lovvalgMedlemskapRepository.hentSistRelevanteOppgitteUtenlandsOppholdHvisEksisterer(
                sak.id
            )

    return LovvalgMedlemskapVurderingService().vurderTilhørighet(
        MedlemskapLovvalgRegisterGrunnlag.fra(
            medlemskapArbeidInntektGrunnlag,
            brukerPersonopplysning,
            oppgittUtenlandsOppholdGrunnlag,
            vurderBosattStatusOgNorskStatsborgerskap =
                unleashGateway.isEnabled(BehandlingsflytFeature.BosattStatsborgerskapGjennomslipp),
        ),
        sak.rettighetsperiode
    )
}