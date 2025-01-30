package no.nav.aap.behandlingsflyt.behandling.lovvalgmedlemskap

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.ÅrsakTilSettPåVent
import no.nav.aap.behandlingsflyt.behandling.dokumentinnhenting.BestillLegeerklæringDto
import no.nav.aap.behandlingsflyt.behandling.dokumentinnhenting.ForhåndsvisBrevRequest
import no.nav.aap.behandlingsflyt.behandling.dokumentinnhenting.HentStatusLegeerklæring
import no.nav.aap.behandlingsflyt.behandling.dokumentinnhenting.PurringLegeerklæring
import no.nav.aap.behandlingsflyt.behandling.lovvalg.MedlemskapLovvalgGrunnlag
import no.nav.aap.behandlingsflyt.behandling.vilkår.medlemskap.KanBehandlesAutomatiskVurdering
import no.nav.aap.behandlingsflyt.behandling.vilkår.medlemskap.MedlemskapLovvalgService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.dokumentinnhenting.BrevRequest
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.dokumentinnhenting.BrevResponse
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.dokumentinnhenting.DokumeninnhentingGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.dokumentinnhenting.LegeerklæringBestillingRequest
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.dokumentinnhenting.LegeerklæringPurringRequest
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.dokumentinnhenting.LegeerklæringStatusResponse
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.MedlemskapArbeidInntektRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonopplysningRepository
import no.nav.aap.behandlingsflyt.hendelse.avløp.BehandlingHendelseServiceImpl
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.mdc.LogKontekst
import no.nav.aap.behandlingsflyt.mdc.LoggingKontekst
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.lås.TaSkriveLåsRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersoninfoGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.httpklient.auth.bruker
import no.nav.aap.komponenter.httpklient.auth.token
import no.nav.aap.lookup.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.tilgang.AuthorizationBodyPathConfig
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.SakPathParam
import no.nav.aap.tilgang.authorizedGet
import no.nav.aap.tilgang.authorizedPost
import tilgang.Operasjon
import java.time.LocalDate
import java.time.Period
import java.util.*
import javax.sql.DataSource

fun NormalOpenAPIRoute.lovvalgMedlemskapAPI(dataSource: DataSource) {
    route("/api/lovvalgmedlemskap/") {
        route("/vurdering") {
            authorizedPost<Unit, KanBehandlesAutomatiskVurdering, LovvalgMedlemskapVurderingRequest>(
                AuthorizationBodyPathConfig(
                    operasjon = Operasjon.SE,
                    applicationsOnly = false
                )
            )
            { _, req ->
                val vurdering = dataSource.transaction { connection ->
                    val repositoryProvider = RepositoryProvider(connection)
                    val behandling = repositoryProvider.provide<BehandlingRepository>().hent(BehandlingReferanse(req.behandlingsReferanse))
                    val sak = repositoryProvider.provide<SakRepository>().hent(behandling.sakId)

                    val personopplysningGrunnlag = repositoryProvider.provide<PersonopplysningRepository>().hentHvisEksisterer(behandling.id)
                        ?: throw IllegalStateException("Forventet å finne personopplysninger")
                    val medlemskapArbeidInntektGrunnlag = repositoryProvider.provide<MedlemskapArbeidInntektRepository>().hentHvisEksisterer(behandling.id)
                    val oppgittUtenlandsOppholdGrunnlag = repositoryProvider.provide<MedlemskapArbeidInntektRepository>().hentOppgittUtenlandsOppholdHvisEksisterer(behandling.id)
                        ?: throw IllegalStateException("Forventet å finne utenlandsoppylsninger")

                    MedlemskapLovvalgService().vurderTilhørighet(
                        MedlemskapLovvalgGrunnlag(medlemskapArbeidInntektGrunnlag, personopplysningGrunnlag, oppgittUtenlandsOppholdGrunnlag),
                        sak.rettighetsperiode
                    )
                }

                respond(vurdering)
            }
        }
    }
}

data class LovvalgMedlemskapVurderingRequest(
    val behandlingsReferanse: UUID
)