package no.nav.aap.aktivitetsplikt

import java.time.Instant
import java.time.LocalDate
import no.nav.aap.brev.BrevbestillingReferanse
import no.nav.aap.behandling.BehandlingId
import no.nav.aap.komponenter.verdityper.Bruker

data class Aktivitetsplikt11_7Vurdering(
    val begrunnelse: String,
    val erOppfylt: Boolean,
    val utfall: Utfall? = null,
    val vurdertAv: Bruker,
    val fom: LocalDate,
    val opprettet: Instant,
    val vurdertIBehandling: BehandlingId,
    val skalIgnorereVarselFrist: Boolean
)

enum class Utfall {
    STANS, OPPHØR
}

data class Aktivitetsplikt11_7Varsel(
    val varselId: BrevbestillingReferanse,
    val sendtDato: LocalDate? = null,
    val svarfrist: LocalDate? = null,
)