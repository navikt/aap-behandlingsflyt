package no.nav.aap.behandlingsflyt.behandling.lovvalgmedlemskap

import com.papsign.ktor.openapigen.route.TagModule
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.Tags
import no.nav.aap.behandlingsflyt.behandling.lovvalg.MedlemskapLovvalgGrunnlag
import no.nav.aap.behandlingsflyt.behandling.vilkår.medlemskap.KanBehandlesAutomatiskVurdering
import no.nav.aap.behandlingsflyt.behandling.vilkår.medlemskap.MedlemskapLovvalgVurderingService
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.MedlemskapArbeidInntektRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonopplysningRepository
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.authorizedGet
import javax.sql.DataSource

fun NormalOpenAPIRoute.lovvalgMedlemskapAPI(dataSource: DataSource) {
    route("/api/lovvalgmedlemskap/") {
        route("/vurdering/{referanse}") {
            authorizedGet<BehandlingReferanse, KanBehandlesAutomatiskVurdering>(
                AuthorizationParamPathConfig(behandlingPathParam = BehandlingPathParam("referanse")),
            null, TagModule(listOf(Tags.Behandling))
            ) { req ->
                val vurdering = dataSource.transaction { connection ->
                    val repositoryProvider = RepositoryProvider(connection)
                    val behandling = repositoryProvider.provide<BehandlingRepository>().hent(BehandlingReferanse(req.referanse))
                    val sak = repositoryProvider.provide<SakRepository>().hent(behandling.sakId)

                    val personopplysningGrunnlag = repositoryProvider.provide<PersonopplysningRepository>().hentHvisEksisterer(behandling.id)
                        ?: throw IllegalStateException("Forventet å finne personopplysninger")
                    val medlemskapArbeidInntektGrunnlag = repositoryProvider.provide<MedlemskapArbeidInntektRepository>().hentHvisEksisterer(behandling.id)
                    val oppgittUtenlandsOppholdGrunnlag = repositoryProvider.provide<MedlemskapArbeidInntektRepository>().hentOppgittUtenlandsOppholdHvisEksisterer(behandling.id)

                    MedlemskapLovvalgVurderingService().vurderTilhørighet(
                        MedlemskapLovvalgGrunnlag(medlemskapArbeidInntektGrunnlag, personopplysningGrunnlag, oppgittUtenlandsOppholdGrunnlag),
                        sak.rettighetsperiode
                    )
                }
                respond(vurdering)
            }
        }
    }
}