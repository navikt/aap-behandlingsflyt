package no.nav.aap.behandlingsflyt.behandling.barnetillegg

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.behandling.ansattinfo.AnsattInfoService
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvResponse
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.Barn
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.BarnGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.BarnRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.BarnIdentifikator
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.BarnIdentifikator.BarnIdent
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.VurderingAvForeldreAnsvarDto
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.behandlingsflyt.tilgang.kanSaksbehandle
import no.nav.aap.behandlingsflyt.tilgang.relevanteIdenterForBehandlingResolver
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.getGrunnlag
import org.slf4j.LoggerFactory
import javax.sql.DataSource

private val log = LoggerFactory.getLogger("barnetilleggApi")

fun NormalOpenAPIRoute.barnetilleggApi(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry,
    gatewayProvider: GatewayProvider,
) {
    val ansattInfoService = AnsattInfoService(gatewayProvider)
    route("/api/barnetillegg") {
        route("/grunnlag/{referanse}") {
            getGrunnlag<BehandlingReferanse, BarnetilleggDto>(
                relevanteIdenterResolver = relevanteIdenterForBehandlingResolver(repositoryRegistry, dataSource),
                behandlingPathParam = BehandlingPathParam("referanse"),
                avklaringsbehovKode = Definisjon.AVKLAR_BARNETILLEGG.kode.toString()
            ) { req ->
                val dto = dataSource.transaction(readOnly = true) { connection ->
                    val repositoryProvider = repositoryRegistry.provider(connection)
                    val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()

                    val behandling: Behandling =
                        BehandlingReferanseService(behandlingRepository).behandling(req)
                    val barnRepository = repositoryProvider.provide<BarnRepository>()

                    val barnetilleggService = BarnetilleggService(
                        repositoryProvider,
                        gatewayProvider,
                    )
                    val barnetilleggTidslinje = barnetilleggService.beregn(behandling.id)
                    val barnGrunnlag = barnRepository.hentHvisEksisterer(behandling.id)

                    val folkeregister = barnGrunnlag?.registerbarn?.barn.orEmpty()

                    log.info("Fant ${folkeregister.size} folkeregister-barn for behandling ${behandling.referanse}.")

                    val uavklarteBarn =
                        barnetilleggTidslinje.segmenter().map { it.verdi.barnTilAvklaring() }.flatten().toSet()

                    val vurderteBarn = barnRepository.hentVurderteBarnHvisEksisterer(behandling.id)

                    val ansattNavnOgEnhet =
                        vurderteBarn?.let { ansattInfoService.hentAnsattNavnOgEnhet(it.vurdertAv) }

                    val vurderteBarnDto = vurderteBarn?.barn.orEmpty().map {
                        val barn = hentBarn(it.ident, barnGrunnlag)
                        when (val vurdertBartIdent = it.ident) {
                            is BarnIdent -> ExtendedVurdertBarnDto(
                                ident = vurdertBartIdent.ident.identifikator, null,
                                vurderinger = it.vurderinger.map {
                                    VurderingAvForeldreAnsvarDto(
                                        fraDato = it.fraDato,
                                        harForeldreAnsvar = it.harForeldreAnsvar,
                                        begrunnelse = it.begrunnelse,
                                        erFosterForelder = it.erFosterForelder,
                                    )
                                },
                                fødselsdato = barn.fodselsDato,
                                oppgittForeldreRelasjon = barn.oppgittForeldreRelasjon,
                            )

                            is BarnIdentifikator.NavnOgFødselsdato -> ExtendedVurdertBarnDto(
                                ident = null,
                                vurderinger = it.vurderinger.map {
                                    VurderingAvForeldreAnsvarDto(
                                        fraDato = it.fraDato,
                                        harForeldreAnsvar = it.harForeldreAnsvar,
                                        begrunnelse = it.begrunnelse,
                                        erFosterForelder = it.erFosterForelder,
                                    )
                                },
                                navn = vurdertBartIdent.navn,
                                fødselsdato = vurdertBartIdent.fødselsdato.toLocalDate(),
                                oppgittForeldreRelasjon = barn.oppgittForeldreRelasjon,

                                )
                        }
                    }

                    val (vurderteFolkeregisterBarnDto, vurderteManuelleBarnDto) = vurderteBarnDto.partition { barn ->
                        barn.ident?.let { ident ->
                            folkeregister.any { b -> b.ident.er(BarnIdent(ident)) }
                        } ?: false
                    }

                    BarnetilleggDto(
                        harTilgangTilÅSaksbehandle = kanSaksbehandle(),
                        søknadstidspunkt = SakService(repositoryProvider).hentSakFor(behandling.id).rettighetsperiode.fom,
                        folkeregisterbarn = folkeregister.map {
                            hentBarn(
                                it.ident,
                                barnGrunnlag
                            )
                        },
                        vurderteBarn = vurderteManuelleBarnDto,
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
                                }.toList(),
                        vurderteFolkeregisterBarn = vurderteFolkeregisterBarnDto,
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
        is BarnIdent -> {
            val barn = registerBarn.singleOrNull { it.ident == ident }
            val oppgittBarn = oppgitteBarn.singleOrNull { it.ident == ident.ident }

            if (barn != null && oppgittBarn != null && barn.ident != oppgittBarn.ident?.let(::BarnIdent)?.ident) {
                log.warn("Mismatch mellom ident for registerbarn og oppgitte barn for ident ${ident.ident}.")
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
                forsorgerPeriode = fødselsdato?.let { Barn.periodeMedRettTil(fødselsdato, barn?.dødsdato) },
                oppgittForeldreRelasjon = oppgittBarn?.relasjon
            )
        }

        is BarnIdentifikator.NavnOgFødselsdato -> {
            val oppgittBarn = oppgitteBarn.singleOrNull { it.fødselsdato == ident.fødselsdato && it.navn == ident.navn }
            IdentifiserteBarnDto(
                ident = null,
                fodselsDato = ident.fødselsdato.toLocalDate(),
                navn = ident.navn,
                forsorgerPeriode = Barn.periodeMedRettTil(ident.fødselsdato, null),
                oppgittForeldreRelasjon = oppgittBarn?.relasjon
            )
        }
    }
}
