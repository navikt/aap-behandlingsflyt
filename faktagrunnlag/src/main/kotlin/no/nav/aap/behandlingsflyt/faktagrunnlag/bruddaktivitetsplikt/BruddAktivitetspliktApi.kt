package no.nav.aap.behandlingsflyt.faktagrunnlag.bruddaktivitetsplikt

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.*
import no.nav.aap.komponenter.dbconnect.transaction
import javax.sql.DataSource

fun NormalOpenAPIRoute.aktivitetspliktApi(dataSource: DataSource) {
    route("/api/aktivitetsplikt") {
        route("/lagre").post<Unit, String, BruddAktivitetspliktRequest> { _, req ->
            dataSource.transaction { connection ->
                val repository = BruddAktivitetspliktRepository(connection)
                repository.lagreBruddAktivitetspliktHendelse(req)
            }
            respond("{}", HttpStatusCode.Accepted)
        }

        route("/{saksnummer}").get<HentHendelseDto, BruddAktivitetspliktResponse> { req ->
            val response = dataSource.transaction { connection ->
                val repository = BruddAktivitetspliktRepository(connection)
                val alleBrudd = repository.hentBruddAktivitetspliktHendelser(req.saksnummer)
                BruddAktivitetspliktResponse(alleBrudd)
            }
            respond(response)
        }

        route("/fjernAlle").post<Unit, String, String> { _, req ->
            dataSource.transaction { connection ->
                val repository = BruddAktivitetspliktRepository(connection)
                repository.cleanup()
            }
            respond("{}", HttpStatusCode.Accepted)
        }
    }
}