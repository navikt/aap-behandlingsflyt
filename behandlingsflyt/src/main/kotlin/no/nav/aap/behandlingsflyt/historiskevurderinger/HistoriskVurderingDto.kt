package no.nav.aap.behandlingsflyt.historiskevurderinger

import java.time.LocalDate

abstract class HistoriskVurderingDto<T>(
    val vurderDato: LocalDate,
    val vurdertAvIdent: String,
    val erGjeldendeVurdering: Boolean,
    val periode: ÅpenPeriodeDto,
    val vurdering: T
)

data class ÅpenPeriodeDto(val fom: LocalDate, val tom: LocalDate? = null) {
    init {
        if (tom != null && fom.isAfter(tom)) {
            throw IllegalArgumentException("tom(${tom}) er før fom(${fom})")
        }
    }
}
