package no.nav.aap.behandlingsflyt.behandling.etannetsted

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.barnetillegg.BarnetilleggRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Institusjon
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.InstitusjonsoppholdGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.InstitusjonsoppholdRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Institusjonstype
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.HelseinstitusjonVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.flate.HelseinstitusjonGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.flate.Helseopphold
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.flate.InstitusjonsoppholdDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.flate.SoningsGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.flate.Soningsforhold
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
            authorizedGet<BehandlingReferanse, SoningsGrunnlag>(
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

                    val manglendePerioder = perioderMedSoning.segmenter()
                        .filterNot { it.verdi == null }
                        .map {
                            Soningsforhold(
                                vurderingsdato = it.periode.fom,
                                vurdering = vurderinger.segment(it.periode.fom)?.verdi,
                                status = it.verdi!!.vurdering
                            )
                        }

                    val harTilgangTilÅSaksbehandle =
                        GatewayProvider.provide<TilgangGateway>().sjekkTilgangTilBehandling(
                            req.referanse,
                            Definisjon.AVKLAR_SONINGSFORRHOLD,
                            token()
                        )


                    SoningsGrunnlag(
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
            authorizedGet<BehandlingReferanse, HelseinstitusjonGrunnlag>(
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
                            Helseopphold(
                                periode = it.periode,
                                vurderinger = vurderinger.begrensetTil(it.periode).segmenter()
                                    .map { helseinstitusjonsvurdering ->
                                        HelseinstitusjonVurdering(
                                            helseinstitusjonsvurdering.verdi.begrunnelse,
                                            helseinstitusjonsvurdering.verdi.faarFriKostOgLosji,
                                            helseinstitusjonsvurdering.verdi.forsoergerEktefelle,
                                            helseinstitusjonsvurdering.verdi.harFasteUtgifter,
                                            helseinstitusjonsvurdering.periode
                                        )
                                    },
                                status = it.verdi!!.vurdering
                            )
                        }

                    val harTilgangTilÅSaksbehandle =
                        GatewayProvider.provide<TilgangGateway>().sjekkTilgangTilBehandling(
                            req.referanse,
                            Definisjon.AVKLAR_HELSEINSTITUSJON,
                            token()
                        )


                    HelseinstitusjonGrunnlag(
                        harTilgangTilÅSaksbehandle = harTilgangTilÅSaksbehandle,
                        opphold = oppholdInfo.segmenter().map { InstitusjonsoppholdDto.institusjonToDto(it) },
                        vurderinger = manglendePerioder
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
