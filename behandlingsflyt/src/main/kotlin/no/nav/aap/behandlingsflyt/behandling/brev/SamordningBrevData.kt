package no.nav.aap.behandlingsflyt.behandling.brev

import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Beløp
import java.time.LocalDate
import java.time.YearMonth

data class SamordningBrevData(
    val gradering: GraderingBrevData?,
    val uføre: UføreBrevData?,
    val arbeidsgiver: ArbeidsgiverBrevData?,
    val tjenestepensjonRefusjonskrav: TjenestepensjonRefusjonskravBrevData?,
    val sykestipend: SykestipendBrevData?,
    val barnepensjon: BarnepensjonBrevData?,
    val andreStatligeYtelser: AndreStatligeYtelserBrevData?,
)

data class GraderingBrevData(
    val vurderinger: List<GraderingVurderingBrev>,
) {
    data class GraderingVurderingBrev(
        val ytelseType: String,
        val perioder: List<GraderingPeriodeBrev>,
    )

    data class GraderingPeriodeBrev(
        val periode: Periode,
        val gradering: Int?,
    )
}

data class UføreBrevData(
    val perioder: List<UførePeriodeBrev>,
) {
    data class UførePeriodeBrev(
        val virkningstidspunkt: LocalDate,
        val uføregradTilSamordning: Int,
    )
}

data class ArbeidsgiverBrevData(
    val perioder: List<Periode>,
)

data class TjenestepensjonRefusjonskravBrevData(
    val harKrav: Boolean,
    val fom: LocalDate?,
    val tom: LocalDate?,
)

data class SykestipendBrevData(
    val perioder: List<Periode>,
)

data class BarnepensjonBrevData(
    val perioder: List<BarnepensjonPeriodeBrev>,
) {
    data class BarnepensjonPeriodeBrev(
        val fom: YearMonth,
        val tom: YearMonth?,
        val månedsats: Beløp,
    )
}

data class AndreStatligeYtelserBrevData(
    val perioder: List<AndreStatligeYtelserPeriodeBrev>,
) {
    data class AndreStatligeYtelserPeriodeBrev(
        val ytelse: String,
        val periode: Periode,
    )
}
