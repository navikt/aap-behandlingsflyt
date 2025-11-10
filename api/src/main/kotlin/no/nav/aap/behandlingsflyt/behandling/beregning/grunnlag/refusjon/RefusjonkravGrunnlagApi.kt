package no.nav.aap.behandlingsflyt.behandling.beregning.grunnlag.refusjon

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.behandling.ansattinfo.AnsattInfoService
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvResponse
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokument
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.søknad.SøknadInformasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.AndreUtbetalingerYtelser
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.navenheter.NavKontorService
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.andreYtelserOppgittISøknad.AndreYtelserOppgittISøknadRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.refusjonkrav.RefusjonkravRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.refusjonkrav.RefusjonkravVurdering
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.AndreUtbetalingerYtelserDto
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.KabalHendelseV0
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Søknad
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.SøknadV0
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.behandlingsflyt.tilgang.kanSaksbehandle
import no.nav.aap.behandlingsflyt.tilgang.relevanteIdenterForBehandlingResolver
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.authorizedPost
import no.nav.aap.tilgang.getGrunnlag
import javax.sql.DataSource

fun NormalOpenAPIRoute.refusjonGrunnlagApi(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry,
    gatewayProvider: GatewayProvider,
) {
    val navKontorService = NavKontorService(gatewayProvider)
    val ansattInfoService = AnsattInfoService(gatewayProvider)

    route("/api/behandling") {
        route("/{referanse}/grunnlag/refusjon") {
            getGrunnlag<BehandlingReferanse, RefusjonkravGrunnlagResponse>(

                relevanteIdenterResolver = relevanteIdenterForBehandlingResolver(repositoryRegistry, dataSource),
                behandlingPathParam = BehandlingPathParam("referanse"),
                avklaringsbehovKode = Definisjon.REFUSJON_KRAV.kode.toString()
            ) { req ->
                val response =
                    dataSource.transaction(readOnly = true) { connection ->
                        val repositoryProvider = repositoryRegistry.provider(connection)
                        val refusjonkravRepository = repositoryProvider.provide<RefusjonkravRepository>()

                        val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                        val behandling = BehandlingReferanseService(behandlingRepository).behandling(req)

                        //TODO: Skal fikses etter prodsetting slik at det bare er gjeldendeVurderinger man skal forholde seg til
                        val gjeldendeVurderinger = refusjonkravRepository.hentHvisEksisterer(behandling.id)?.map {
                                it.tilResponse(ansattInfoService)
                            }

                        val andreYtelserRepository = repositoryProvider.provide<AndreYtelserOppgittISøknadRepository>()
                        val andreUtbetalinger = andreYtelserRepository.hentHvisEksisterer(behandling.id)

                        val gjeldendeVurdering =
                            gjeldendeVurderinger?.firstOrNull()
                        val historiskeVurderinger =
                            refusjonkravRepository
                                .hentHistoriskeVurderinger(behandling.sakId, behandling.id)
                                .map { it.tilResponse(ansattInfoService) }


                        val økonomiskSosialHjelp: Boolean? = if (andreUtbetalinger?.stønad == null ) {
                            null
                        } else if (andreUtbetalinger.stønad?.contains(AndreUtbetalingerYtelser.ØKONOMISK_SOSIALHJELP) == true) {
                            true
                        } else {
                            false
                        }


                        RefusjonkravGrunnlagResponse(
                            harTilgangTilÅSaksbehandle = kanSaksbehandle(),
                            gjeldendeVurdering = gjeldendeVurdering,
                            gjeldendeVurderinger = gjeldendeVurderinger,
                            historiskeVurderinger = historiskeVurderinger,
                            økonomiskSosialHjelp = økonomiskSosialHjelp
                        )
                    }
                respond(response)
            }
        }
    }

    route("/api/navenhet") {
        route("/{referanse}/finn") {
            authorizedPost<BehandlingReferanse, List<NavEnheterResponse>, NavEnheterRequest>(
                AuthorizationParamPathConfig(
                    relevanteIdenterResolver = relevanteIdenterForBehandlingResolver(repositoryRegistry, dataSource),
                    behandlingPathParam = BehandlingPathParam("referanse")
                )
            ) { req, body ->
                val response = navKontorService.hentNavEnheter()?.filter { enhet ->
                    enhet.navn.contains(
                        body.navn,
                        ignoreCase = true
                    ) || enhet.enhetsNummer.contains(body.navn, ignoreCase = true)
                }
                    ?.map { enhet ->
                        NavEnheterResponse(navn = enhet.navn, enhetsnummer = enhet.enhetsNummer)
                    }.orEmpty()
                respond(response)
            }
        }
    }
}


private fun RefusjonkravVurdering.tilResponse(ansattInfoService: AnsattInfoService): RefusjonkravVurderingResponse {
    val navnOgEnhet = ansattInfoService.hentAnsattNavnOgEnhet(vurdertAv)
    return RefusjonkravVurderingResponse(
        harKrav = harKrav,
        navKontor = navKontor,
        fom = fom,
        tom = tom,
        vurdertAv =
            VurdertAvResponse(
                ident = vurdertAv,
                dato = opprettetTid?.toLocalDate() ?: error("Fant ikke opprettet tid for refusjonkrav vurdering"),
                ansattnavn = navnOgEnhet?.navn,
                enhetsnavn = navnOgEnhet?.enhet
            )
    )
}