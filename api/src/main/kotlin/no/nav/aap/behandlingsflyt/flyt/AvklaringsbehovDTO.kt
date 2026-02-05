package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehov
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Status
import no.nav.aap.komponenter.tidslinje.somTidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid
import java.time.LocalDate
import java.time.LocalDateTime

data class AvklaringsbehovDTO(
    val definisjon: Definisjon,
    val status: Status,
    val endringer: List<EndringDTO>,
    /* Periodene er sortert og har ikke overlapp. */
    val perioder: List<AvklaringsbehovPeriodeDTO>?
) {
    constructor(avklaringsbehov: Avklaringsbehov, kravdato: LocalDate) : this(
        definisjon = avklaringsbehov.definisjon,
        status = avklaringsbehov.status(),
        endringer = avklaringsbehov.historikk.map { endring ->
            EndringDTO(
                status = endring.status,
                tidsstempel = endring.tidsstempel,
                begrunnelse = endring.begrunnelse,
                endretAv = endring.endretAv
            )
        },
        perioder =
            avklaringsbehov.perioderVedtaketBehøverVurdering
                ?.somTidslinje({ it }, { AvklaringsbehovPeriodeDTO(it, RelevansDTO.RELEVANT) })
                ?.let { relevantTidslinje ->
                    val rettighetsperiode = Periode(kravdato, Tid.MAKS)
                    relevantTidslinje.mergePrioriterHøyre(
                        relevantTidslinje.komplement(rettighetsperiode) { Unit }
                            .map { periode, _ -> AvklaringsbehovPeriodeDTO(periode, RelevansDTO.IKKE_RELEVANT)}
                    )
                        .komprimer()
                        .segmenter()
                        .map { it.verdi }
            }
    )
}

data class AvklaringsbehovPeriodeDTO(
    val periode: Periode,
    val relevans: RelevansDTO,
)

enum class RelevansDTO {
    RELEVANT,
    IKKE_RELEVANT,
}

data class EndringDTO(
    val status: Status,
    val tidsstempel: LocalDateTime = LocalDateTime.now(),
    val begrunnelse: String?,
    val endretAv: String
)
