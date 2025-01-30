package no.nav.aap.behandlingsflyt.behandling.lovvalgmedlemskap

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.behandling.lovvalg.MedlemskapLovvalgGrunnlag
import no.nav.aap.behandlingsflyt.behandling.vilkår.medlemskap.KanBehandlesAutomatiskVurdering
import no.nav.aap.behandlingsflyt.behandling.vilkår.medlemskap.MedlemskapLovvalgService
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.MedlemskapArbeidInntektRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonopplysningRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.tilgang.AuthorizationBodyPathConfig
import no.nav.aap.tilgang.authorizedPost
import no.nav.aap.tilgang.plugin.kontrakt.Behandlingsreferanse
import tilgang.Operasjon
import java.util.*
import javax.sql.DataSource

fun NormalOpenAPIRoute.lovvalgMedlemskapAPI(dataSource: DataSource) {
    route("/api/lovvalgmedlemskap/") {
        route("/vurdering") {
            authorizedPost<Unit, KanBehandlesAutomatiskVurdering, LovvalgMedlemskapVurderingRequest>(
                AuthorizationBodyPathConfig(
                    operasjon = Operasjon.SE,
                    applicationRole = "medlemskaplovvalg-api",
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
): Behandlingsreferanse {
    override fun hentAvklaringsbehovKode(): String? {
        return Definisjon.AVKLAR_LOVVALG_MEDLEMSKAP.toString()
    }

    override fun hentBehandlingsreferanse(): String {
        return behandlingsReferanse.toString()
    }
}