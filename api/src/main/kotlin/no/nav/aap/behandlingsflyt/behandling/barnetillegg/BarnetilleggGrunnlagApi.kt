package no.nav.aap.behandlingsflyt.behandling.barnetillegg

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.behandling.ansattinfo.AnsattInfoService
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvResponse
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.Barn
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.BarnGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.BarnRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.OppgitteBarn
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.SaksbehandlerOppgitteBarn.SaksbehandlerOppgitteBarn
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.BarnIdentifikator
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.BarnIdentifikator.BarnIdent
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.VurderingAvForeldreAnsvarDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.VurdertBarn
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
import no.nav.aap.komponenter.repository.RepositoryProvider
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

                    val behandling: Behandling = BehandlingReferanseService(behandlingRepository).behandling(req)

                    opprettBarnetilleggDto(
                        behandling,
                        repositoryProvider,
                        gatewayProvider,
                        ansattInfoService,
                        kanSaksbehandle()
                    )
                }

                respond(dto)
            }
        }
    }
}

private fun opprettBarnetilleggDto(
    behandling: Behandling,
    repositoryProvider: RepositoryProvider,
    gatewayProvider: GatewayProvider,
    ansattInfoService: AnsattInfoService,
    harTilgangTilÅSaksbehandle: Boolean
): BarnetilleggDto {
    val barnRepository = repositoryProvider.provide<BarnRepository>()
    val barnetilleggService = BarnetilleggService(repositoryProvider, gatewayProvider)
    val barnetilleggTidslinje = barnetilleggService.beregn(behandling.id)
    val barnGrunnlag = barnRepository.hentHvisEksisterer(behandling.id)

    val folkeregister = barnGrunnlag?.registerbarn?.barn.orEmpty()
    log.info("Fant ${folkeregister.size} folkeregister-barn for behandling ${behandling.referanse}.")

    val saksbehandlerOppgittBarn = barnGrunnlag?.saksbehandlerOppgitteBarn?.barn.orEmpty()
    val uavklarteBarn = barnetilleggTidslinje.segmenter().map { it.verdi.barnTilAvklaring() }.flatten().toSet()
    val vurderteBarn = barnRepository.hentVurderteBarnHvisEksisterer(behandling.id)
    val ansattNavnOgEnhet = vurderteBarn?.let { ansattInfoService.hentAnsattNavnOgEnhet(it.vurdertAv) }

    val vurderteBarnDto = vurderteBarn?.barn.orEmpty().map {
        mapTilExtendedVurdertBarnDto(it, barnGrunnlag)
    }

    val oppgitteBarn = barnGrunnlag?.oppgitteBarn?.oppgitteBarn.orEmpty()
    val vurderteFolkeregisterBarnDto = filtrerFolkeregisterBarn(vurderteBarnDto, folkeregister)
    val vurderteSaksbehandlerOppgittBarnDto =
        filtrerSaksbehandlerOppgittBarn(vurderteBarnDto, saksbehandlerOppgittBarn, barnRepository, behandling)
    val vurderteOppgittBarnDto = filtrerOppgitteBarn(vurderteBarnDto, oppgitteBarn)

    return BarnetilleggDto(
        harTilgangTilÅSaksbehandle = harTilgangTilÅSaksbehandle,
        søknadstidspunkt = SakService(repositoryProvider).hentSakFor(behandling.id).rettighetsperiode.fom,
        folkeregisterbarn = folkeregister.map { hentBarn(it.ident, barnGrunnlag) },
        saksbehandlerOppgitteBarn = saksbehandlerOppgittBarn.map { hentBarn(it.identifikator(), barnGrunnlag) },
        vurderteBarn = vurderteOppgittBarnDto,
        vurdertAv = vurderteBarn?.let {
            VurdertAvResponse(
                ident = it.vurdertAv,
                dato = it.vurdertTidspunkt.toLocalDate(),
                ansattnavn = ansattNavnOgEnhet?.navn,
                enhetsnavn = ansattNavnOgEnhet?.enhet
            )
        },
        barnSomTrengerVurdering = uavklarteBarn.map { hentBarn(it, barnGrunnlag) },
        vurderteFolkeregisterBarn = vurderteFolkeregisterBarnDto,
        vurderteSaksbehandlerOppgitteBarn = vurderteSaksbehandlerOppgittBarnDto
    )
}

private fun filtrerFolkeregisterBarn(
    vurderteBarn: List<ExtendedVurdertBarnDto>,
    folkeregisterBarn: List<Barn>
): List<ExtendedVurdertBarnDto> = vurderteBarn.filter { barn ->
    barn.ident?.let { ident ->
        folkeregisterBarn.any { it.ident.er(BarnIdent(ident)) }
    } ?: false
}

private fun filtrerSaksbehandlerOppgittBarn(
    vurderteBarn: List<ExtendedVurdertBarnDto>,
    saksbehandlerOppgittBarn: List<SaksbehandlerOppgitteBarn>,
    barnRepository: BarnRepository,
    behandling: Behandling
): List<SlettbarVurdertBarnDto> {
    val nyeBarnIDenneBehandling = hentNyeSaksbehandlerOppgitteBarnFor(behandling, barnRepository)
    val filtrerteSaksbehandlerOppgittBarn = vurderteBarn.filter { vurdertBarn ->
        saksbehandlerOppgittBarn.any { sob ->
            matcherBarn(vurdertBarn, sob)
        }
    }

    return filtrerteSaksbehandlerOppgittBarn.map { vurdertBarn ->
        val erNyttBarn = nyeBarnIDenneBehandling.any { nyttBarn ->
            matcherBarn(vurdertBarn, nyttBarn)
        }
        SlettbarVurdertBarnDto(
            vurdertBarn = vurdertBarn,
            erSlettbar = erNyttBarn
        )
    }
}

private fun hentNyeSaksbehandlerOppgitteBarnFor(
    behandling: Behandling,
    barnRepository: BarnRepository
): List<SaksbehandlerOppgitteBarn> {
    val tidligereBarnGrunnlag = behandling.forrigeBehandlingId?.let { barnRepository.hentHvisEksisterer(it) }
    val gjeldendeBarnGrunnlag = barnRepository.hentHvisEksisterer(behandling.id)

    val tidligereSaksbehandlerOppgitteBarn = tidligereBarnGrunnlag?.saksbehandlerOppgitteBarn?.barn.orEmpty()
    val gjeldendeSaksbehandlerOppgitteBarn = gjeldendeBarnGrunnlag?.saksbehandlerOppgitteBarn?.barn.orEmpty()

    return gjeldendeSaksbehandlerOppgitteBarn.filter { gjeldendeBarn ->
        tidligereSaksbehandlerOppgitteBarn.none { tidligereBarn ->
            matcherSaksbehandlerOppgitteBarn(tidligereBarn, gjeldendeBarn)
        }
    }
}

private fun matcherSaksbehandlerOppgitteBarn(
    barn1: SaksbehandlerOppgitteBarn,
    barn2: SaksbehandlerOppgitteBarn
): Boolean {
    val ident1 = barn1.identifikator()
    val ident2 = barn2.identifikator()

    return if (ident1 is BarnIdent && ident2 is BarnIdent) {
        ident1.er(ident2)
    } else {
        barn1.navn == barn2.navn && barn1.fødselsdato == barn2.fødselsdato
    }
}

private fun matcherBarn(
    vurdertBarn: ExtendedVurdertBarnDto,
    saksbehandlerOppgittBarn: SaksbehandlerOppgitteBarn
): Boolean {
    return vurdertBarn.ident?.let { ident ->
        when (val identifikator = saksbehandlerOppgittBarn.identifikator()) {
            is BarnIdent -> identifikator.er(BarnIdent(ident))
            else -> false
        }
    } ?: (saksbehandlerOppgittBarn.navn == vurdertBarn.navn &&
            saksbehandlerOppgittBarn.fødselsdato.toLocalDate() == vurdertBarn.fødselsdato)
}

private fun filtrerOppgitteBarn(
    vurderteBarn: List<ExtendedVurdertBarnDto>,
    oppgitteBarn: List<OppgitteBarn.OppgittBarn>
): List<ExtendedVurdertBarnDto> = vurderteBarn.filter { barn ->
    barn.ident?.let { ident ->
        oppgitteBarn.any { it.ident?.let { BarnIdent(it) }?.er(BarnIdent(ident)) == true }
    } ?: run {
        // Barn uten ident - match på navn og fødselsdato
        oppgitteBarn.any { it.navn == barn.navn && it.fødselsdato?.toLocalDate() == barn.fødselsdato }
    }
}

private fun mapTilExtendedVurdertBarnDto(
    vurdertBarn: VurdertBarn,
    barnGrunnlag: BarnGrunnlag?
): ExtendedVurdertBarnDto {
    val barn = hentBarn(vurdertBarn.ident, barnGrunnlag)
    val vurderinger = vurdertBarn.vurderinger.map {
        VurderingAvForeldreAnsvarDto(
            fraDato = it.fraDato,
            harForeldreAnsvar = it.harForeldreAnsvar,
            begrunnelse = it.begrunnelse,
            erFosterForelder = it.erFosterForelder,
        )
    }

    return when (val ident = vurdertBarn.ident) {
        is BarnIdent -> ExtendedVurdertBarnDto(
            ident = ident.ident.identifikator,
            navn = null,
            vurderinger = vurderinger,
            fødselsdato = barn.fodselsDato,
            oppgittForeldreRelasjon = barn.oppgittForeldreRelasjon,
        )

        is BarnIdentifikator.NavnOgFødselsdato -> ExtendedVurdertBarnDto(
            ident = null,
            navn = ident.navn,
            vurderinger = vurderinger,
            fødselsdato = ident.fødselsdato.toLocalDate(),
            oppgittForeldreRelasjon = barn.oppgittForeldreRelasjon,
        )
    }
}

fun hentBarn(ident: BarnIdentifikator, barnGrunnlag: BarnGrunnlag?): IdentifiserteBarnDto {
    return when (ident) {
        is BarnIdent -> hentBarnMedIdent(ident, barnGrunnlag)
        is BarnIdentifikator.NavnOgFødselsdato -> hentBarnUtenIdent(ident, barnGrunnlag)
    }
}

private fun hentBarnMedIdent(ident: BarnIdent, barnGrunnlag: BarnGrunnlag?): IdentifiserteBarnDto {
    val registerBarn = barnGrunnlag?.registerbarn?.barn?.singleOrNull { it.ident == ident }
    val oppgittBarn = barnGrunnlag?.oppgitteBarn?.oppgitteBarn?.singleOrNull { it.ident == ident.ident }
    val saksbehandlerOppgittBarn =
        barnGrunnlag?.saksbehandlerOppgitteBarn?.barn?.singleOrNull { it.ident == ident.ident }

    validerBarnData(registerBarn, oppgittBarn, saksbehandlerOppgittBarn, ident)

    val fødselsdato = registerBarn?.fødselsdato ?: oppgittBarn?.fødselsdato ?: saksbehandlerOppgittBarn?.fødselsdato
    ?: error("Fødselsdato mangler for barn med ident ${ident.ident}")

    return IdentifiserteBarnDto(
        ident = ident.ident,
        fodselsDato = fødselsdato.toLocalDate(),
        navn = oppgittBarn?.navn ?: saksbehandlerOppgittBarn?.navn,
        forsorgerPeriode = Barn.periodeMedRettTil(fødselsdato, registerBarn?.dødsdato),
        oppgittForeldreRelasjon = oppgittBarn?.relasjon ?: saksbehandlerOppgittBarn?.relasjon
    )
}

private fun hentBarnUtenIdent(
    ident: BarnIdentifikator.NavnOgFødselsdato,
    barnGrunnlag: BarnGrunnlag?
): IdentifiserteBarnDto {
    val oppgittBarn = barnGrunnlag?.oppgitteBarn?.oppgitteBarn?.singleOrNull {
        it.fødselsdato == ident.fødselsdato && it.navn == ident.navn
    }
    val saksbehandlerOppgittBarn = barnGrunnlag?.saksbehandlerOppgitteBarn?.barn?.singleOrNull {
        it.fødselsdato == ident.fødselsdato && it.navn == ident.navn
    }

    return IdentifiserteBarnDto(
        ident = null,
        fodselsDato = ident.fødselsdato.toLocalDate(),
        navn = ident.navn,
        forsorgerPeriode = ident.fødselsdato.let { Barn.periodeMedRettTil(it, null) },
        oppgittForeldreRelasjon = oppgittBarn?.relasjon ?: saksbehandlerOppgittBarn?.relasjon
    )
}

private fun validerBarnData(
    registerBarn: Barn?,
    oppgittBarn: OppgitteBarn.OppgittBarn?,
    barn: SaksbehandlerOppgitteBarn?,
    ident: BarnIdent
) {
    if (registerBarn != null && oppgittBarn != null) {
        val oppgittIdent = oppgittBarn.ident?.let { BarnIdent(it) }
        if (registerBarn.ident != oppgittIdent) {
            log.warn("Mismatch mellom ident for registerbarn og oppgitte barn for ident ${ident.ident}.")
        }
        if (registerBarn.fødselsdato != oppgittBarn.fødselsdato) {
            log.warn("Mismatch mellom fødselsdato registerbarn og oppgitte barn for ident ${ident.ident}.")
        }
    }

    if (registerBarn != null && barn != null) {
        if (registerBarn.ident != barn.ident) {
            log.warn("Mismatch mellom ident for registerbarn og saksbehandler oppgitte barn for ident ${ident.ident}.")
        }
        if (registerBarn.fødselsdato != barn.fødselsdato) {
            log.warn("Mismatch mellom fødselsdato registerbarn og saksbehandler oppgitte barn for ident ${ident.ident}.")
        }
    }
}
