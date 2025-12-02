package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.OpenAPIPipelineResponseContext
import com.papsign.ktor.openapigen.route.response.respondWithStatus
import com.papsign.ktor.openapigen.route.route
import com.papsign.ktor.openapigen.route.tag
import io.ktor.http.*
import no.nav.aap.behandlingsflyt.Tags
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.flate.LøsAvklaringsbehovPåBehandling
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.flate.LøsPeriodisertAvklaringsbehovPåBehandling
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.LøsningForPeriode
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.mdc.LogKontekst
import no.nav.aap.behandlingsflyt.mdc.LoggingKontekst
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.behandlingsflyt.sakogbehandling.lås.TaSkriveLåsRepository
import no.nav.aap.behandlingsflyt.tilgang.relevanteIdenterForBehandlingResolver
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.komponenter.server.auth.bruker
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.tilgang.AuthorizationBodyPathConfig
import no.nav.aap.tilgang.Operasjon
import no.nav.aap.tilgang.authorizedPost
import javax.sql.DataSource

fun NormalOpenAPIRoute.avklaringsbehovApi(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry,
    gatewayProvider: GatewayProvider,
) {
    route("/api/behandling").tag(Tags.Behandling) {
        route("/løs-behov") {
            authorizedPost<Unit, Unit, LøsAvklaringsbehovPåBehandling>(
                AuthorizationBodyPathConfig(
                    relevanteIdenterResolver = relevanteIdenterForBehandlingResolver(repositoryRegistry, dataSource),
                    operasjon = Operasjon.SAKSBEHANDLE,
                )
            ) { _, request ->
                løsAvklaringsbehov(
                    dataSource = dataSource,
                    repositoryRegistry = repositoryRegistry,
                    gatewayProvider = gatewayProvider,
                    behandlingReferanse = BehandlingReferanse(request.referanse),
                    hendelse = LøsAvklaringsbehovHendelse(
                        løsning = request.behov,
                        ingenEndringIGruppe = request.ingenEndringIGruppe == true,
                        behandlingVersjon = request.behandlingVersjon,
                        bruker = bruker()
                    )
                )
            }
        }
        route("/løs-periodisert-behov") {
            authorizedPost<Unit, Unit, LøsPeriodisertAvklaringsbehovPåBehandling>(
                AuthorizationBodyPathConfig(
                    relevanteIdenterResolver = relevanteIdenterForBehandlingResolver(repositoryRegistry, dataSource),
                    operasjon = Operasjon.SAKSBEHANDLE,
                )
            ) { _, request ->
                validerPerioder(request.behov.løsningerForPerioder)
                løsAvklaringsbehov(
                    dataSource = dataSource,
                    repositoryRegistry = repositoryRegistry,
                    gatewayProvider = gatewayProvider,
                    behandlingReferanse = BehandlingReferanse(request.referanse),
                    hendelse = LøsAvklaringsbehovHendelse(
                        løsning = request.behov,
                        ingenEndringIGruppe = request.ingenEndringIGruppe == true,
                        behandlingVersjon = request.behandlingVersjon,
                        bruker = bruker()
                    )
                )
            }
        }
    }
}

fun validerPerioder(løsninger: List<LøsningForPeriode>) {
    val ugyldigePerioder = løsninger
        .mapNotNull {
            val tom = it.tom
            when {
                tom == null || it.fom <= tom -> null
                else -> "${it.fom}–$tom"
            }
        }
    if (ugyldigePerioder.isNotEmpty()) {
        throw UgyldigForespørselException("Ny vurderinger med ugyldige perioder: $ugyldigePerioder")
    }

    val overlapp = løsninger
        .sortedBy { it.fom }
        .windowed(2, 1)
        .mapNotNull { (vurdering, nesteVurdering) ->
            val tom = vurdering.tom ?: nesteVurdering.fom.minusDays(1)

            if (tom < vurdering.fom || tom >= nesteVurdering.fom)
                "${vurdering.fom}–${vurdering.tom ?: "…"} og ${nesteVurdering.fom}–${nesteVurdering.tom ?: "…"}"
            else
                null
        }
    if (overlapp.isNotEmpty()) {
        throw UgyldigForespørselException("Vurderinger har overlappende perioder: ${overlapp.joinToString()}")
    }
}

private suspend fun OpenAPIPipelineResponseContext<Unit>.løsAvklaringsbehov(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry,
    gatewayProvider: GatewayProvider,
    behandlingReferanse: BehandlingReferanse,
    hendelse: LøsAvklaringsbehovHendelse
) {
    dataSource.transaction { connection ->
        val repositoryProvider = repositoryRegistry.provider(connection)
        val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
        val taSkriveLåsRepository = repositoryProvider.provide<TaSkriveLåsRepository>()
        val flytJobbRepository = repositoryProvider.provide<FlytJobbRepository>()

        LoggingKontekst(
            repositoryProvider,
            LogKontekst(referanse = behandlingReferanse)
        ).use {
            val lås = taSkriveLåsRepository.lås(behandlingReferanse.referanse)
            BehandlingTilstandValidator(
                BehandlingReferanseService(behandlingRepository),
                flytJobbRepository
            ).validerTilstand(
                behandlingReferanse, hendelse.behandlingVersjon
            )

            AvklaringsbehovHendelseHåndterer(repositoryProvider, gatewayProvider).håndtere(
                key = lås.behandlingSkrivelås.id,
                hendelse = hendelse
            )
            taSkriveLåsRepository.verifiserSkrivelås(lås)
        }
    }
    respondWithStatus(HttpStatusCode.Accepted)
}