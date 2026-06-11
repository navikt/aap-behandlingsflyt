package no.nav.aap.behandlingsflyt.kontrakt.datadeling

import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * @param beregningsgrunnlag Hvilket beløp ble brukt for å utlede dagsats før redusering. Det er G-justert mhp rettighetsperiode.fom.
 */
public data class DatadelingDTO(
    val rettighetsPeriodeFom: LocalDate,
    val rettighetsPeriodeTom: LocalDate,
    val behandlingStatus: no.nav.aap.behandlingsflyt.kontrakt.behandling.Status,
    val behandlingsId: String,
    val vedtaksDato: LocalDate,
    val sak: SakDTO,
    val tilkjent: List<TilkjentDTO>,
    val rettighetsTypeTidsLinje: List<RettighetsTypePeriode>,
    val muligMaksdato: LocalDate?,
    val behandlingsReferanse: String,
    val samId: String? = null,
    val vedtakId: Long,
    val beregningsgrunnlag: BigDecimal?,
    val stansOpphørVurdering: Set<GjeldendeStansEllerOpphørDTO>?,
    val arenavedtak: List<ArenavedtakDTO>,
    val søknadsdatoer: List<LocalDateTime>,
)

public data class ArenavedtakDTO(
    public val vedtakId: Long,
    public val fom: LocalDate,
    public val tom: LocalDate,
    public val  vedtaksvariant: ArenaVedtaksvariantDTO,
)

public enum class ArenaVedtaksvariantDTO {
    O_AVSLAG,
    O_INNV_NAV,
    O_INNV_SOKNAD,
    E_FORLENGE,
    E_VERDI,
    G_AVSLAG,
    G_INNV_NAV,
    G_INNV_SOKNAD,
    S_DOD,
    S_OPPHOR,
    S_STANS,
    ;
}


public data class GjeldendeStansEllerOpphørDTO(
    val fom: LocalDate,
    val opprettet: Instant,
    val vurdering: StansEllerOpphørEnumDTO,
    val avslagsårsaker: Set<AvslagsårsakDTO>,
)

public enum class StansEllerOpphørEnumDTO {
    STANS,
    OPPHØR
}

public enum class AvslagsårsakDTO(
    public val type: StansEllerOpphørEnumDTO,
) {
    BRUKER_OVER_67(StansEllerOpphørEnumDTO.OPPHØR),
    IKKE_RETT_PA_SYKEPENGEERSTATNING(StansEllerOpphørEnumDTO.OPPHØR),
    IKKE_RETT_PA_STUDENT(StansEllerOpphørEnumDTO.OPPHØR),
    VARIGHET_OVERSKREDET_STUDENT(StansEllerOpphørEnumDTO.OPPHØR),
    IKKE_SYKDOM_AV_VISS_VARIGHET(StansEllerOpphørEnumDTO.OPPHØR),
    IKKE_SYKDOM_SKADE_LYTE_VESENTLIGDEL(StansEllerOpphørEnumDTO.OPPHØR),
    IKKE_NOK_REDUSERT_ARBEIDSEVNE(StansEllerOpphørEnumDTO.OPPHØR),
    IKKE_BEHOV_FOR_OPPFOLGING(StansEllerOpphørEnumDTO.OPPHØR),
    IKKE_MEDLEM(StansEllerOpphørEnumDTO.OPPHØR),
    IKKE_OPPFYLT_OPPHOLDSKRAV_EØS(StansEllerOpphørEnumDTO.STANS),
    ANNEN_FULL_YTELSE(StansEllerOpphørEnumDTO.OPPHØR),
    INNTEKTSTAP_DEKKES_ETTER_ANNEN_LOVGIVNING(StansEllerOpphørEnumDTO.OPPHØR),
    IKKE_RETT_PA_AAP_UNDER_BEHANDLING_AV_UFORE(StansEllerOpphørEnumDTO.OPPHØR),
    VARIGHET_OVERSKREDET_OVERGANG_UFORE(StansEllerOpphørEnumDTO.OPPHØR),
    VARIGHET_OVERSKREDET_ARBEIDSSØKER(StansEllerOpphørEnumDTO.OPPHØR),
    IKKE_RETT_PA_AAP_I_PERIODE_SOM_ARBEIDSSOKER(StansEllerOpphørEnumDTO.STANS),
    IKKE_RETT_UNDER_STRAFFEGJENNOMFØRING(StansEllerOpphørEnumDTO.STANS),
    BRUDD_PÅ_AKTIVITETSPLIKT_STANS(StansEllerOpphørEnumDTO.STANS),
    BRUDD_PÅ_AKTIVITETSPLIKT_OPPHØR(StansEllerOpphørEnumDTO.OPPHØR),
    BRUDD_PÅ_OPPHOLDSKRAV_STANS(StansEllerOpphørEnumDTO.STANS),
    BRUDD_PÅ_OPPHOLDSKRAV_OPPHØR(StansEllerOpphørEnumDTO.OPPHØR),
    ORDINÆRKVOTE_BRUKT_OPP(StansEllerOpphørEnumDTO.OPPHØR),
    SYKEPENGEERSTATNINGKVOTE_BRUKT_OPP(StansEllerOpphørEnumDTO.OPPHØR),
    IKKE_SYKDOM_SKADE_LYTE(StansEllerOpphørEnumDTO.OPPHØR),
}


public data class RettighetsTypePeriode(
    val fom: LocalDate,
    val tom: LocalDate,
    val verdi: String
)

public data class SakDTO(
    val saksnummer: String,
    val fnr: List<String>,
    val opprettetTidspunkt: LocalDateTime = LocalDateTime.now(),
)

public data class TilkjentDTO(
    val tilkjentFom: LocalDate,
    val tilkjentTom: LocalDate,
    val dagsats: Int,
    val gradering: Int,
    val samordningUføregradering: Int? = null,
    val grunnlagsfaktor: BigDecimal,
    val grunnbeløp: BigDecimal,

    /** Antall barn som gir rett til barnetillegg. */
    val antallBarn: Int,

    /** Størrelsen på ugradert barnetilleggsats.
     *
     * Verdien er ugradert, i den forstand at:
     * Hvis barnetilleggsatsen er spesifisert i AAP-forskriften § 8 til 38 kroner, og medlemmet får 50% AAP,
     * så vil [barnetilleggsats] være 38.
     **/
    val barnetilleggsats: BigDecimal,


    /** Størrelsen på total, ugradert barnetillegg.
     *
     * Verdien er total i den forstand at den tar hensyn til antall barn.
     *
     * Den er ugradert i den forstand at hvis medlemmet har 2 barn, får 50 % AAP
     * på grunn av samordning, og barnetilleggssatsen er spesifisert i AAP-forskriften § 8 til 38 kroner,
     * så vil [barnetillegg] være 2 * 38 = 76. Altså vi har ikke redusert barnetillegget med 50% her.
     *
     * Spesifikasjon: [barnetillegg] = [barnetilleggsats] * [antallBarn].
     */
    val barnetillegg: BigDecimal
)


public data class ArbeidIPeriodeDTO(
    val periodeFom: LocalDate,
    val periodeTom: LocalDate,
    val timerArbeidet: BigDecimal
)

public data class DetaljertMeldekortDTO(
    val personIdent: String,
    val saksnummer: Saksnummer,
    val behandlingId: Long,
    val journalpostId: String,
    val meldeperiodeFom: LocalDate,
    val meldeperiodeTom: LocalDate,
    val mottattTidspunkt: LocalDateTime,
    val timerArbeidPerPeriode: List<ArbeidIPeriodeDTO>,
)
