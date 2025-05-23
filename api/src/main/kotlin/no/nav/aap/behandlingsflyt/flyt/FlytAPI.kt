package no.nav.aap.behandlingsflyt.flyt

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.response.respondWithStatus
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.*
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovOrkestrator
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.BehandlingTilstandValidator
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.FrivilligeAvklaringsbehov
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkår
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.flyt.flate.VilkårDTO
import no.nav.aap.behandlingsflyt.flyt.flate.VilkårsperiodeDTO
import no.nav.aap.behandlingsflyt.flyt.flate.visning.DynamiskStegGruppeVisningService
import no.nav.aap.behandlingsflyt.flyt.flate.visning.ProsesseringStatus
import no.nav.aap.behandlingsflyt.flyt.flate.visning.Visning
import no.nav.aap.behandlingsflyt.hendelse.mottak.BehandlingSattPåVent
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.MANUELT_SATT_PÅ_VENT_KODE
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegGruppe
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.mdc.LogKontekst
import no.nav.aap.behandlingsflyt.mdc.LoggingKontekst
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.behandlingsflyt.sakogbehandling.lås.TaSkriveLåsRepository
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.httpklient.auth.bruker
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbStatus
import no.nav.aap.motor.api.JobbInfoDto
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.Operasjon
import no.nav.aap.tilgang.authorizedGet
import no.nav.aap.tilgang.authorizedPost
import org.slf4j.LoggerFactory
import javax.sql.DataSource

private val log = LoggerFactory.getLogger("flytApi")

fun NormalOpenAPIRoute.flytApi(dataSource: DataSource, repositoryRegistry: RepositoryRegistry) {
    route("/api/behandling") {
        route("/{referanse}/flyt") {
            authorizedGet<BehandlingReferanse, BehandlingFlytOgTilstandDto>(
                AuthorizationParamPathConfig(
                    behandlingPathParam = BehandlingPathParam("referanse")
                )
            ) { req ->
                val dto = dataSource.transaction(readOnly = true) { connection ->
                    val repositoryProvider = repositoryRegistry.provider(connection)
                    val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                    val vilkårsresultatRepository =
                        repositoryProvider.provide<VilkårsresultatRepository>()
                    val avklaringsbehovRepository =
                        repositoryProvider.provide<AvklaringsbehovRepository>()

                    var behandling = behandling(behandlingRepository, req)
                    val avklaringsbehovene = avklaringsbehov(
                        avklaringsbehovRepository,
                        behandling.id
                    )
                    val flytJobbRepository = repositoryProvider.provide<FlytJobbRepository>()
                    val gruppeVisningService = DynamiskStegGruppeVisningService(repositoryProvider)

                    val jobber = flytJobbRepository.hentJobberForBehandling(behandling.id.toLong())
                    val prosessering =
                        Prosessering(
                            utledStatus(jobber, avklaringsbehovene),
                            jobber.map {
                                JobbInfoDto(
                                    id = it.jobbId(),
                                    type = it.type(),
                                    status = it.status(),
                                    planlagtKjøretidspunkt = it.nesteKjøring(),
                                    metadata = mapOf(),
                                    antallFeilendeForsøk = it.antallRetriesForsøkt(),
                                    feilmelding = hentFeilmeldingHvisBehov(
                                        it.status(),
                                        it.jobbId(),
                                        flytJobbRepository
                                    ),
                                    beskrivelse = it.beskrivelse(),
                                    navn = it.navn(),
                                    opprettetTidspunkt = it.opprettetTidspunkt(),
                                )
                            })
                    // Henter denne ut etter status er utledet for å være sikker på at dataene er i rett tilstand
                    behandling = behandling(behandlingRepository, req)
                    val flyt = utledType(behandling.typeBehandling()).flyt()

                    val stegGrupper: Map<StegGruppe, List<StegType>> =
                        flyt.stegene().groupBy { steg -> steg.gruppe }
                    val aktivtSteg = behandling.aktivtSteg()
                    val aktivtStegDefinisjon = Definisjon.fraStegType(aktivtSteg);
                    var erFullført = true

                    val alleAvklaringsbehovInkludertFrivillige = FrivilligeAvklaringsbehov(
                        avklaringsbehovene,
                        flyt, aktivtSteg
                    )
                    val vurdertStegPair =
                        utledVurdertGruppe(prosessering, aktivtSteg, flyt, avklaringsbehovene)
                    val vilkårsresultat = vilkårResultat(vilkårsresultatRepository, behandling.id)
                    val alleAvklaringsbehov = alleAvklaringsbehovInkludertFrivillige.alle()

                    LoggingKontekst(
                        repositoryProvider,
                        LogKontekst(referanse = BehandlingReferanse(req.referanse))
                    ).use {
                        val behandlingVersjon = behandling.versjon
                        log.info("Henter flyt med behandlingversjon: ${behandlingVersjon}")
                    }


                    BehandlingFlytOgTilstandDto(
                        flyt = stegGrupper.map { (gruppe, steg) ->
                            erFullført = erFullført && gruppe != aktivtSteg.gruppe
                            FlytGruppe(
                                stegGruppe = gruppe,
                                skalVises = gruppeVisningService.skalVises(gruppe, behandling.id),
                                erFullført = erFullført,
                                steg = steg.map { stegType ->
                                    FlytSteg(
                                        stegType = stegType,
                                        avklaringsbehov = alleAvklaringsbehov
                                            .filter { avklaringsbehov ->
                                                avklaringsbehov.skalLøsesISteg(
                                                    stegType
                                                )
                                            }
                                            .map { behov ->
                                                AvklaringsbehovDTO(
                                                    behov.definisjon,
                                                    behov.status(),
                                                    emptyList()
                                                )
                                            },
                                        vilkårDTO = hentUtRelevantVilkårForSteg(
                                            vilkårsresultat = vilkårsresultat,
                                            stegType = stegType
                                        )
                                    )
                                }
                            )
                        },
                        aktivtSteg = aktivtSteg,
                        aktivtStegDefinisjon = aktivtStegDefinisjon,
                        aktivGruppe = aktivtSteg.gruppe,
                        vurdertSteg = vurdertStegPair?.second,
                        vurdertGruppe = vurdertStegPair?.first,
                        behandlingVersjon = behandling.versjon,
                        prosessering = prosessering,
                        visning = utledVisning(
                            aktivtSteg = aktivtSteg,
                            flyt = flyt,
                            alleAvklaringsbehovInkludertFrivillige = alleAvklaringsbehovInkludertFrivillige,
                            status = prosessering.status,
                            typeBehandling = behandling.typeBehandling()
                        )
                    )
                }
                respond(dto)
            }
        }
        route("/{referanse}/resultat") {
            authorizedGet<BehandlingReferanse, BehandlingResultatDto>(
                AuthorizationParamPathConfig(
                    behandlingPathParam = BehandlingPathParam("referanse")
                ),
            ) { req ->
                val dto = dataSource.transaction(readOnly = true) { connection ->
                    val repositoryProvider = repositoryRegistry.provider(connection)
                    val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                    val vilkårsresultatRepository =
                        repositoryProvider.provide<VilkårsresultatRepository>()

                    val behandling = behandling(behandlingRepository, req)

                    val vilkårResultat = vilkårResultat(vilkårsresultatRepository, behandling.id)

                    BehandlingResultatDto(alleVilkår(vilkårResultat))
                }
                respond(dto)
            }
        }
        route("/{referanse}/sett-på-vent") {
            authorizedPost<BehandlingReferanse, BehandlingResultatDto, SettPåVentRequest>(
                AuthorizationParamPathConfig(
                    operasjon = Operasjon.SAKSBEHANDLE,
                    behandlingPathParam = BehandlingPathParam("referanse"),
                    avklaringsbehovKode = MANUELT_SATT_PÅ_VENT_KODE
                )
            ) { request, body ->
                dataSource.transaction { connection ->
                    val repositoryProvider = repositoryRegistry.provider(connection)
                    LoggingKontekst(
                        repositoryProvider,
                        LogKontekst(referanse = BehandlingReferanse(request.referanse))
                    ).use {
                        val taSkriveLåsRepository =
                            repositoryProvider.provide<TaSkriveLåsRepository>()
                        val lås = taSkriveLåsRepository.lås(request.referanse)

                        val behandlingRepository =
                            repositoryProvider.provide<BehandlingRepository>()
                        val flytJobbRepository = repositoryProvider.provide<FlytJobbRepository>()
                        BehandlingTilstandValidator(
                            BehandlingReferanseService(behandlingRepository),
                            flytJobbRepository
                        ).validerTilstand(
                            request,
                            body.behandlingVersjon
                        )

                        AvklaringsbehovOrkestrator(repositoryProvider)
                            .settBehandlingPåVent(
                                lås.behandlingSkrivelås.id, BehandlingSattPåVent(
                                    frist = body.frist,
                                    begrunnelse = body.begrunnelse,
                                    behandlingVersjon = body.behandlingVersjon,
                                    grunn = body.grunn,
                                    bruker = bruker()
                                )
                            )
                        taSkriveLåsRepository.verifiserSkrivelås(lås)
                    }
                }
                respondWithStatus(HttpStatusCode.NoContent)
            }
        }
        route("/{referanse}/vente-informasjon") {
            authorizedGet<BehandlingReferanse, Venteinformasjon>(
                AuthorizationParamPathConfig(
                    behandlingPathParam = BehandlingPathParam(
                        "referanse"
                    )
                )
            ) { request ->
                val dto = dataSource.transaction(readOnly = true) { connection ->
                    val repositoryProvider = repositoryRegistry.provider(connection)
                    val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                    val avklaringsbehovRepository =
                        repositoryProvider.provide<AvklaringsbehovRepository>()

                    val behandling = behandling(behandlingRepository, request)
                    val avklaringsbehovene =
                        avklaringsbehov(avklaringsbehovRepository, behandling.id)

                    val ventepunkter = avklaringsbehovene.hentÅpneVentebehov()
                    if (avklaringsbehovene.erSattPåVent()) {
                        val avklaringsbehov = ventepunkter.first()
                        Venteinformasjon(
                            avklaringsbehov.definisjon,
                            avklaringsbehov.frist(),
                            avklaringsbehov.begrunnelse(),
                            requireNotNull(avklaringsbehov.grunn())
                        )
                    } else {
                        null
                    }
                }
                if (dto == null) {
                    respondWithStatus(HttpStatusCode.NoContent)
                } else {
                    respond(dto)
                }
            }
        }
    }
}

// Utleder hvilken visningsgruppe frontend skal rute til for kvalitetsikring / totrinn
private fun utledVurdertGruppe(
    prosessering: Prosessering,
    aktivtSteg: StegType,
    flyt: BehandlingFlyt,
    avklaringsbehovene: Avklaringsbehovene
): Pair<StegGruppe, StegType>? {
    if (prosessering.status == ProsesseringStatus.FERDIG) {
        if (aktivtSteg == StegType.KVALITETSSIKRING) {
            val relevanteBehov = avklaringsbehovene.alle().filter { it.kreverKvalitetssikring() }
                .filter { avklaringsbehov -> avklaringsbehov.erIkkeAvbrutt() }
                .filter { avklaringsbehov -> !avklaringsbehov.erKvalitetssikretTidligere() }

            val skalTilStegForBehov = flyt.skalTilStegForBehov(relevanteBehov)
            val stegType = requireNotNull(skalTilStegForBehov)

            return stegType.gruppe to stegType
        } else if (aktivtSteg == StegType.FATTE_VEDTAK) {
            val relevanteBehov = avklaringsbehovene.alle().filter { it.erTotrinn() }
                .filter { avklaringsbehov -> avklaringsbehov.erIkkeAvbrutt() }
                .filter { avklaringsbehov -> !avklaringsbehov.erTotrinnsVurdert() }

            val skalTilStegForBehov = flyt.skalTilStegForBehov(relevanteBehov)
            val stegType = requireNotNull(skalTilStegForBehov)

            return stegType.gruppe to stegType
        }
    }
    return null
}

private fun hentFeilmeldingHvisBehov(
    status: JobbStatus,
    jobbId: Long,
    flytJobbRepository: FlytJobbRepository
): String? {
    if (status == JobbStatus.FEILET) {
        return flytJobbRepository.hentFeilmeldingForOppgave(jobbId)
    }
    return null
}

private fun utledStatus(oppgaver: List<JobbInput>, avklaringsbehovene: Avklaringsbehovene): ProsesseringStatus {
    if (oppgaver.isEmpty()) {
        if (avklaringsbehovene.harÅpentBrevVentebehov()) {
            return ProsesseringStatus.JOBBER
        }
        return ProsesseringStatus.FERDIG
    }
    if (oppgaver.any { it.status() == JobbStatus.FEILET }) {
        return ProsesseringStatus.FEILET
    }
    return ProsesseringStatus.JOBBER
}

private fun utledVisning(
    aktivtSteg: StegType,
    flyt: BehandlingFlyt,
    alleAvklaringsbehovInkludertFrivillige: FrivilligeAvklaringsbehov,
    status: ProsesseringStatus,
    typeBehandling: TypeBehandling
): Visning {
    val jobberEllerFeilet = status in listOf(ProsesseringStatus.JOBBER, ProsesseringStatus.FEILET)
    val påVent = alleAvklaringsbehovInkludertFrivillige.erSattPåVent()
    val beslutterReadOnly = aktivtSteg != StegType.FATTE_VEDTAK
    val erTilKvalitetssikring =
        harÅpentKvalitetssikringsAvklaringsbehov(alleAvklaringsbehovInkludertFrivillige) && aktivtSteg == StegType.KVALITETSSIKRING
    val saksbehandlerReadOnly = erTilKvalitetssikring || !flyt.erStegFør(aktivtSteg, StegType.FATTE_VEDTAK)
    val visBeslutterKort =
        !beslutterReadOnly || (!saksbehandlerReadOnly && alleAvklaringsbehovInkludertFrivillige.harVærtSendtTilbakeFraBeslutterTidligere())
    val visKvalitetssikringKort = utledVisningAvKvalitetsikrerKort(alleAvklaringsbehovInkludertFrivillige)
    val kvalitetssikringReadOnly = visKvalitetssikringKort && flyt.erStegFør(aktivtSteg, StegType.KVALITETSSIKRING)
    val visBrevkort =
        alleAvklaringsbehovInkludertFrivillige.hentBehovForDefinisjon(Definisjon.SKRIV_BREV)?.erÅpent() == true ||
                alleAvklaringsbehovInkludertFrivillige.hentBehovForDefinisjon(Definisjon.SKRIV_FORHÅNDSVARSEL_BRUDD_AKTIVITETSPLIKT_BREV)
                    ?.erÅpent() == true

    if (jobberEllerFeilet) {
        return Visning(
            saksbehandlerReadOnly = true,
            beslutterReadOnly = true,
            kvalitetssikringReadOnly = true,
            visBeslutterKort = visBeslutterKort,
            visKvalitetssikringKort = visKvalitetssikringKort,
            visVentekort = påVent,
            visBrevkort = false,
            typeBehandling = typeBehandling
        )
    } else {
        return Visning(
            saksbehandlerReadOnly = påVent || saksbehandlerReadOnly,
            beslutterReadOnly = påVent || beslutterReadOnly,
            kvalitetssikringReadOnly = påVent || kvalitetssikringReadOnly,
            visBeslutterKort = visBeslutterKort,
            visKvalitetssikringKort = visKvalitetssikringKort,
            visVentekort = påVent,
            visBrevkort = visBrevkort,
            typeBehandling = typeBehandling
        )
    }
}

private fun utledVisningAvKvalitetsikrerKort(
    avklaringsbehovene: FrivilligeAvklaringsbehov
): Boolean {
    if (avklaringsbehovene.skalTilbakeføresEtterKvalitetssikring()) {
        return true
    }
    if (harÅpentKvalitetssikringsAvklaringsbehov(avklaringsbehovene)) {
        return true
    }
    return false
}

private fun harÅpentKvalitetssikringsAvklaringsbehov(avklaringsbehovene: FrivilligeAvklaringsbehov): Boolean =
    avklaringsbehovene.hentBehovForDefinisjon(Definisjon.KVALITETSSIKRING)?.erÅpent() == true

private fun alleVilkår(vilkårResultat: Vilkårsresultat): List<VilkårDTO> {
    return vilkårResultat.alle().map { vilkår ->
        VilkårDTO(
            vilkår.type,
            perioder = vilkår.vilkårsperioder().map { vp ->
                VilkårsperiodeDTO(
                    vp.periode,
                    vp.utfall,
                    vp.manuellVurdering,
                    vp.begrunnelse,
                    vp.avslagsårsak,
                    vp.innvilgelsesårsak
                )
            })
    }
}

private fun behandling(behandlingRepository: BehandlingRepository, req: BehandlingReferanse): Behandling {
    return BehandlingReferanseService(behandlingRepository).behandling(req)
}

private fun avklaringsbehov(
    avklaringsbehovRepository: AvklaringsbehovRepository,
    behandlingId: BehandlingId
): Avklaringsbehovene {
    return avklaringsbehovRepository.hentAvklaringsbehovene(behandlingId)
}

private fun vilkårResultat(
    vilkårsresultatRepository: VilkårsresultatRepository,
    behandlingId: BehandlingId
): Vilkårsresultat {
    return vilkårsresultatRepository.hent(behandlingId)
}

private fun hentUtRelevantVilkårForSteg(vilkårsresultat: Vilkårsresultat, stegType: StegType): VilkårDTO? {
    var vilkår: Vilkår? = null
    if (stegType == StegType.AVKLAR_SYKDOM) {
        vilkår = vilkårsresultat.optionalVilkår(Vilkårtype.SYKDOMSVILKÅRET)
    }
    if (stegType == StegType.VURDER_ALDER) {
        vilkår = vilkårsresultat.optionalVilkår(Vilkårtype.ALDERSVILKÅRET)
    }
    if (stegType == StegType.VURDER_BISTANDSBEHOV) {
        vilkår = vilkårsresultat.optionalVilkår(Vilkårtype.BISTANDSVILKÅRET)
    }
    if (stegType == StegType.VURDER_LOVVALG) {
        vilkår = vilkårsresultat.optionalVilkår(Vilkårtype.LOVVALG)
    }
    if (stegType == StegType.VURDER_MEDLEMSKAP) {
        vilkår = vilkårsresultat.optionalVilkår(Vilkårtype.MEDLEMSKAP)
    }
    if (vilkår == null) {
        return null
    }
    return VilkårDTO(
        vilkår.type,
        perioder = vilkår.vilkårsperioder().map { vp ->
            VilkårsperiodeDTO(
                vp.periode,
                vp.utfall,
                vp.manuellVurdering,
                vp.begrunnelse,
                vp.avslagsårsak,
                vp.innvilgelsesårsak
            )
        })
}
