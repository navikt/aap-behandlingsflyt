package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.samordning

import no.nav.aap.behandlingsflyt.behandling.samordning.Ytelse
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Prosent
import java.math.BigDecimal
import java.time.LocalDate

data class VurderingerForSamordning(
    val begrunnelse: String,
    val maksDatoEndelig: Boolean?,
    val maksDato: LocalDate?,
    val vurderteSamordningerData: List<SamordningVurderingData>
)

data class SamordningVurderingData(
    val ytelseType: Ytelse,
    val periode: Periode,
    val gradering: Int?,
    val kronesum: BigDecimal? = null,
    val manuell: Boolean? = null
)

