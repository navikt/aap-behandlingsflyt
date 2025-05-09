package no.nav.aap.behandlingsflyt.hendelse

import com.papsign.ktor.openapigen.route.TagModule
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.*
import no.nav.aap.behandlingsflyt.Azp
import no.nav.aap.behandlingsflyt.EMPTY_JSON_RESPONSE
import no.nav.aap.behandlingsflyt.Tags
import no.nav.aap.behandlingsflyt.behandling.bruddaktivitetsplikt.SaksnummerParameter
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepository
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Innsending
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.ManuellRevurdering
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.ManuellRevurderingV0
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.ÅrsakTilBehandling
import no.nav.aap.behandlingsflyt.prosessering.HendelseMottattHåndteringJobbUtfører
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.httpklient.auth.Bruker
import no.nav.aap.komponenter.httpklient.auth.bruker
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.tilgang.AuthorizationMachineToMachineConfig
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.SakPathParam
import no.nav.aap.tilgang.authorizedPost
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import javax.sql.DataSource

private val log = LoggerFactory.getLogger("hendelse.MottattHendelseAPI")

fun NormalOpenAPIRoute.mottattHendelseApi(dataSource: DataSource, repositoryRegistry: RepositoryRegistry) {
    route("/api/hendelse") {
        route("/send") {
            authorizedPost<Unit, String, Innsending>(
                modules = arrayOf(TagModule(listOf(Tags.Sak))),
                routeConfig = AuthorizationMachineToMachineConfig(
                    authorizedAzps = listOf(Azp.Postmottak.uuid, Azp.Dokumentinnhenting.uuid)
                )
            ) { _, dto ->
                registrerMottattHendelse(dto, dataSource, repositoryRegistry)
                respond(EMPTY_JSON_RESPONSE, HttpStatusCode.Accepted)
            }
        }

        route("/sak/{saksnummer}/send") {
            // TODO: Fikse bug i tilgang for å sikre endepunktet
            authorizedPost<SaksnummerParameter, String, Innsending>(
                modules = arrayOf(TagModule(listOf(Tags.Sak))),
                routeConfig = AuthorizationParamPathConfig(
                    sakPathParam = SakPathParam("saksnummer"),
                )
            ) { _, dto ->
                validerHendelse(dto, bruker())
                registrerMottattHendelse(dto, dataSource, repositoryRegistry)
                respond(EMPTY_JSON_RESPONSE, HttpStatusCode.Accepted)
            }
        }
    }
}

private fun registrerMottattHendelse(
    dto: Innsending,
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry,
) {
    MDC.putCloseable("saksnummer", dto.saksnummer.toString()).use {
        dataSource.transaction { connection ->
            val repositoryProvider = repositoryRegistry.provider(connection)

            val sak = repositoryProvider.provide<SakRepository>().hent(dto.saksnummer)
            val mottattDokumentRepository = repositoryProvider.provide<MottattDokumentRepository>()
            val flytJobbRepository = repositoryProvider.provide<FlytJobbRepository>()

            log.info("Mottok dokumenthendelse. Brevkategori: ${dto.type} Mottattdato: ${dto.mottattTidspunkt}")

            if (kjennerTilDokumentFraFør(dto, sak, mottattDokumentRepository)) {
                log.warn("Allerede håndtert dokument med referanse {}", dto.referanse)
            } else {
                flytJobbRepository.leggTil(
                    HendelseMottattHåndteringJobbUtfører.nyJobb(
                        sakId = sak.id,
                        dokumentReferanse = dto.referanse,
                        brevkategori = dto.type,
                        kanal = dto.kanal,
                        melding = dto.melding,
                        mottattTidspunkt = dto.mottattTidspunkt
                    ),
                )
            }
        }
    }
}

fun validerHendelse(innsending: Innsending, bruker: Bruker) {
    if (innsending.type == InnsendingType.MANUELL_REVURDERING && innsending.melding is ManuellRevurdering) {
        val melding = innsending.melding as ManuellRevurderingV0
        val gjelderOverstyringAvStarttidspunkt =
            melding.årsakerTilBehandling.contains(ÅrsakTilBehandling.VURDER_RETTIGHETSPERIODE)
        val unleashGateway = GatewayProvider.provide<UnleashGateway>()
        if (gjelderOverstyringAvStarttidspunkt && !unleashGateway.isEnabled(
                BehandlingsflytFeature.OverstyrStarttidspunkt,
                bruker.ident
            )
        ) {
            throw UgyldigForespørselException("Funksjonsbryter for overstyr starttidspunkt er skrudd av")
        }
    }
}

private fun kjennerTilDokumentFraFør(
    innsending: Innsending,
    sak: Sak,
    mottattDokumentRepository: MottattDokumentRepository
): Boolean {
    val innsendinger = mottattDokumentRepository.hentDokumenterAvType(sak.id, innsending.type)

    return innsendinger.any { dokument -> dokument.referanse == innsending.referanse }
}