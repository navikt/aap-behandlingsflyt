package no.nav.aap.behandlingsflyt.kontrakt.datadeling

import no.nav.aap.behandlingsflyt.kontrakt.sak.Status
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.RettighetsType
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

public data class DatadelingDTO(
    val beregningsgrunnlag: BigDecimal, //lag ny enkel klasse for kun beregningsgrunnlaget
    val underveisperiode: List<UnderveisDTO>,  //Lag ny DTO
    val rettighetsPeriodeFom: LocalDate,
    val rettighetsPeriodeTom: LocalDate,
    val behandlingStatus: Status,
    val vedtaksDato: LocalDate,
    val sak: SakDTO, // -\\-
    val tilkjent: List<TilkjentDTO>,
)

public data class SakDTO(
    val saksnummer: String,
    val status: Status,
    val fnr: List<String>,
    val opprettetTidspunkt: LocalDateTime = LocalDateTime.now(),
)

public data class UnderveisDTO(
    val underveisFom: LocalDate,
    val underveisTom: LocalDate,
    val meldeperiodeFom: LocalDate,
    val meldeperiodeTom: LocalDate,
    val utfall: String,
    val rettighetsType: RettighetsType?,
    val avslagsårsak: String, // skal ikke denne være Avslagsårsak?
)

public data class TilkjentDTO(
    val tilkjentFom: LocalDate,
    val tilkjentTom: LocalDate,
    val dagsats: Int,
    val gradering: Int,
    val grunnlag: Int,
    val grunnlagsfaktor: Int,
    val grunnbeløp: Int,
    val antallBarn: Int,
    val barnetilleggsats: Int,
    val barnetillegg: Int
)
