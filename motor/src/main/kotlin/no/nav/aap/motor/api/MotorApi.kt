package no.nav.aap.motor.api

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.dbconnect.transaction
import no.nav.aap.motor.mdc.JobbLogInfoProviderHolder
import no.nav.aap.motor.retry.DriftJobbRepositoryExposed
import javax.sql.DataSource

fun NormalOpenAPIRoute.motorApi(dataSource: DataSource) {
    route("/drift/api/jobb") {
        route("/feilende") {
            get<Unit, List<JobbInfoDto>> { _ ->
                val saker: List<JobbInfoDto> = dataSource.transaction(readOnly = true) { connection ->
                    DriftJobbRepositoryExposed(connection).hentAlleFeilende()
                        .map { pair ->
                            val info = pair.first
                            JobbInfoDto(
                                id = info.jobbId(),
                                type = info.type(),
                                status = info.status(),
                                antallFeilendeForsøk = info.antallRetriesForsøkt(),
                                feilmelding = pair.second,
                                metadata = JobbLogInfoProviderHolder.get().hentInformasjon(connection, info)?.felterMedVerdi
                                    ?: mapOf()
                            )
                        }

                }
                respond(saker)
            }
        }
        route("/planlagte-jobber") {
            get<Unit, List<JobbInfoDto>> { _ ->
                val saker: List<JobbInfoDto> = dataSource.transaction(readOnly = true) { connection ->
                    DriftJobbRepositoryExposed(connection).hentInfoOmGjentagendeJobber().map { info ->
                        JobbInfoDto(
                            id = info.jobbId(),
                            type = info.type(),
                            status = info.status(),
                            antallFeilendeForsøk = info.antallRetriesForsøkt(),
                            metadata = mapOf()
                        )
                    }
                }
                respond(saker)
            }
        }
        route("/rekjor/{jobbId}") {
            get<JobbIdDTO, String> { jobbId ->
                val antallSchedulert = dataSource.transaction { connection ->
                    DriftJobbRepositoryExposed(connection).markerFeilendeForKlar(jobbId.id)
                }
                respond("Rekjøring av feilede startet, startet " + antallSchedulert + " jobber.")
            }
        }
        route("/rekjorAlleFeilede") {
            get<Unit, String> {
                val antallSchedulert = dataSource.transaction { connection ->
                    DriftJobbRepositoryExposed(connection).markerAlleFeiledeForKlare()
                }
                respond("Rekjøring av feilede startet, startet " + antallSchedulert + " jobber.")
            }
        }
    }
}

