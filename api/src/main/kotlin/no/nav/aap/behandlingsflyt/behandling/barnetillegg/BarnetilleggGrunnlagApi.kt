package no.nav.aap.behandlingsflyt.behandling.barnetillegg

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.behandling.ansattinfo.AnsattInfoService
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvResponse
import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.Barn
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.BarnGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.BarnRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.BarnIdentifikator
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.VurderingAvForeldreAnsvarDto
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
import org.slf4j.LoggerFactory
import javax.sql.DataSource

private val log = LoggerFactory.getLogger("barnetilleggApi")

fun NormalOpenAPIRoute.barnetilleggApi(dataSource: DataSource, repositoryRegistry: RepositoryRegistry) {
    route("/api/barnetillegg") {
        route("/grunnlag/{referanse}") {
            getGrunnlag<BehandlingReferanse, BarnetilleggDto>(
                behandlingPathParam = BehandlingPathParam("referanse"),
                avklaringsbehovKode = Definisjon.AVKLAR_BARNETILLEGG.kode.toString()
            ) { req ->
                val dto = dataSource.transaction(readOnly = true) { connection ->
                    val repositoryProvider = repositoryRegistry.provider(connection)
                    val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                    val vilkårsresultatRepository =
                        repositoryProvider.provide<VilkårsresultatRepository>()

                    val behandling: Behandling =
                        BehandlingReferanseService(behandlingRepository).behandling(req)
                    val barnRepository = repositoryProvider.provide<BarnRepository>()

                    val sakOgBehandlingService = SakOgBehandlingService(repositoryProvider)
                    val barnetilleggService = BarnetilleggService(
                        sakOgBehandlingService,
                        barnRepository,
                        vilkårsresultatRepository,
                    )
                    val barnetilleggTidslinje = barnetilleggService.beregn(behandling.id)

                    val folkeregister = barnetilleggTidslinje.map { it.verdi.registerBarn() }.flatten().toSet()
                    val uavklarteBarn = barnetilleggTidslinje.map { it.verdi.barnTilAvklaring() }.flatten().toSet()

                    val vurderteBarn = barnRepository.hentVurderteBarnHvisEksisterer(behandling.id)
                    val barnGrunnlag = barnRepository.hentHvisEksisterer(behandling.id)

                    val ansattNavnOgEnhet =
                        vurderteBarn?.let { AnsattInfoService(GatewayProvider).hentAnsattNavnOgEnhet(it.vurdertAv) }

                    BarnetilleggDto(
                        harTilgangTilÅSaksbehandle = kanSaksbehandle(),
                        søknadstidspunkt = sakOgBehandlingService.hentSakFor(behandling.id).rettighetsperiode.fom,
                        folkeregisterbarn = folkeregister.map {
                            hentBarn(
                                it,
                                barnGrunnlag
                            )
                        },
                        vurderteBarn = vurderteBarn?.barn.orEmpty().map {
                            val vurdertBartIdent = it.ident
                            when (vurdertBartIdent) {
                                is BarnIdentifikator.BarnIdent -> ExtendedVurdertBarnDto(
                                    vurdertBartIdent.ident.identifikator, null,
                                    it.vurderinger.map {
                                        VurderingAvForeldreAnsvarDto(
                                            it.fraDato,
                                            it.harForeldreAnsvar,
                                            it.begrunnelse
                                        )
                                    },
                                    hentBarn(
                                        it.ident,
                                        barnGrunnlag
                                    ).fodselsDato,
                                )

                                is BarnIdentifikator.NavnOgFødselsdato -> ExtendedVurdertBarnDto(
                                    ident = null,
                                    vurderinger = it.vurderinger.map {
                                        VurderingAvForeldreAnsvarDto(
                                            it.fraDato,
                                            it.harForeldreAnsvar,
                                            it.begrunnelse
                                        )
                                    },
                                    navn = vurdertBartIdent.navn,
                                    fødselsdato = vurdertBartIdent.fødselsdato.toLocalDate()
                                )
                            }
                        },
                        vurdertAv =
                            vurderteBarn?.let {
                                VurdertAvResponse(
                                    ident = it.vurdertAv,
                                    dato = it.vurdertTidspunkt.toLocalDate(),
                                    ansattnavn = ansattNavnOgEnhet?.navn,
                                    enhetsnavn = ansattNavnOgEnhet?.enhet
                                )
                            },
                        barnSomTrengerVurdering =
                            uavklarteBarn
                                .map {
                                    hentBarn(
                                        it,
                                        barnGrunnlag
                                    )
                                }.toList()
                    )
                }

                respond(dto)
            }
        }
    }
}

fun hentBarn(ident: BarnIdentifikator, barnGrunnlag: BarnGrunnlag?): IdentifiserteBarnDto {
    val registerBarn = barnGrunnlag?.registerbarn?.barn.orEmpty()
    val oppgitteBarn = barnGrunnlag?.oppgitteBarn?.oppgitteBarn.orEmpty()

    return when (ident) {
        is BarnIdentifikator.BarnIdent -> {
            val barn = registerBarn.singleOrNull { it.ident == ident.ident }
            val oppgittBarn = oppgitteBarn.singleOrNull { it.ident == ident.ident }

            if (barn != null && oppgittBarn != null && barn.ident != oppgittBarn.ident) {
                log.warn("Mismatch mellom idnet for registerbarn og oppgitte barn for ident ${ident.ident}.")
            }

            if (barn != null && oppgittBarn != null && barn.fødselsdato != oppgittBarn.fødselsdato) {
                log.warn("Mismatch mellom fødselsdato registerbarn og oppgitte barn for ident ${ident.ident}.")
            }

            val fødselsdato = if (barn?.fødselsdato != null) {
                barn.fødselsdato
            } else {
                oppgittBarn?.fødselsdato
            }

            IdentifiserteBarnDto(
                ident = ident.ident,
                fodselsDato = fødselsdato?.toLocalDate(),
                navn = oppgittBarn?.navn,
                forsorgerPeriode = fødselsdato?.let { Barn.periodeMedRettTil(fødselsdato) }
            )
        }

        is BarnIdentifikator.NavnOgFødselsdato -> IdentifiserteBarnDto(
            ident = null,
            fodselsDato = ident.fødselsdato.toLocalDate(),
            navn = ident.navn,
            forsorgerPeriode = Barn.periodeMedRettTil(ident.fødselsdato),
        )
    }
}
