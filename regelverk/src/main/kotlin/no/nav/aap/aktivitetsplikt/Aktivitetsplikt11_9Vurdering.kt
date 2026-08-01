package no.nav.aap.aktivitetsplikt

import java.time.Instant
import java.time.LocalDate
import no.nav.aap.behandling.BehandlingId
import no.nav.aap.komponenter.verdityper.Bruker

data class Aktivitetsplikt11_9Vurdering(
    val dato: LocalDate,
    val begrunnelse: String,
    val grunn: Grunn,
    val brudd: Brudd,
    val vurdertAv: Bruker,
    val opprettet: Instant,
    val vurdertIBehandling: BehandlingId,
)

enum class Grunn {
    IKKE_RIMELIG_GRUNN, RIMELIG_GRUNN
}

enum class Brudd {
    IKKE_MØTT_TIL_TILTAK,
    IKKE_MØTT_TIL_BEHANDLING,
    IKKE_MØTT_TIL_MØTE,
    IKKE_SENDT_DOKUMENTASJON,
}