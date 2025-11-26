package no.nav.aap.behandlingsflyt.behandling.lovvalgmedlemskap.grunnlag

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.behandling.ansattinfo.AnsattInfoService
import no.nav.aap.behandlingsflyt.behandling.lovvalg.tilTidslinje
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvService
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.MedlemskapArbeidInntektForutgåendeRepository
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

fun NormalOpenAPIRoute.forutgåendeMedlemskapAPI(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry,
    gatewayProvider: GatewayProvider,
) {
    val ansattInfoService = AnsattInfoService(gatewayProvider)
    route("/api/behandling") {
        route("/{referanse}/grunnlag/forutgaaendemedlemskap") {
            getGrunnlag<BehandlingReferanse, ForutgåendeMedlemskapGrunnlagResponse>(
                relevanteIdenterResolver = relevanteIdenterForBehandlingResolver(repositoryRegistry, dataSource),
                behandlingPathParam = BehandlingPathParam("referanse"),
                avklaringsbehovKode =  Definisjon.AVKLAR_FORUTGÅENDE_MEDLEMSKAP.kode.toString()
            ) { req ->
                val grunnlag = dataSource.transaction { connection ->
                    val repositoryProvider = repositoryRegistry.provider(connection)
                    val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                    val forutgåendeRepository =
                        repositoryProvider.provide<MedlemskapArbeidInntektForutgåendeRepository>()
                    val behandling = BehandlingReferanseService(behandlingRepository).behandling(req)

                    val data =
                        forutgåendeRepository.hentHvisEksisterer(behandling.id)?.vurderinger?.maxByOrNull { it.vurdertTidspunkt } // TODO må legge innn støtte for periodisering her
                    val historiskeManuelleVurderinger =
                        forutgåendeRepository.hentHistoriskeVurderinger(behandling.sakId, behandling.id)
                    val ansattNavnOgEnhet = data?.let { ansattInfoService.hentAnsattNavnOgEnhet(it.vurdertAv) }

                    ForutgåendeMedlemskapGrunnlagResponse(
                        harTilgangTilÅSaksbehandle = kanSaksbehandle(),
                        vurdering = data?.toResponse(ansattNavnOgEnhet = ansattNavnOgEnhet),
                        historiskeManuelleVurderinger = historiskeManuelleVurderinger.map { it.toResponse() }
                    )
                }
                respond(grunnlag)
            }
        }

        route("/{referanse}/grunnlag/forutgaaendemedlemskap-v2") {
            getGrunnlag<BehandlingReferanse, PeriodisertForutgåendeMedlemskapGrunnlagResponse>(
                relevanteIdenterResolver = relevanteIdenterForBehandlingResolver(repositoryRegistry, dataSource),
                behandlingPathParam = BehandlingPathParam("referanse"),
                avklaringsbehovKode =  Definisjon.AVKLAR_FORUTGÅENDE_MEDLEMSKAP.kode.toString()
            ) { req ->
                val grunnlag = dataSource.transaction { connection ->
                    val repositoryProvider = repositoryRegistry.provider(connection)
                    val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                    val medlemskapRepository =
                        repositoryProvider
                            .provide<MedlemskapArbeidInntektForutgåendeRepository>()
                    val sakRepository = repositoryProvider.provide<SakRepository>()
                    val behandling = BehandlingReferanseService(behandlingRepository).behandling(req)
                    val sak = sakRepository.hent(behandling.sakId)
                    val vurdertAvService = VurdertAvService(repositoryProvider, gatewayProvider)

                    val grunnlag = medlemskapRepository.hentHvisEksisterer(behandling.id)
                    val nyeVurderinger = grunnlag?.vurderinger?.filter { it.vurdertIBehandling == behandling.id }
                    val gjeldendeVedtatteVurderinger =
                        grunnlag?.vurderinger?.filter { it.vurdertIBehandling != behandling.id }?.tilTidslinje() ?: Tidslinje()

                    // TODO skal denne ta utgangspunkt i avklaringsbehov?
                    val behøverVurderinger =
                        if (gjeldendeVedtatteVurderinger.isEmpty()) listOf(sak.rettighetsperiode)
                        else sak.rettighetsperiode.minus(gjeldendeVedtatteVurderinger.helePerioden())

                    PeriodisertForutgåendeMedlemskapGrunnlagResponse(
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
