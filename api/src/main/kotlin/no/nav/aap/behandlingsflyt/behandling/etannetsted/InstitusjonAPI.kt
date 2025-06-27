package no.nav.aap.behandlingsflyt.behandling.etannetsted

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.behandling.ansattinfo.AnsattInfoService
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvResponse
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.barnetillegg.BarnetilleggRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Institusjon
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.InstitusjonsoppholdGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.InstitusjonsoppholdRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Institusjonstype
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.tilgang.TilgangGateway
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.httpklient.auth.token
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.authorizedGet
import javax.sql.DataSource

fun NormalOpenAPIRoute.institusjonAPI(dataSource: DataSource, repositoryRegistry: RepositoryRegistry) {
    route("/api/behandling") {
        route("/{referanse}/grunnlag/institusjon/soning") {
            authorizedGet<BehandlingReferanse, SoningsGrunnlagDto>(
                AuthorizationParamPathConfig(
                    behandlingPathParam = BehandlingPathParam(
                        "referanse"
                    )
                )
            ) { req ->
                val soningsgrunnlag = dataSource.transaction(readOnly = true) { connection ->
                    val repositoryProvider = repositoryRegistry.provider(connection)
                    val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                    val sakRepository = repositoryProvider.provide<SakRepository>()
                    val barnetilleggRepository = repositoryProvider.provide<BarnetilleggRepository>()
                    val institusjonsoppholdRepository = repositoryProvider.provide<InstitusjonsoppholdRepository>()
                    val behandling = BehandlingReferanseService(behandlingRepository).behandling(req)

                    val utlederService =
                        EtAnnetStedUtlederService(
                            barnetilleggRepository, institusjonsoppholdRepository,
                            sakRepository,
                            behandlingRepository
                        )
                    val behov = utlederService.utled(behandling.id)

                    // Hent ut rå fakta fra grunnlaget
                    val grunnlag = institusjonsoppholdRepository.hentHvisEksisterer(behandling.id)
                    val soningsforholdInfo =
                        byggTidslinjeAvType(grunnlag, Institusjonstype.FO)

                    val perioderMedSoning = behov.perioderTilVurdering.mapValue { it.soning }.komprimer()
                    val vurderinger = grunnlag?.soningsVurderinger?.tilTidslinje() ?: Tidslinje()

                    val manglendePerioder =
                        perioderMedSoning
                            .segmenter()
                            .filterNot { it.verdi == null }
                            .map {
                                SoningsforholdDto(
                                    vurderingsdato = it.periode.fom,
                                    vurdering =
                                        vurderinger.segment(it.periode.fom)?.verdi?.let {
                                            SoningsvurderingDto(
                                                skalOpphøre = it.skalOpphøre,
                                                begrunnelse = it.begrunnelse,
                                                fraDato = it.fraDato
                                            )
                                        },
                                    status = it.verdi!!.vurdering.toDto()
                                )
                            }

                    val harTilgangTilÅSaksbehandle =
                        GatewayProvider.provide<TilgangGateway>().sjekkTilgangTilBehandling(
                            req.referanse,
                            Definisjon.AVKLAR_SONINGSFORRHOLD,
                            token()
                        )

                    SoningsGrunnlagDto(
                        harTilgangTilÅSaksbehandle = harTilgangTilÅSaksbehandle,
                        soningsforholdInfo.segmenter().map { InstitusjonsoppholdDto.institusjonToDto(it) },
                        manglendePerioder
                    )
                }
                respond(soningsgrunnlag)
            }
        }
    }
    route("/api/behandling") {
        route("/{referanse}/grunnlag/institusjon/helse") {
            authorizedGet<BehandlingReferanse, HelseinstitusjonGrunnlagDto>(
                AuthorizationParamPathConfig(
                    behandlingPathParam = BehandlingPathParam("referanse")
                )
            ) { req ->
                val grunnlagDto = dataSource.transaction(readOnly = true) { connection ->
                    val repositoryProvider = repositoryRegistry.provider(connection)
                    val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                    val sakRepository = repositoryProvider.provide<SakRepository>()
                    val behandling = BehandlingReferanseService(behandlingRepository).behandling(req)
                    val institusjonsoppholdRepository = repositoryProvider.provide<InstitusjonsoppholdRepository>()
                    val barnetilleggRepository = repositoryProvider.provide<BarnetilleggRepository>()

                    val utlederService =
                        EtAnnetStedUtlederService(
                            barnetilleggRepository, institusjonsoppholdRepository,
                            sakRepository,
                            behandlingRepository
                        )
                    val behov = utlederService.utled(behandling.id)

                    // Hent ut rå fakta fra grunnlaget
                    val grunnlag = institusjonsoppholdRepository.hentHvisEksisterer(behandling.id)
                    val oppholdInfo =
                        byggTidslinjeAvType(grunnlag, Institusjonstype.HS)

                    val perioderMedHelseopphold = behov.perioderTilVurdering.mapValue { it.helse }.komprimer()
                    val vurderinger = grunnlag?.helseoppholdvurderinger?.tilTidslinje() ?: Tidslinje()

                    val manglendePerioder = perioderMedHelseopphold.segmenter()
                        .filterNot { it.verdi == null }
                        .map {
                            HelseoppholdDto(
                                periode = it.periode,
                                vurderinger = vurderinger.begrensetTil(it.periode).segmenter()
                                    .map { helseinstitusjonsvurdering ->
                                        HelseinstitusjonVurderingDto(
                                            begrunnelse = helseinstitusjonsvurdering.verdi.begrunnelse,
                                            faarFriKostOgLosji = helseinstitusjonsvurdering.verdi.faarFriKostOgLosji,
                                            forsoergerEktefelle = helseinstitusjonsvurdering.verdi.forsoergerEktefelle,
                                            harFasteUtgifter = helseinstitusjonsvurdering.verdi.harFasteUtgifter,
                                            periode = helseinstitusjonsvurdering.periode,
                                        )
                                    },
                                status = it.verdi!!.vurdering.toDto()
                            )
                        }

                    val harTilgangTilÅSaksbehandle =
                        GatewayProvider.provide<TilgangGateway>().sjekkTilgangTilBehandling(
                            req.referanse,
                            Definisjon.AVKLAR_HELSEINSTITUSJON,
                            token()
                        )

                    val ansattNavnOgEnhet = grunnlag?.helseoppholdvurderinger?.let {  AnsattInfoService().hentAnsattNavnOgEnhet(it.vurdertAv) }

                    HelseinstitusjonGrunnlagDto(
                        harTilgangTilÅSaksbehandle = harTilgangTilÅSaksbehandle,
                        opphold = oppholdInfo.segmenter().map { InstitusjonsoppholdDto.institusjonToDto(it) },
                        vurderinger = manglendePerioder,
                        vurdertAv =
                            grunnlag?.helseoppholdvurderinger?.let {
                                VurdertAvResponse(
                                    ident = it.vurdertAv,
                                    dato = it.vurdertTidspunkt.toLocalDate(),
                                    ansattnavn = ansattNavnOgEnhet?.navn,
                                    enhetsnavn = ansattNavnOgEnhet?.enhet
                                )
                            }
                    )
                }
                respond(grunnlagDto)
            }
        }
    }
}

private fun byggTidslinjeAvType(
    soningsopphold: InstitusjonsoppholdGrunnlag?, institusjonstype: Institusjonstype
): Tidslinje<Institusjon> {
    return Tidslinje(soningsopphold?.oppholdene?.opphold?.filter { it.verdi.type == institusjonstype }
        ?: emptyList())
}
