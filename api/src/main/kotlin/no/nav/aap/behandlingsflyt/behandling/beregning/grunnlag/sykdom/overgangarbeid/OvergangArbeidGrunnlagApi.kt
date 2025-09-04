package no.nav.aap.behandlingsflyt.behandling.beregning.grunnlag.sykdom.overgangarbeid

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.behandling.ansattinfo.AnsattInfoService
import no.nav.aap.behandlingsflyt.behandling.beregning.grunnlag.sykdom.sykdom.SykdomsvurderingResponse
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvResponse
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangarbeid.OvergangArbeidRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangarbeid.OvergangArbeidVurdering
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

fun NormalOpenAPIRoute.overgangArbeidGrunnlagApi(
    dataSource: DataSource, repositoryRegistry: RepositoryRegistry,
    gatewayProvider: GatewayProvider,
) {
    val ansattInfoService = AnsattInfoService(gatewayProvider)

    route("/api/behandling") {
        route("/{referanse}/grunnlag/overgangarbeid") {
            getGrunnlag<BehandlingReferanse, OvergangArbeidGrunnlagResponse>(
                behandlingPathParam = BehandlingPathParam("referanse"),
                avklaringsbehovKode = Definisjon.AVKLAR_OVERGANG_ARBEID.kode.toString()
            ) { req ->
                val respons = dataSource.transaction(readOnly = true) { connection ->
                    val repositoryProvider = repositoryRegistry.provider(connection)
                    val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                    val overgangUforeRepository = repositoryProvider.provide<OvergangArbeidRepository>()
                    val sykdomRepository = repositoryProvider.provide<SykdomRepository>()

                    val behandling: Behandling =
                        BehandlingReferanseService(behandlingRepository).behandling(req)

                    val historiskeVurderinger =
                        overgangUforeRepository.hentHistoriskeOvergangArbeidVurderinger(behandling.sakId, behandling.id)
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

                    OvergangArbeidGrunnlagResponse(
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

private fun OvergangArbeidVurdering.tilResponse(erGjeldende: Boolean? = false, ansattInfoService: AnsattInfoService): OvergangArbeidVurderingResponse {
    val navnOgEnhet = ansattInfoService.hentAnsattNavnOgEnhet(vurdertAv)
    return OvergangArbeidVurderingResponse(
        begrunnelse = begrunnelse,
        brukerRettPåAAP = brukerRettPåAAP,
        vurderingenGjelderFra = vurderingenGjelderFra,
        virkningsdato = virkningsdato,
        vurdertAv = VurdertAvResponse(
            ident = vurdertAv,
            dato = opprettet?.atZone(ZoneId.of("Europe/Oslo"))?.toLocalDate()
                ?: error("Mangler opprettet dato for overgangarbeidvurdering"),
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