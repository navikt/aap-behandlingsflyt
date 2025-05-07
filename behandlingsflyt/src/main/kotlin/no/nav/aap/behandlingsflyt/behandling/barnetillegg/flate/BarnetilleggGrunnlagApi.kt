package no.nav.aap.behandlingsflyt.behandling.barnetillegg.flate

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.behandling.barnetillegg.BarnetilleggService
import no.nav.aap.behandlingsflyt.faktagrunnlag.GrunnlagKopiererImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.Barn
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.BarnRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonopplysningGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonopplysningRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.tilgang.TilgangGatewayImpl
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.httpklient.auth.token
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.authorizedGet
import javax.sql.DataSource
import no.nav.aap.lookup.repository.RepositoryRegistry

fun NormalOpenAPIRoute.barnetilleggApi(dataSource: DataSource) {
    route("/api/barnetillegg") {
        route("/grunnlag/{referanse}") {
            authorizedGet<BehandlingReferanse, BarnetilleggDto>(
                AuthorizationParamPathConfig(
                    behandlingPathParam = BehandlingPathParam(
                        "referanse"
                    )
                )
            ) { req ->
                val dto = dataSource.transaction(readOnly = true) { connection ->
                    val repositoryProvider = RepositoryRegistry.provider(connection)
                    val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                    val vilkårsresultatRepository =
                        repositoryProvider.provide<VilkårsresultatRepository>()
                    val sakRepository = repositoryProvider.provide<SakRepository>()
                    val personopplysningRepository =
                        repositoryProvider.provide<PersonopplysningRepository>()

                    val behandling: Behandling =
                        BehandlingReferanseService(behandlingRepository).behandling(req)
                    val barnRepository = repositoryProvider.provide<BarnRepository>()

                    val sakOgBehandlingService = SakOgBehandlingService(
                        GrunnlagKopiererImpl(repositoryProvider), sakRepository, behandlingRepository
                    )
                    val barnetilleggService = BarnetilleggService(
                        sakOgBehandlingService,
                        barnRepository,
                        personopplysningRepository,
                        vilkårsresultatRepository,
                    )
                    val barnetilleggTidslinje = barnetilleggService.beregn(behandling.id)

                    val folkeregister = mutableSetOf<Ident>()
                    val uavklarteBarn = mutableSetOf<Ident>()
                    barnetilleggTidslinje.segmenter().forEach { segment ->
                        uavklarteBarn.addAll(segment.verdi.barnTilAvklaring())
                        folkeregister.addAll(segment.verdi.registerBarn())
                    }

                    val barnGrunnlag = barnRepository.hentHvisEksisterer(behandling.id)
                    val personopplysningGrunnlag =
                        requireNotNull(personopplysningRepository.hentHvisEksisterer(behandling.id))

                    val harTilgangTilÅSaksbehandle = TilgangGatewayImpl.sjekkTilgangTilBehandling(
                        req.referanse,
                        Definisjon.AVKLAR_BARNETILLEGG.kode.toString(),
                        token()
                    )


                    BarnetilleggDto(
                        harTilgangTilÅSaksbehandle = harTilgangTilÅSaksbehandle,
                        søknadstidspunkt = sakOgBehandlingService.hentSakFor(behandling.id).rettighetsperiode.fom,
                        folkeregisterbarn = folkeregister.map {
                            hentBarn(
                                it,
                                personopplysningGrunnlag
                            )
                        },
                        vurderteBarn = barnGrunnlag?.vurderteBarn?.barn?.map {
                            ExtendedVurdertBarnDto(
                                ident = it.ident.identifikator,
                                vurderinger = it.vurderinger,
                                fødselsdato = hentBarn(
                                    it.ident,
                                    personopplysningGrunnlag
                                ).fødselsdato
                            )
                        } ?: emptyList(),
                        barnSomTrengerVurdering = uavklarteBarn.map {
                            hentBarn(
                                it,
                                personopplysningGrunnlag
                            )
                        }
                            .toList()
                    )
                }

                respond(dto)
            }
        }
    }
}

fun hentBarn(ident: Ident, personopplysningGrunnlag: PersonopplysningGrunnlag): IdentifiserteBarnDto {
    val personopplysning =
        requireNotNull(personopplysningGrunnlag.relatertePersonopplysninger?.personopplysninger?.single { relatertPersonopplysning ->
            relatertPersonopplysning.gjelderForIdent(
                ident
            )
        })

    return IdentifiserteBarnDto(
        ident,
        Barn(ident, personopplysning.fødselsdato, personopplysning.dødsdato).periodeMedRettTil(),
        personopplysning.fødselsdato.toLocalDate()
    )
}
