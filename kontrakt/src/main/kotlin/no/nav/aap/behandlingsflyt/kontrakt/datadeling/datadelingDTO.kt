package no.nav.aap.behandlingsflyt.kontrakt.datadeling

import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.kontrakt.sak.Status
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * @param beregningsgrunnlag Hvilket beløp ble brukt for å utlede dagsats før redusering. Det er G-justert mhp rettighetsperiode.fom.
 */
public data class DatadelingDTO(
    val underveisperiode: List<UnderveisDTO>,
    val rettighetsPeriodeFom: LocalDate,
    val rettighetsPeriodeTom: LocalDate,
    val behandlingStatus: no.nav.aap.behandlingsflyt.kontrakt.behandling.Status,
    val behandlingsId: String,
    val vedtaksDato: LocalDate,
    val sak: SakDTO,
    val tilkjent: List<TilkjentDTO>,
    val rettighetsTypeTidsLinje: List<RettighetsTypePeriode>,
    val behandlingsReferanse: String,
    val samId: String? = null,
    val vedtakId: Long,
    val beregningsgrunnlag: BigDecimal
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

public data class MeldtArbeidIPeriodeDTO(
    val periodeFom: LocalDate,
    val periodeTom: LocalDate,
    val timerArbeidet: BigDecimal,
    val mottattTidspunkt: LocalDateTime,
    val journalpostId: String,
)

public data class DetaljertTimeArbeidetListeDTO(
    val personIdent: String,
    val saksnummer: Saksnummer,
    val behandlingId: Long,
    val meldeperiodeFom: LocalDate,
    val meldeperiodeTom: LocalDate,
    val detaljertArbeidIPeriode: List<MeldtArbeidIPeriodeDTO>,
    val meldepliktStatusKode: String?,
    val rettighetsTypeKode: String?,
    val avslagsårsakKode: String?
)