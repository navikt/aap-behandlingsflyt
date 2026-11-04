package no.nav.aap.behandlingsflyt.behandling.lovvalgmedlemskap.grunnlag

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovMetadataService
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.lovvalg.MedlemskapLovvalgRegisterGrunnlag
import no.nav.aap.behandlingsflyt.behandling.vilkår.medlemskap.KanBehandlesAutomatiskVurdering
import no.nav.aap.behandlingsflyt.behandling.vilkår.medlemskap.LovvalgDelvurderingRepository
import no.nav.aap.behandlingsflyt.behandling.vilkår.medlemskap.LovvalgMedlemskapVurderingService
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvService
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.LovvalgMedlemskapRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonopplysningRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.gjeldendeVurderinger
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.VurderLovvalgSteg
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.tilgang.kanSaksbehandle
import no.nav.aap.behandlingsflyt.tilgang.relevanteIdenterForBehandlingResolver
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.repository.RepositoryProvider
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.komponenter.tidslinje.orEmpty
import no.nav.aap.komponenter.verdityper.Tid
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.getGrunnlag
import javax.sql.DataSource

fun NormalOpenAPIRoute.lovvalgMedlemskapGrunnlagApi(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry,
    gatewayProvider: GatewayProvider,
) {
    val unleashGateway = gatewayProvider.provide<UnleashGateway>()
    route("/api/behandling") {
        route("/{referanse}/grunnlag/lovvalgmedlemskap") {
            getGrunnlag<BehandlingReferanse, PeriodisertLovvalgMedlemskapGrunnlagResponse>(
                relevanteIdenterResolver = relevanteIdenterForBehandlingResolver(repositoryRegistry, dataSource),
                behandlingPathParam = BehandlingPathParam("referanse"),
                påkrevdRolle = Definisjon.AVKLAR_LOVVALG_MEDLEMSKAP.løsesAv
            ) { req ->
                val grunnlag =
                    dataSource.transaction { connection ->
                        val repositoryProvider = repositoryRegistry.provider(connection)
                        val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                        val lovvalgDelvurderingRepository = repositoryProvider.provide<LovvalgDelvurderingRepository>()
                        val lovvalgMedlemskapRepository =
                            repositoryProvider
                                .provide<LovvalgMedlemskapRepository>()
                        val sakRepository = repositoryProvider.provide<SakRepository>()
                        val behandling = BehandlingReferanseService(behandlingRepository).behandling(req)
                        val sak = sakRepository.hent(behandling.sakId)
                        val vurdertAvService = VurdertAvService(repositoryProvider, gatewayProvider)
                        val avklaringsbehovRepository = repositoryProvider.provide<AvklaringsbehovRepository>()
                        val vurderLovvalgSteg = VurderLovvalgSteg.konstruer(repositoryProvider, gatewayProvider)
                        val avklaringsbehovMetadataService =
                            AvklaringsbehovMetadataService(repositoryProvider, gatewayProvider)

                        val grunnlag = lovvalgMedlemskapRepository.hentHvisEksisterer(behandling.id)
                        val nyeVurderinger = grunnlag?.vurderinger?.filter { it.vurdertIBehandling == behandling.id }
                        val gjeldendeVedtatteVurderinger =
                            grunnlag?.vurderinger?.filter { it.vurdertIBehandling != behandling.id }
                                ?.gjeldendeVurderinger()
                                .orEmpty()

                        val delvurderingerForVurdertIBehandling = mutableMapOf<BehandlingId, KanBehandlesAutomatiskVurdering?>()
                        fun delvurderingerFor(vurdertIBehandling: BehandlingId) =
                            delvurderingerForVurdertIBehandling.getOrPut(vurdertIBehandling) {
                                lovvalgDelvurderingRepository.hentHvisEksisterer(vurdertIBehandling)?.vurdering()
                            }

                        val avklaringsbehov = avklaringsbehovRepository.hentAvklaringsbehovene(behandling.id)
                        val behøverVurderinger =
                            avklaringsbehov.hentBehovForDefinisjon(Definisjon.AVKLAR_LOVVALG_MEDLEMSKAP)
                                ?.perioderVedtaketBehøverVurdering()
                                .orEmpty()

                        // Dersom steget behøver en vurdering, skal det ikke lenger være overstyrt i denne behandlingen
                        val overstyrt = nyeVurderinger?.any { it.overstyrt } ?: false && behøverVurderinger.isEmpty()
                        val delvurderinger = lovvalgDelvurderingRepository
                            .hentHvisEksisterer(behandling.id)?.vurdering()
                            ?: utledFallbackDelvurderinger(behandling, repositoryProvider, unleashGateway)
                        PeriodisertLovvalgMedlemskapGrunnlagResponse(
                            harTilgangTilÅSaksbehandle = kanSaksbehandle(),
                            overstyrt = overstyrt,
                            behøverVurderinger = behøverVurderinger.toList(),
                            delvurderinger = delvurderinger,
                            kanVurderes = listOf(sak.rettighetsperiode),
                            nyeVurderinger = nyeVurderinger?.map {
                                it.toResponse(
                                    vurdertAvService = vurdertAvService,
                                    delvurderinger = delvurderingerFor(it.vurdertIBehandling),
                                )
                            } ?: emptyList(),
                            sisteVedtatteVurderinger = gjeldendeVedtatteVurderinger
                                .komprimer()
                                .segmenter()
                                .map { segment ->
                                    val verdi = segment.verdi
                                    verdi.toResponse(
                                        vurdertAvService = vurdertAvService,
                                        fom = segment.fom(),
                                        tom = if (segment.tom().isEqual(Tid.MAKS)) null else segment.tom(),
                                        delvurderinger = delvurderingerFor(verdi.vurdertIBehandling),
                                    )
                                },
                            ikkeRelevantePerioder = avklaringsbehovMetadataService.perioderSomSkalFremhevesSomIkkeRelevant(
                                vurderLovvalgSteg,
                                behandling,
                            ),
                        )
                    }

                respond(grunnlag)
            }
        }
    }
}

private fun utledFallbackDelvurderinger(
    behandling: Behandling,
    repositoryProvider: RepositoryProvider,
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
