package no.nav.aap.behandlingsflyt.behandling.barnetillegg

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.behandling.ansattinfo.AnsattInfoService
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvResponse
import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.BarnFraRegister
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.BarnGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.BarnRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.BarnIdentifikator
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.VurderingAvForeldreAnsvarDto
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.db.PersonRepository
import no.nav.aap.behandlingsflyt.tilgang.kanSaksbehandle
import no.nav.aap.komponenter.dbconnect.transaction
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
                    val personRepository = repositoryProvider.provide<PersonRepository>()

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
                        vurderteBarn?.let { AnsattInfoService().hentAnsattNavnOgEnhet(it.vurdertAv) }

                    val uavklarteBarnIdentifiserbare =
                        tilIdentifiserbartBarnDto(uavklarteBarn, personRepository, barnGrunnlag)

                    BarnetilleggDto(
                        harTilgangTilÅSaksbehandle = kanSaksbehandle(),
                        søknadstidspunkt = sakOgBehandlingService.hentSakFor(behandling.id).rettighetsperiode.fom,
                        folkeregisterbarn = tilIdentifiserbartBarnDto(folkeregister, personRepository, barnGrunnlag),
                        vurderteBarn = vurderteBarn?.barn.orEmpty().map {
                            val vurdertBartIdent = it.ident
                            when (vurdertBartIdent) {
                                is BarnIdentifikator.RegistertBarnPerson -> {
                                    val person = personRepository.hent(vurdertBartIdent.personId)
                                    ExtendedVurdertBarnDto(
                                        person.aktivIdent().identifikator, null,
                                        it.vurderinger.map {
                                            VurderingAvForeldreAnsvarDto(
                                                it.fraDato,
                                                it.harForeldreAnsvar,
                                                it.begrunnelse
                                            )
                                        },
                                        barnGrunnlag?.fødselsdatoFor(vurdertBartIdent)?.toLocalDate()
                                    )
                                }

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

                                is BarnIdentifikator.BarnIdent -> ExtendedVurdertBarnDto(
                                    ident = vurdertBartIdent.ident.identifikator,
                                    vurderinger = it.vurderinger.map {
                                        VurderingAvForeldreAnsvarDto(
                                            it.fraDato,
                                            it.harForeldreAnsvar,
                                            it.begrunnelse
                                        )
                                    },
                                    navn = null,
                                    fødselsdato = null,
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
                        barnSomTrengerVurdering = uavklarteBarnIdentifiserbare
                    )
                }

                respond(dto)
            }
        }
    }
}

private fun tilIdentifiserbartBarnDto(
    uavklarteBarn: Set<BarnIdentifikator>,
    personRepository: PersonRepository,
    barnGrunnlag: BarnGrunnlag?
): List<IdentifiserteBarnDto> {
    val uavklarteBarnIdentifiserbare = uavklarteBarn.map {
        val (identifikator, ident) = when (it) {
            is BarnIdentifikator.BarnIdent -> {
                val lagretPerson = personRepository.finn(it.ident)
                if (lagretPerson != null) {
                    Pair(
                        BarnIdentifikator.RegistertBarnPerson(lagretPerson.id),
                        lagretPerson.aktivIdent()
                    )
                } else {
                    Pair(BarnIdentifikator.BarnIdent(it.ident), it.ident)
                }
            }

            is BarnIdentifikator.NavnOgFødselsdato -> Pair(it, null)
            is BarnIdentifikator.RegistertBarnPerson -> Pair(
                it,
                personRepository.hent(it.personId).aktivIdent()
            )
        }

        val fødselsdato = barnGrunnlag?.fødselsdatoFor(identifikator)
        IdentifiserteBarnDto(
            ident = ident,
            fodselsDato = fødselsdato?.toLocalDate(),
            navn = barnGrunnlag?.finnNavnFor(identifikator),
            forsorgerPeriode = fødselsdato?.let { BarnFraRegister.periodeMedRettTil(it) }
        )
    }
    return uavklarteBarnIdentifiserbare
}

fun BarnGrunnlag.fødselsdatoFor(ident: BarnIdentifikator): Fødselsdato? {
    val fødselsdatoBlantOppgitteBarn =
        this.oppgitteBarn?.oppgitteBarn.orEmpty().find { it.identifikator() == ident }?.fødselsdato
    val fødselssdatoFraRegisterbarn =
        this.registerbarn?.barn.orEmpty().find { it.identifikator() == ident }?.fødselsdato

    return if (fødselssdatoFraRegisterbarn != null) {
        fødselssdatoFraRegisterbarn
    } else {
        fødselsdatoBlantOppgitteBarn
    }
}

fun BarnGrunnlag.finnNavnFor(ident: BarnIdentifikator): String? {
    val navnBlantOppgitteBarn =
        this.oppgitteBarn?.oppgitteBarn.orEmpty().find { it.identifikator() == ident }?.navn

    return navnBlantOppgitteBarn
}

