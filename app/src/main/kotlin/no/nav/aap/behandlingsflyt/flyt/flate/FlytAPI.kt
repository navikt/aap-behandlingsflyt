package no.nav.aap.behandlingsflyt.flyt.flate

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import com.zaxxer.hikari.HikariDataSource
import io.ktor.http.*
import no.nav.aap.auth.bruker
import no.nav.aap.behandlingsflyt.avklaringsbehov.AvklaringsbehovRepositoryImpl
import no.nav.aap.behandlingsflyt.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.avklaringsbehov.BehandlingTilstandValidator
import no.nav.aap.behandlingsflyt.avklaringsbehov.FrivilligeAvklaringsbehov
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.dbconnect.transaction
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkår
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.flyt.BehandlingFlyt
import no.nav.aap.behandlingsflyt.flyt.utledType
import no.nav.aap.behandlingsflyt.hendelse.mottak.BehandlingHendelseHåndterer
import no.nav.aap.behandlingsflyt.hendelse.mottak.BehandlingSattPåVent
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.behandlingsflyt.sakogbehandling.lås.TaSkriveLåsRepository
import no.nav.aap.behandlingsflyt.server.respondWithStatus
import no.nav.aap.motor.FlytOppgaveRepository
import no.nav.aap.motor.OppgaveInput
import no.nav.aap.motor.OppgaveStatus
import no.nav.aap.verdityper.flyt.StegGruppe
import no.nav.aap.verdityper.flyt.StegType
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import org.slf4j.MDC

fun NormalOpenAPIRoute.flytApi(dataSource: HikariDataSource) {
    route("/api/behandling") {
        route("/{referanse}/flyt") {
            get<BehandlingReferanse, BehandlingFlytOgTilstandDto> { req ->
                val dto = dataSource.transaction(readOnly = true) { connection ->
                    val behandling = behandling(connection, req)
                    val oppgaveRepository = FlytOppgaveRepository(connection)
                    val flyt = utledType(behandling.typeBehandling()).flyt()

                    val stegGrupper: Map<StegGruppe, List<StegType>> =
                        flyt.stegene().groupBy { steg -> steg.gruppe }

                    val aktivtSteg = behandling.aktivtSteg()
                    var erFullført = true
                    val alleAvklaringsbehovInkludertFrivillige = FrivilligeAvklaringsbehov(
                        avklaringsbehov(
                            connection,
                            behandling.id
                        ),
                        flyt, aktivtSteg
                    )
                    val oppgaver = oppgaveRepository.hentOppgaveForBehandling(behandling.id)
                    val prosessering =
                        Prosessering(utledStatus(oppgaver), oppgaver.map { OppgaveDto(it.type(), it.status()) })
                    BehandlingFlytOgTilstandDto(
                        flyt = stegGrupper.map { (gruppe, steg) ->
                            erFullført = erFullført && gruppe != aktivtSteg.gruppe
                            FlytGruppe(
                                stegGruppe = gruppe,
                                erFullført = erFullført,
                                steg = steg.map { stegType ->
                                    FlytSteg(
                                        stegType = stegType,
                                        avklaringsbehov = alleAvklaringsbehovInkludertFrivillige.alle()
                                            .filter { avklaringsbehov -> avklaringsbehov.skalLøsesISteg(stegType) }
                                            .map { behov ->
                                                AvklaringsbehovDTO(
                                                    behov.definisjon,
                                                    behov.status(),
                                                    emptyList()
                                                )
                                            },
                                        vilkårDTO = hentUtRelevantVilkårForSteg(
                                            vilkårsresultat = vilkårResultat(connection, behandling.id),
                                            stegType = stegType
                                        )
                                    )
                                }
                            )
                        },
                        aktivtSteg = aktivtSteg,
                        aktivGruppe = aktivtSteg.gruppe,
                        behandlingVersjon = behandling.versjon,
                        prosessering = prosessering,
                        visning = utledVisning(
                            aktivtSteg = aktivtSteg,
                            flyt = flyt,
                            alleAvklaringsbehovInkludertFrivillige = alleAvklaringsbehovInkludertFrivillige,
                            status = prosessering.status
                        )
                    )
                }
                respond(dto)
            }
        }
        route("/{referanse}/resultat") {
            get<BehandlingReferanse, BehandlingResultatDto> { req ->
                val dto = dataSource.transaction(readOnly = true) { connection ->
                    val behandling = behandling(connection, req)

                    val vilkårResultat = vilkårResultat(connection, behandling.id)

                    BehandlingResultatDto(alleVilkår(vilkårResultat))
                }
                respond(dto)
            }
        }
        route("/{referanse}/sett-på-vent") {
            post<BehandlingReferanse, BehandlingResultatDto, SettPåVentRequest> { request, body ->
                dataSource.transaction { connection ->
                    val taSkriveLåsRepository = TaSkriveLåsRepository(connection)
                    val lås = taSkriveLåsRepository.lås(request.ref())
                    BehandlingTilstandValidator(connection).validerTilstand(
                        request,
                        body.behandlingVersjon
                    )

                    MDC.putCloseable("sakId", lås.sakSkrivelås.id.toString()).use {
                        MDC.putCloseable("behandlingId", lås.behandlingSkrivelås.id.toString()).use {
                            BehandlingHendelseHåndterer(connection).håndtere(
                                key = lås.behandlingSkrivelås.id,
                                hendelse = BehandlingSattPåVent(
                                    frist = body.frist,
                                    begrunnelse = body.begrunnelse,
                                    behandlingVersjon = body.behandlingVersjon,
                                    bruker = pipeline.context.bruker()
                                )
                            )
                            taSkriveLåsRepository.verifiserSkrivelås(lås)
                        }
                    }
                }
                respondWithStatus(HttpStatusCode.NoContent)
            }
        }
        route("/{referanse}/vente-informasjon") {
            get<BehandlingReferanse, Venteinformasjon> { request ->
                val dto = dataSource.transaction(readOnly = true) { connection ->
                    val behandling = behandling(connection, request)
                    val avklaringsbehovene = avklaringsbehov(connection, behandling.id)

                    val ventepunkter = avklaringsbehovene.hentVentepunkter()
                    if (avklaringsbehovene.erSattPåVent()) {
                        val avklaringsbehov = ventepunkter.first()
                        Venteinformasjon(avklaringsbehov.frist(), avklaringsbehov.begrunnelse())
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

private fun utledStatus(oppgaver: List<OppgaveInput>): ProsesseringStatus {
    if (oppgaver.isEmpty()) {
        return ProsesseringStatus.FERDIG
    }
    if (oppgaver.any { it.status() == OppgaveStatus.FEILET }) {
        return ProsesseringStatus.FEILET
    }
    return ProsesseringStatus.JOBBER
}

private fun utledVisning(
    aktivtSteg: StegType,
    flyt: BehandlingFlyt,
    alleAvklaringsbehovInkludertFrivillige: FrivilligeAvklaringsbehov,
    status: ProsesseringStatus
): Visning {
    val jobber = status in listOf(ProsesseringStatus.JOBBER, ProsesseringStatus.FEILET)
    val påVent = alleAvklaringsbehovInkludertFrivillige.erSattPåVent()
    val beslutterReadOnly = aktivtSteg != StegType.FATTE_VEDTAK
    val saksbehandlerReadOnly = !flyt.erStegFør(aktivtSteg, StegType.FATTE_VEDTAK)
    val visBeslutterKort =
        !beslutterReadOnly || (!saksbehandlerReadOnly && alleAvklaringsbehovInkludertFrivillige.harVærtSendtTilbakeFraBeslutterTidligere())

    return Visning(
        saksbehandlerReadOnly = påVent || (!jobber && saksbehandlerReadOnly),
        beslutterReadOnly = påVent || (!jobber && beslutterReadOnly),
        visBeslutterKort = visBeslutterKort,
        visVentekort = påVent
    )

}

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

private fun behandling(connection: DBConnection, req: BehandlingReferanse): Behandling {
    return BehandlingReferanseService(connection).behandling(req)
}

private fun avklaringsbehov(connection: DBConnection, behandlingId: BehandlingId): Avklaringsbehovene {
    return AvklaringsbehovRepositoryImpl(connection).hentAvklaringsbehovene(behandlingId)
}

private fun vilkårResultat(connection: DBConnection, behandlingId: BehandlingId): Vilkårsresultat {
    return VilkårsresultatRepository(connection).hent(behandlingId)
}

private fun hentUtRelevantVilkårForSteg(vilkårsresultat: Vilkårsresultat, stegType: StegType): VilkårDTO? {
    var vilkår: Vilkår? = null
    if (stegType == StegType.AVKLAR_SYKDOM) {
        vilkår = vilkårsresultat.finnVilkår(Vilkårtype.SYKDOMSVILKÅRET)
    }
    if (stegType == StegType.VURDER_ALDER) {
        vilkår = vilkårsresultat.finnVilkår(Vilkårtype.ALDERSVILKÅRET)
    }
    if (stegType == StegType.VURDER_BISTANDSBEHOV) {
        vilkår = vilkårsresultat.finnVilkår(Vilkårtype.BISTANDSVILKÅRET)
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
