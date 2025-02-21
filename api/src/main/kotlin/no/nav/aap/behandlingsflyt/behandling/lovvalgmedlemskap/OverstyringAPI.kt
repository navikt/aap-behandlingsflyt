package no.nav.aap.behandlingsflyt.behandling.lovvalgmedlemskap

import com.fasterxml.jackson.annotation.JsonProperty
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respondWithStatus
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.*
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.BehandlingTilstandValidator
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.mdc.LogKontekst
import no.nav.aap.behandlingsflyt.mdc.LoggingKontekst
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.behandlingsflyt.sakogbehandling.lås.TaSkriveLåsRepository
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.tilgang.AuthorizationBodyPathConfig
import no.nav.aap.tilgang.Operasjon
import no.nav.aap.tilgang.authorizedPost
import java.util.*
import javax.sql.DataSource

fun NormalOpenAPIRoute.overstyringAPI(dataSource: DataSource) {
    route("/api/lovvalgmedlemskap") {
        route("/overstyr") {
            authorizedPost<Unit, Unit, ManuellOverstyring>(
                AuthorizationBodyPathConfig(
                    operasjon = Operasjon.SAKSBEHANDLE
                )
            ) { _, request ->
                dataSource.transaction { connection ->
                    val repositoryProvider = RepositoryProvider(connection)
                    val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                    val taSkriveLåsRepository = repositoryProvider.provide<TaSkriveLåsRepository>()
                    val avklaringsbehovRepository = repositoryProvider.provide<AvklaringsbehovRepository>()

                    LoggingKontekst(
                        repositoryProvider,
                        LogKontekst(referanse = BehandlingReferanse(request.referanse))
                    ).use {
                        val lås = taSkriveLåsRepository.lås(request.referanse)
                        val flytJobbRepository = FlytJobbRepository(connection)
                        BehandlingTilstandValidator(
                            BehandlingReferanseService(behandlingRepository),
                            flytJobbRepository
                        ).validerTilstand(
                            BehandlingReferanse(request.referanse), request.behandlingVersjon
                        )

                        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(lås.behandlingSkrivelås.id)

                        when (request.overstyringType) {
                            OverstyringType.LOVVALG -> avklaringsbehovene.leggTil(
                                definisjoner = listOf(Definisjon.MANUELL_OVERSTYRING_LOVVALG),
                                funnetISteg = StegType.VURDER_LOVVALG
                            )
                            OverstyringType.MEDLEMSKAP -> avklaringsbehovene.leggTil(
                                definisjoner = listOf(Definisjon.MANUELL_OVERSTYRING_MEDLEMSKAP),
                                funnetISteg = StegType.VURDER_MEDLEMSKAP
                            )
                        }

                        taSkriveLåsRepository.verifiserSkrivelås(lås)
                    }
                }
                respondWithStatus(HttpStatusCode.Accepted)
            }
        }
    }
}

data class ManuellOverstyring(
    @JsonProperty(value = "referanse", required = true) val referanse: UUID,
    @JsonProperty(value = "behandlingVersjon", required = true, defaultValue = "0") val behandlingVersjon: Long,
    val overstyringType: OverstyringType
)

enum class OverstyringType {
    LOVVALG,
    MEDLEMSKAP
}