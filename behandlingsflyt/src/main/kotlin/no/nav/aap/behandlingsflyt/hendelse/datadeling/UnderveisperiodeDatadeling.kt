package no.nav.aap.behandlingsflyt.hendelse.datadeling

import no.nav.aap.komponenter.type.Periode
import java.math.BigDecimal

data class UnderveisperiodeDatadeling(
    val periode: Periode,
    val meldeperiode: Periode,
    val meldepliktstatus: String?,
    val arbeidsgrad: Int,
    val overgrenseVerdi: Boolean,
    val timerArbeidet: BigDecimal,
)
