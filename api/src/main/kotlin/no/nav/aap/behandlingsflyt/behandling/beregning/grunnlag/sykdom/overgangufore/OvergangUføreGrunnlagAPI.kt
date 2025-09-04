package no.nav.aap.behandlingsflyt.behandling.beregning.grunnlag.sykdom.overgangufore

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.behandling.ansattinfo.AnsattInfoService
import no.nav.aap.behandlingsflyt.behandling.beregning.grunnlag.sykdom.sykdom.SykdomsvurderingResponse
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvResponse
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangufore.OvergangUføreRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangufore.OvergangUføreVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Sykdomsvurdering
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.behandlingsflyt.tilgang.kanSaksbehandle
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.getGrunnlag
import java.time.ZoneId
import javax.sql.DataSource

fun NormalOpenAPIRoute.overgangUforeGrunnlagApi(
    dataSource: DataSource, repositoryRegistry: RepositoryRegistry,
    gatewayProvider: GatewayProvider,
) {
    val ansattInfoService = AnsattInfoService(gatewayProvider)

    route("/api/behandling") {
        route("/{referanse}/grunnlag/overgangufore") {
            getGrunnlag<BehandlingReferanse, OvergangUføreGrunnlagResponse>(
                behandlingPathParam = BehandlingPathParam("referanse"),
                avklaringsbehovKode = Definisjon.AVKLAR_OVERGANG_UFORE.kode.toString()
            ) { req ->
                val respons = dataSource.transaction(readOnly = true) { connection ->
                    val repositoryProvider = repositoryRegistry.provider(connection)
                    val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                    val overgangUforeRepository = repositoryProvider.provide<OvergangUføreRepository>()
                    val sykdomRepository = repositoryProvider.provide<SykdomRepository>()

                    val behandling: Behandling =
                        BehandlingReferanseService(behandlingRepository).behandling(req)

                    val historiskeVurderinger =
                        overgangUforeRepository.hentHistoriskeOvergangUforeVurderinger(behandling.sakId, behandling.id)
                    val grunnlag = overgangUforeRepository.hentHvisEksisterer(behandling.id)
                    val nåTilstand = grunnlag?.vurderinger.orEmpty()
                    val vedtatteBistandsvurderinger = behandling.forrigeBehandlingId
                        ?.let { overgangUforeRepository.hentHvisEksisterer(it) }
                        ?.vurderinger.orEmpty()
                    val vurdering = nåTilstand
                        .filterNot { it in vedtatteBistandsvurderinger }
                        .singleOrNull()

                    val gjeldendeSykdomsvurderinger =
                        sykdomRepository.hentHvisEksisterer(behandling.id)?.sykdomsvurderinger.orEmpty()

                    OvergangUføreGrunnlagResponse(
                        harTilgangTilÅSaksbehandle = kanSaksbehandle(),
                        vurdering = vurdering?.tilResponse(ansattInfoService = ansattInfoService),
                        gjeldendeVedtatteVurderinger = vedtatteBistandsvurderinger.map { it.tilResponse(ansattInfoService = ansattInfoService) },
                        historiskeVurderinger = historiskeVurderinger.map { it.tilResponse(ansattInfoService = ansattInfoService) },
                        gjeldendeSykdsomsvurderinger = gjeldendeSykdomsvurderinger.map { it.tilResponse(ansattInfoService) },
                    )
                }

                respond(respons)
            }
        }
    }
}

private fun OvergangUføreVurdering.tilResponse(erGjeldende: Boolean? = false, ansattInfoService: AnsattInfoService): OvergangUføreVurderingResponse {
    val navnOgEnhet = ansattInfoService.hentAnsattNavnOgEnhet(vurdertAv)
    return OvergangUføreVurderingResponse(
        begrunnelse = begrunnelse,
        brukerHarSøktUføretrygd = brukerHarSøktOmUføretrygd,
        brukerHarFåttVedtakOmUføretrygd = brukerHarFåttVedtakOmUføretrygd,
        brukerRettPåAAP = brukerRettPåAAP,
        vurderingenGjelderFra = vurderingenGjelderFra,
        virkningsdato = virkningsdato,
        vurdertAv = VurdertAvResponse(
            ident = vurdertAv,
            dato = opprettet?.atZone(ZoneId.of("Europe/Oslo"))?.toLocalDate()
                ?: error("Mangler opprettet dato for overganguførevurdering"),
            ansattnavn = navnOgEnhet?.navn,
            enhetsnavn = navnOgEnhet?.enhet,
        ),
        erGjeldende = erGjeldende
    )
}

private fun Sykdomsvurdering.tilResponse(ansattInfoService: AnsattInfoService): SykdomsvurderingResponse {
    val navnOgEnhet = ansattInfoService.hentAnsattNavnOgEnhet(vurdertAv.ident)
    return SykdomsvurderingResponse(
        begrunnelse = begrunnelse,
        vurderingenGjelderFra = vurderingenGjelderFra,
        dokumenterBruktIVurdering = dokumenterBruktIVurdering,
        erArbeidsevnenNedsatt = erArbeidsevnenNedsatt,
        harSkadeSykdomEllerLyte = harSkadeSykdomEllerLyte,
        erSkadeSykdomEllerLyteVesentligdel = erSkadeSykdomEllerLyteVesentligdel,
        erNedsettelseIArbeidsevneAvEnVissVarighet = erNedsettelseIArbeidsevneAvEnVissVarighet,
        erNedsettelseIArbeidsevneMerEnnHalvparten = erNedsettelseIArbeidsevneMerEnnHalvparten,
        erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense,
        yrkesskadeBegrunnelse = yrkesskadeBegrunnelse,
        kodeverk = kodeverk,
        hoveddiagnose = hoveddiagnose,
        bidiagnoser = bidiagnoser,
        vurdertAv = VurdertAvResponse(
            ident = vurdertAv.ident,
            dato = opprettet.atZone(ZoneId.of("Europe/Oslo")).toLocalDate(),
            ansattnavn = navnOgEnhet?.navn,
            enhetsnavn = navnOgEnhet?.enhet,
        )
    )
}
