package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.samordning

import no.nav.aap.behandlingsflyt.behandling.samordning.Ytelse
import no.nav.aap.komponenter.type.Periode
import java.math.BigDecimal
import java.time.LocalDate

data class VurderingerForSamordning(
    val begrunnelse: String?,
    val maksDatoEndelig: Boolean?,
    val fristNyRevurdering: LocalDate?,
    val vurderteSamordningerData: List<SamordningVurderingData>
) {
    init {
        vurderteSamordningerData.groupBy { it.ytelseType }.forEach { (_, samordninger) ->
            // VERIFISER INGEN OVERLAPP
            val sortedPerioder = samordninger.map { it.periode }.sortedBy { it.fom }

            for (i in 0 until sortedPerioder.size - 1) {
                val current = sortedPerioder[i]
                val next = sortedPerioder[i + 1]
                require(!current.overlapper(next)) { "Perioder kan ikke overlappe for samme ytelsetype: $current og $next" }
            }
        }
    }
}

data class SamordningVurderingData(
    val ytelseType: Ytelse,
    val periode: Periode,
    val gradering: Int?,
    val kronesum: BigDecimal? = null,
    val manuell: Boolean? = null
)
