package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.samordning

import no.nav.aap.behandlingsflyt.behandling.samordning.Ytelse
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.komponenter.type.Periode
import java.math.BigDecimal
import java.time.LocalDate

data class VurderingerForSamordning(
    val begrunnelse: String?,
    @Deprecated("Ikke lenger i bruk")
    val maksDatoEndelig: Boolean? = null,
    @Deprecated("Ikke lenger i bruk")
    val fristNyRevurdering: LocalDate? = null,
    val vurderteSamordningerData: List<SamordningVurderingData>
) {
    fun valider() {
        vurderteSamordningerData.groupBy { it.ytelseType }.forEach { (_, samordninger) ->
            // VERIFISER INGEN OVERLAPP
            samordninger
                .map { it.periode }
                .sortedBy { it.fom }
                .windowed(2)
                .forEach { (current, next) ->
                    if (current.overlapper(next)) {
                        throw UgyldigForespørselException("Perioder kan ikke overlappe for samme ytelsetype: $current og $next")
                    }
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
