package no.nav.aap.behandlingsflyt.behandling.lovvalgmedlemskap.grunnlag

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.behandling.ansattinfo.AnsattInfoService
import no.nav.aap.behandlingsflyt.behandling.lovvalg.tilTidslinje
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvService
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.MedlemskapArbeidInntektRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.tilgang.kanSaksbehandle
import no.nav.aap.behandlingsflyt.tilgang.relevanteIdenterForBehandlingResolver
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.verdityper.Tid
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.getGrunnlag
import javax.sql.DataSource
import kotlin.collections.map

fun NormalOpenAPIRoute.lovvalgMedlemskapGrunnlagAPI(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry,
    gatewayProvider: GatewayProvider,
) {
    val ansattInfoService = AnsattInfoService(gatewayProvider)


    route("/api/behandling") {
        route("/{referanse}/grunnlag/lovvalgmedlemskap") {
            getGrunnlag<BehandlingReferanse, LovvalgMedlemskapGrunnlagResponse>(
                relevanteIdenterResolver =  relevanteIdenterForBehandlingResolver(repositoryRegistry, dataSource),
                behandlingPathParam = BehandlingPathParam("referanse"),
                avklaringsbehovKode = Definisjon.AVKLAR_LOVVALG_MEDLEMSKAP.kode.toString()
            ) { req ->
                val grunnlag =
                    dataSource.transaction { connection ->
                        val repositoryProvider = repositoryRegistry.provider(connection)
                        val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                        val lovvalgMedlemskapRepository =
                            repositoryProvider
                                .provide<MedlemskapArbeidInntektRepository>()
                        val behandling = BehandlingReferanseService(behandlingRepository).behandling(req)

                        val gjeldendeManuellVurdering =
                            lovvalgMedlemskapRepository.hentHvisEksisterer(behandling.id)?.vurderinger?.firstOrNull()
                        val historiskeManuelleVurderinger =
                            lovvalgMedlemskapRepository.hentHistoriskeVurderinger(behandling.sakId, behandling.id)
                        val ansattNavnOgEnhet = gjeldendeManuellVurdering?.let { ansattInfoService.hentAnsattNavnOgEnhet(it.vurdertAv)}

                        LovvalgMedlemskapGrunnlagResponse(
                            kanSaksbehandle(),
                            gjeldendeManuellVurdering?.toResponse(ansattNavnOgEnhet),
                            historiskeManuelleVurderinger.map { it.toResponse() }
                        )
                    }
                respond(grunnlag)
            }
        }

        route("/{referanse}/grunnlag/lovvalgmedlemskap-v2") {
            getGrunnlag<BehandlingReferanse, PeriodisertLovvalgMedlemskapGrunnlagResponse>(
                relevanteIdenterResolver =  relevanteIdenterForBehandlingResolver(repositoryRegistry, dataSource),
                behandlingPathParam = BehandlingPathParam("referanse"),
                avklaringsbehovKode = Definisjon.AVKLAR_LOVVALG_MEDLEMSKAP.kode.toString()
            ) { req ->
                val grunnlag =
                    dataSource.transaction { connection ->
                        val repositoryProvider = repositoryRegistry.provider(connection)
                        val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                        val lovvalgMedlemskapRepository =
                            repositoryProvider
                                .provide<MedlemskapArbeidInntektRepository>()
                        val sakRepository = repositoryProvider.provide<SakRepository>()
                        val behandling = BehandlingReferanseService(behandlingRepository).behandling(req)
                        val sak = sakRepository.hent(behandling.sakId)
                        val vurdertAvService = VurdertAvService(repositoryProvider, gatewayProvider)

                        val grunnlag = lovvalgMedlemskapRepository.hentHvisEksisterer(behandling.id)
                        val nyeVurderinger = grunnlag?.vurderinger?.filter { it.vurdertIBehandling == behandling.id }
                        val gjeldendeVedtatteVurderinger = grunnlag?.vurderinger?.filter { it.vurdertIBehandling != behandling.id }?.tilTidslinje() ?: Tidslinje()

                        val behøverVurderinger =
                            if (gjeldendeVedtatteVurderinger.isEmpty()) listOf(sak.rettighetsperiode)
                            else sak.rettighetsperiode.minus(gjeldendeVedtatteVurderinger.helePerioden())

                        PeriodisertLovvalgMedlemskapGrunnlagResponse(
                            harTilgangTilÅSaksbehandle = kanSaksbehandle(),
                            overstyrt = (nyeVurderinger)?.any { it.overstyrt } ?: false,
                            behøverVurderinger = behøverVurderinger.toList(),
                            kanVurderes = listOf(sak.rettighetsperiode),
                            nyeVurderinger = nyeVurderinger?.map { it.toResponse(vurdertAvService) } ?: emptyList(),
                            sisteVedtatteVurderinger = gjeldendeVedtatteVurderinger
                                .komprimer()
                                .segmenter()
                                .map { segment ->
                                    val verdi = segment.verdi
                                    verdi.toResponse(
                                        vurdertAvService = vurdertAvService,
                                        fom = segment.fom(),
                                        tom = if (segment.tom().isEqual(Tid.MAKS)) null else segment.tom()
                                    )
                                }
                        )
                    }

                respond(grunnlag)
            }
        }
    }
}