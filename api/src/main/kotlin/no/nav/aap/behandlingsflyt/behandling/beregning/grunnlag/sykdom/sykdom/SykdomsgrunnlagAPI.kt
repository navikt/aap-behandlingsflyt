package no.nav.aap.behandlingsflyt.behandling.beregning.grunnlag.sykdom.sykdom

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.behandling.ansattinfo.AnsattInfoService
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvResponse
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.YrkesskadeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Sykdomsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Yrkesskadevurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.flate.InnhentetSykdomsOpplysninger
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.flate.RegistrertYrkesskade
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.behandlingsflyt.tilgang.kanSaksbehandle
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.getGrunnlag
import java.time.LocalDate
import java.time.ZoneId
import javax.sql.DataSource

fun NormalOpenAPIRoute.sykdomsgrunnlagApi(dataSource: DataSource, repositoryRegistry: RepositoryRegistry) {
    route("/api/behandling") {
        route("/{referanse}/grunnlag/sykdom/sykdom") {
            getGrunnlag<BehandlingReferanse, SykdomGrunnlagResponse>(
                behandlingPathParam = BehandlingPathParam("referanse"),
                avklaringsbehovKode = Definisjon.AVKLAR_SYKDOM.kode.toString()
            ) { req ->
                val response = dataSource.transaction(readOnly = true) { connection ->
                    val repositoryProvider = repositoryRegistry.provider(connection)
                    val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                    val sykdomRepository = repositoryProvider.provide<SykdomRepository>()
                    val yrkesskadeRepository = repositoryProvider.provide<YrkesskadeRepository>()
                    val behandling = BehandlingReferanseService(behandlingRepository).behandling(req)

                    val yrkesskadeGrunnlag = yrkesskadeRepository.hentHvisEksisterer(behandlingId = behandling.id)
                    val sykdomGrunnlag = sykdomRepository.hentHvisEksisterer(behandlingId = behandling.id)

                    val innhentedeYrkesskader = yrkesskadeGrunnlag?.yrkesskader?.yrkesskader.orEmpty()
                        .map { yrkesskade -> RegistrertYrkesskade(yrkesskade) }

                    val nåTilstand = sykdomGrunnlag?.sykdomsvurderinger.orEmpty()

                    val historikkSykdomsvurderinger =
                        sykdomRepository.hentHistoriskeSykdomsvurderinger(behandling.sakId, behandling.id)

                    val vedtatteSykdomsvurderinger = behandling.forrigeBehandlingId
                        ?.let { sykdomRepository.hentHvisEksisterer(it) }
                        ?.sykdomsvurderinger.orEmpty()

                    val vedtatteSykdomsvurderingerIder = vedtatteSykdomsvurderinger.map { it.id }
                    val sykdomsvurderinger = nåTilstand.filterNot { it.id in vedtatteSykdomsvurderingerIder }

                    SykdomGrunnlagResponse(
                        opplysninger = InnhentetSykdomsOpplysninger(
                            oppgittYrkesskadeISøknad = false,
                            innhentedeYrkesskader = innhentedeYrkesskader,
                        ),
                        skalVurdereYrkesskade = innhentedeYrkesskader.isNotEmpty(),
                        sykdomsvurderinger = sykdomsvurderinger
                            .sortedBy { it.vurderingenGjelderFra ?: LocalDate.MIN }
                            .map { it.toDto() },
                        historikkSykdomsvurderinger = historikkSykdomsvurderinger
                            .sortedBy { it.opprettet }
                            .mapIndexed { index, vurdering ->
                                vurdering.toDto()
                            },
                        gjeldendeVedtatteSykdomsvurderinger = vedtatteSykdomsvurderinger
                            .sortedBy { it.vurderingenGjelderFra ?: LocalDate.MIN }
                            .map { it.toDto() },
                        harTilgangTilÅSaksbehandle = kanSaksbehandle()
                    )
                }

                respond(response)
            }
        }
        route("/{referanse}/grunnlag/sykdom/yrkesskade") {
            getGrunnlag<BehandlingReferanse, YrkesskadeVurderingGrunnlagResponse>(
                behandlingPathParam = BehandlingPathParam("referanse"),
                avklaringsbehovKode = Definisjon.AVKLAR_YRKESSKADE.kode.toString()
            ) { req ->
                val response = dataSource.transaction(readOnly = true) { connection ->
                    val repositoryProvider = repositoryRegistry.provider(connection)
                    val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                    val sykdomRepository = repositoryProvider.provide<SykdomRepository>()
                    val yrkesskadeRepository = repositoryProvider.provide<YrkesskadeRepository>()

                    val behandling: Behandling =
                        BehandlingReferanseService(behandlingRepository).behandling(req)

                    val yrkesskadeGrunnlag =
                        yrkesskadeRepository.hentHvisEksisterer(behandlingId = behandling.id)
                    val sykdomGrunnlag = sykdomRepository.hentHvisEksisterer(behandlingId = behandling.id)

                    yrkesskadeGrunnlag to sykdomGrunnlag

                    val innhentedeYrkesskader = yrkesskadeGrunnlag?.yrkesskader?.yrkesskader.orEmpty()
                        .map { yrkesskade -> RegistrertYrkesskade(yrkesskade) }

                    YrkesskadeVurderingGrunnlagResponse(
                        harTilgangTilÅSaksbehandle = kanSaksbehandle(),
                        opplysninger = InnhentetSykdomsOpplysninger(
                            oppgittYrkesskadeISøknad = false,
                            innhentedeYrkesskader = innhentedeYrkesskader,
                        ),
                        yrkesskadeVurdering = sykdomGrunnlag?.yrkesskadevurdering?.toResponse(),
                    )
                }

                respond(response)
            }
        }
    }
}

private fun Yrkesskadevurdering.toResponse(): YrkesskadevurderingResponse {
    val navnOgEnhet = AnsattInfoService().hentAnsattNavnOgEnhet(vurdertAv)
    return YrkesskadevurderingResponse(
        begrunnelse = begrunnelse,
        relevanteSaker = relevanteSaker,
        andelAvNedsettelsen = andelAvNedsettelsen?.prosentverdi(),
        erÅrsakssammenheng = erÅrsakssammenheng,
        vurdertAv = VurdertAvResponse(
            ident = vurdertAv,
            dato = requireNotNull(vurdertTidspunkt?.toLocalDate()) { "Fant ikke vurderingstidspunkt for yrkesskadevurdering" },
            ansattnavn = navnOgEnhet?.navn,
            enhetsnavn = navnOgEnhet?.enhet,
        )
    )
}

private fun Sykdomsvurdering.toDto(): SykdomsvurderingResponse {
    val navnOgEnhet = AnsattInfoService().hentAnsattNavnOgEnhet(vurdertAv.ident)
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

