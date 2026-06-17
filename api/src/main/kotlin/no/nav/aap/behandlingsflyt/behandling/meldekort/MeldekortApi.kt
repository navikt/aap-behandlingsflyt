package no.nav.aap.behandlingsflyt.behandling.meldekort

import com.papsign.ktor.openapigen.route.TagModule
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.Tags
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.ArbeidIPeriodeV0
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.MeldekortV0
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.flate.SaksnummerParameter
import no.nav.aap.behandlingsflyt.tilgang.relevanteIdenterForSakResolver
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.komponenter.server.auth.bruker
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.Operasjon
import no.nav.aap.tilgang.Rolle
import no.nav.aap.tilgang.SakPathParam
import no.nav.aap.tilgang.authorizedGet
import no.nav.aap.tilgang.authorizedPost
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import javax.sql.DataSource

fun NormalOpenAPIRoute.meldekortApi(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry,
    gatewayProvider: GatewayProvider,
    clock: Clock = Clock.systemDefaultZone()
) {
    route("/api/meldekort/{saksnummer}") {
        authorizedGet<SaksnummerParameter, MeldeperioderMedMeldekortResponse>(
            AuthorizationParamPathConfig(
                relevanteIdenterResolver = relevanteIdenterForSakResolver(repositoryRegistry, dataSource),
                sakPathParam = SakPathParam("saksnummer")
            ),
            null,
            modules = arrayOf(TagModule(listOf(Tags.Sak))),
        ) { req ->
            val response = dataSource.transaction(readOnly = true) { connection ->
                val meldekortService =
                    MeldekortService(repositoryRegistry.provider(connection), gatewayProvider, clock)
                meldekortService.hentMeldeperioderMedMeldekort(Saksnummer(req.saksnummer))
            }

            respond(response)
        }

        authorizedPost<SaksnummerParameter, OppdaterMeldekortResponse, OppdaterMeldekortRequest>(
            AuthorizationParamPathConfig(
                relevanteIdenterResolver = relevanteIdenterForSakResolver(repositoryRegistry, dataSource),
                sakPathParam = SakPathParam("saksnummer"),
                operasjon = Operasjon.SAKSBEHANDLE,
                påkrevdRolle = listOf(Rolle.SAKSBEHANDLER_NASJONAL, Rolle.SAKSBEHANDLER_OPPFOLGING),
            ),
            modules = arrayOf(TagModule(listOf(Tags.Sak))),
        ) { req, body ->
            val response = dataSource.transaction { connection ->
                val meldekortService =
                    MeldekortService(repositoryRegistry.provider(connection), gatewayProvider, clock)
                val bruker = bruker()
                meldekortService.oppdaterMeldekort(
                    saksnummer = Saksnummer(req.saksnummer),
                    meldeperiode = body.meldeperiode,
                    meldedato = body.meldeDato,
                    meldekort = body.tilMeldekort(bruker),
                    bruker = bruker,
                ).tilResponse()
            }

            respond(response)
        }

        route("prosessering") {
            authorizedGet<SaksnummerParameter, MeldekortProsesseringResponse>(
                AuthorizationParamPathConfig(
                    relevanteIdenterResolver = relevanteIdenterForSakResolver(repositoryRegistry, dataSource),
                    sakPathParam = SakPathParam("saksnummer")
                ),
                null,
                modules = arrayOf(TagModule(listOf(Tags.Sak))),
            ) { req ->
                val response = dataSource.transaction(readOnly = true) { connection ->
                    val meldekortService =
                        MeldekortService(repositoryRegistry.provider(connection), gatewayProvider, clock)
                    meldekortService.hentProsesseringStatus(Saksnummer(req.saksnummer))
                }

                respond(response)
            }
        }

    }
}

private fun OppdatertMeldekort.tilResponse(): OppdaterMeldekortResponse =
    OppdaterMeldekortResponse(
        journalpostId = journalpostId.identifikator,
        oppdatertTidspunkt = LocalDate.ofInstant(tidspunkt, ZoneId.of("Europe/Oslo")),
    )

data class OppdaterMeldekortRequest(
    val meldeperiode: Periode,
    val meldeDato: LocalDate,
    val begrunnelse: String,
    val dager: Set<DagDto>,
) {
    fun tilMeldekort(vurdertAv: Bruker): MeldekortV0 =
        MeldekortV0(
            harDuArbeidet = dager
                .takeIf { it.isNotEmpty() }?.let { it.sumOf { dag -> dag.timerArbeidet } > 0.0 },
            opprettetAv = vurdertAv.ident,
            begrunnelse = begrunnelse,
            timerArbeidPerPeriode = dager.map {
                ArbeidIPeriodeV0(
                    fraOgMedDato = it.dato,
                    tilOgMedDato = it.dato,
                    timerArbeid = it.timerArbeidet,
                )
            }
        )
}