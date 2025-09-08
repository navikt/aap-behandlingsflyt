package no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt

import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import java.time.Instant
import java.time.LocalDate

data class Aktivitetsplikt11_7Vurdering(
    val begrunnelse: String,
    val erOppfylt: Boolean,
    val utfall: Utfall? = null,
    val vurdertAv: String,
    val gjelderFra: LocalDate,
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