package no.nav.aap.behandlingsflyt.kontrakt.datadeling

import no.nav.aap.behandlingsflyt.kontrakt.sak.Status
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

public data class DatadelingDTO(
    val underveisperiode: List<UnderveisDTO>,  //Lag ny DTO
    val rettighetsPeriodeFom: LocalDate,
    val rettighetsPeriodeTom: LocalDate,
    val behandlingStatus: no.nav.aap.behandlingsflyt.kontrakt.behandling.Status,
    val behandlingsId: String,
    val vedtaksDato: LocalDate,
    val sak: SakDTO, // -\\-
    val tilkjent: List<TilkjentDTO>,
    val rettighetsTypeTidsLinje: List<RettighetsTypePeriode>,
    val behandlingsReferanse: String,
    val samId: String? = null,
    val vedtakId: Long
)

public data class RettighetsTypePeriode(
    val fom: LocalDate,
    val tom: LocalDate,
    val verdi: String
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
    val rettighetsType: String?,
    val avslagsårsak: String?, // skal ikke denne være Avslagsårsak?
)

public data class TilkjentDTO(
    val tilkjentFom: LocalDate,
    val tilkjentTom: LocalDate,
    val dagsats: Int,
    val gradering: Int,
    val samordningUføregradering: Int? = null,
    @Deprecated("Denne er alltid lik dagsats fra behandlingsflyt.")
    val grunnlag: BigDecimal,
    val grunnlagsfaktor: BigDecimal,
    val grunnbeløp: BigDecimal,
    val antallBarn: Int,
    val barnetilleggsats: BigDecimal,
    val barnetillegg: BigDecimal
)
