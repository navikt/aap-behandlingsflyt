package no.nav.aap.motor.api

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.dbconnect.transaction
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.mdc.JobbLogInfoProviderHolder
import no.nav.aap.motor.retry.DriftJobbRepositoryExposed
import javax.sql.DataSource

fun NormalOpenAPIRoute.motorApi(dataSource: DataSource) {
    route("/drift/api/jobb") {
        route("/feilende") {
            get<Unit, List<JobbInfoDto>> { _ ->
                val saker: List<JobbInfoDto> = dataSource.transaction(readOnly = true) { connection ->
                    DriftJobbRepositoryExposed(connection).hentAlleFeilende()
                        .map { (jobbInput, jobbStatus) ->
                            jobbInfoDto(jobbInput, jobbStatus, connection)
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
                            planlagtKjøretidspunkt = info.nesteKjøring(),
                            metadata = mapOf(),
                            antallFeilendeForsøk = 0,
                            beskrivelse = info.beskrivelse(),
                            navn = info.navn()
                        )
                    }
                }
                respond(saker)
            }
        }
        route("/rekjor/{jobbId}") {
            get<JobbIdDTO, String> { jobbId ->
                val antallSchedulert = dataSource.transaction { connection ->
                    DriftJobbRepositoryExposed(connection).markerFeilendeForKlar(jobbId.jobbId)
                }
                respond("Rekjøring av feilende jobb med ID $jobbId startet, startet $antallSchedulert jobber.")
            }
        }
        route("/rekjorAlleFeilede") {
            get<Unit, String> {
                val antallSchedulert = dataSource.transaction { connection ->
                    DriftJobbRepositoryExposed(connection).markerAlleFeiledeForKlare()
                }
                respond("Rekjøring av feilede startet, startet $antallSchedulert jobber.")
            }
        }
        route("/sisteKjørte") {
            get<Unit, List<JobbInfoDto>> { _ ->
                val saker: List<JobbInfoDto> = dataSource.transaction(readOnly = true) { connection ->
                    DriftJobbRepositoryExposed(connection).hentSisteJobber(150)
                        .map { (jobbInput, jobbStatus) ->
                            jobbInfoDto(jobbInput, jobbStatus, connection)
                        }

                }
                respond(saker)
            }
        }
    }
}

private fun jobbInfoDto(
    jobbInput: JobbInput,
    jobbStatus: String?,
    connection: DBConnection
): JobbInfoDto {
    return JobbInfoDto(
        id = jobbInput.jobbId(),
        type = jobbInput.type(),
        navn = jobbInput.navn(),
        beskrivelse = jobbInput.beskrivelse(),
        status = jobbInput.status(),
        antallFeilendeForsøk = jobbInput.antallRetriesForsøkt(),
        feilmelding = jobbStatus,
        planlagtKjøretidspunkt = jobbInput.nesteKjøring(),
        metadata = JobbLogInfoProviderHolder.get()
            .hentInformasjon(connection, jobbInput)?.felterMedVerdi
            ?: mapOf()
    )
}

