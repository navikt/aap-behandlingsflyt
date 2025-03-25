package no.nav.aap.behandlingsflyt.hendelse

import com.papsign.ktor.openapigen.route.TagModule
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.*
import no.nav.aap.behandlingsflyt.EMPTY_JSON_RESPONSE
import no.nav.aap.behandlingsflyt.Tags
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepository
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Innsending
import no.nav.aap.behandlingsflyt.prosessering.HendelseMottattHåndteringJobbUtfører
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.flate.FinnEllerOpprettSakDTO
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.flate.SaksinfoDTO
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.tilgang.AuthorizationBodyPathConfig
import no.nav.aap.tilgang.Operasjon
import no.nav.aap.tilgang.authorizedPost
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import javax.sql.DataSource

private val log = LoggerFactory.getLogger("hendelse.MottattHendelseAPI")

fun NormalOpenAPIRoute.mottattHendelseApi(dataSource: DataSource) {
    route("/api/hendelse") {
        route("/send")
        {
            authorizedPost<Unit, String, Innsending>(
                modules = arrayOf(TagModule(listOf(Tags.Sak))),
                routeConfig = AuthorizationBodyPathConfig(
                    operasjon = Operasjon.SAKSBEHANDLE,
                    applicationsOnly = false,
                    applicationRole = "opprett-sak",
                )
            )
            { _, dto ->
                MDC.putCloseable("saksnummer", dto.saksnummer.toString()).use {
                    dataSource.transaction { connection ->
                        val repositoryProvider = RepositoryProvider(connection)
                        val sak = repositoryProvider.provide<SakRepository>().hent(dto.saksnummer)
                        val mottattDokumentRepository = repositoryProvider.provide<MottattDokumentRepository>()

                        log.info("Mottok dokumenthendelse. Brevkategori: ${dto.type} Mottattdato: ${dto.mottattTidspunkt}")

                        if (kjennerTilDokumentFraFør(dto, sak, mottattDokumentRepository)) {
                            log.warn("Allerede håndtert dokument med referanse {}", dto.referanse)
                        } else {
                            val flytJobbRepository = FlytJobbRepository(connection)
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
                respond(EMPTY_JSON_RESPONSE, HttpStatusCode.Accepted)
            }
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