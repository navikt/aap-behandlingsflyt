package no.nav.aap.behandlingsflyt

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.aordning.InntektkomponentenGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.aordning.InntektskomponentResponse
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.YearMonth
import javax.sql.DataSource

fun NormalOpenAPIRoute.testAPI(dataSource: DataSource) {
    route("/api/test/inntekt") {
        post<Unit, InntektskomponentResponse, String> { _, req ->
            val inntektRespons = dataSource.transaction(readOnly = true) { connection ->
                val sakRepository = RepositoryProvider(connection).provide(SakRepository::class)

                val sak = sakRepository.hent((Saksnummer(req)))

                val inntektskomponentGateway = InntektkomponentenGateway()

                val response = inntektskomponentGateway.hentAInntekt(
                    sak.person.aktivIdent().identifikator,
                    YearMonth.from(sak.rettighetsperiode.fom),
                    YearMonth.from(sak.rettighetsperiode.fom)
                )
                response
            }
            respond(inntektRespons)
        }
    }
}