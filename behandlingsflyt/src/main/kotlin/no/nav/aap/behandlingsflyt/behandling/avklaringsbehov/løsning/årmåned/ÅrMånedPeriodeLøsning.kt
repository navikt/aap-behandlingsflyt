package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.årmåned

import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.komponenter.verdityper.Tid
import java.time.YearMonth

interface ÅrMånedPeriodeLøsning {
    val fom: String
    val tom: String?
}

fun validerPerioder(perioder: List<ÅrMånedPeriodeLøsning>) {
    val ugyldigePerioder = perioder
        .mapNotNull {
            val tom = it.tom?.let{ YearMonth.parse(it) }
            when {
                tom == null || YearMonth.parse(it.fom) <= tom -> null
                else -> "${it.fom}–$tom"
            }
        }
    if (ugyldigePerioder.isNotEmpty()) {
        throw UgyldigForespørselException("Ny vurderinger med ugyldige perioder: $ugyldigePerioder")
    }

    val overlapp = perioder
        .sortedBy { it.fom }
        .windowed(2, 1)
        .mapNotNull { (vurdering, nesteVurdering) ->
            val tom = vurdering.tom?.let { YearMonth.parse(it) } ?: YearMonth.from(Tid.MAKS)

            if (tom < YearMonth.parse(vurdering.fom) || tom >= YearMonth.parse(nesteVurdering.fom))
                "${vurdering.fom}–${vurdering.tom ?: "…"} og ${nesteVurdering.fom}–${nesteVurdering.tom ?: "…"}"
            else
                null
        }
    if (overlapp.isNotEmpty()) {
        throw UgyldigForespørselException("Vurderinger har overlappende perioder: ${overlapp.joinToString()}")
    }
}