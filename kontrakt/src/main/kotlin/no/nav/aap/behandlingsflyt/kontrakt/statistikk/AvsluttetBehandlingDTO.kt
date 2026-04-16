package no.nav.aap.behandlingsflyt.kontrakt.statistikk

import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Sendes til statistikkappen når en behandling avsluttes.
 *
 * @param beregningsGrunnlag Beregningsgrunnlag. Kan være null om behandlingen avsluttes før inntekt hentes inn.
 */
public data class AvsluttetBehandlingDTO(
    val tilkjentYtelse: TilkjentYtelseDTO,
    val vilkårsResultat: VilkårsResultatDTO,
    val beregningsGrunnlag: BeregningsgrunnlagDTO?,
    val diagnoser: Diagnoser? = null,
    val rettighetstypePerioder: List<RettighetstypePeriode>,
    val resultat: ResultatKode?,
    val vedtakstidspunkt: LocalDateTime?,
    val fritaksvurderinger: Iterable<Fritakvurdering>? = null,
    val perioderMedArbeidsopptrapping: List<PeriodeDTO>,
    val vedtattStansOpphør: List<StansEllerOpphør>
)

public data class PeriodeDTO(val fom: LocalDate, val tom: LocalDate)

public data class Fritakvurdering(
    val harFritak: Boolean,
    val fraDato: LocalDate,
    val tilDato: LocalDate? = null,
)

public enum class ResultatKode {
    INNVILGET,
    AVSLAG,
    TRUKKET,
    KLAGE_OPPRETTHOLDES,
    KLAGE_OMGJØRES,
    KLAGE_DELVIS_OMGJØRES,
    KLAGE_AVSLÅTT,
    KLAGE_TRUKKET,
    AVBRUTT
}

public data class StansEllerOpphør(
    val type: Avslagstype,
    val fom: LocalDate,
    val årsaker: Set<Avslagsårsak>
)

public enum class Avslagsårsak {
    BRUKER_UNDER_18,
    BRUKER_OVER_67,
    MANGLENDE_DOKUMENTASJON,
    IKKE_RETT_PA_SYKEPENGEERSTATNING,
    IKKE_RETT_PA_STUDENT,
    VARIGHET_OVERSKREDET_STUDENT,
    IKKE_SYKDOM_AV_VISS_VARIGHET,
    IKKE_SYKDOM_SKADE_LYTE_VESENTLIGDEL,
    IKKE_NOK_REDUSERT_ARBEIDSEVNE,
    IKKE_BEHOV_FOR_OPPFOLGING,
    IKKE_MEDLEM_FORUTGÅENDE,
    IKKE_MEDLEM,
    IKKE_OPPFYLT_OPPHOLDSKRAV_EØS,
    NORGE_IKKE_KOMPETENT_STAT,
    ANNEN_FULL_YTELSE,
    INNTEKTSTAP_DEKKES_ETTER_ANNEN_LOVGIVNING,
    IKKE_RETT_PA_AAP_UNDER_BEHANDLING_AV_UFORE,
    VARIGHET_OVERSKREDET_OVERGANG_UFORE,
    VARIGHET_OVERSKREDET_ARBEIDSSØKER,
    IKKE_RETT_PA_AAP_I_PERIODE_SOM_ARBEIDSSOKER,
    IKKE_RETT_UNDER_STRAFFEGJENNOMFØRING,
    BRUDD_PÅ_AKTIVITETSPLIKT_STANS,
    BRUDD_PÅ_AKTIVITETSPLIKT_OPPHØR,
    BRUDD_PÅ_OPPHOLDSKRAV_STANS,
    BRUDD_PÅ_OPPHOLDSKRAV_OPPHØR,
    HAR_RETT_TIL_FULLT_UTTAK_ALDERSPENSJON,
    ORDINÆRKVOTE_BRUKT_OPP,
    SYKEPENGEERSTATNINGKVOTE_BRUKT_OPP,
}

public enum class Avslagstype {
    STANS,
    OPPHØR,
}

public data class Diagnoser(
    val kodeverk: String,
    val diagnosekode: String,
    val bidiagnoser: List<String>
)

public data class RettighetstypePeriode(
    val fraDato: LocalDate,
    val tilDato: LocalDate,
    val rettighetstype: RettighetsType
)


/**
 * Liste over perioder med tilkjent ytelse.
 */
public data class TilkjentYtelseDTO(
    val perioder: List<TilkjentYtelsePeriodeDTO>
)

public data class TilkjentYtelsePeriodeDTO(
    val fraDato: LocalDate,
    val tilDato: LocalDate,
    val dagsats: Double,
    val gradering: Double,
    val redusertDagsats: Double,
    val antallBarn: Int,
    val barnepensjonDagsats: Double,
    val barnetilleggSats: Double,
    val barnetillegg: Double,
    val utbetalingsdato: LocalDate,
    val minsteSats: Minstesats,
    val samordningGradering: Double,
    val institusjonGradering: Double,
    val arbeidGradering: Double,
    val samordningUføregradering: Double,
    val samordningArbeidsgiverGradering: Double,
    val meldepliktGradering: Double
)

public enum class Minstesats { IKKE_MINSTESATS, MINSTESATS_OVER_25, MINSTESATS_UNDER_25 }

public data class VilkårsResultatDTO(
    val typeBehandling: TypeBehandling, val vilkår: List<VilkårDTO>
)

public data class VilkårDTO(val vilkårType: Vilkårtype, val perioder: List<VilkårsPeriodeDTO>)


public enum class Utfall {
    IKKE_VURDERT, IKKE_RELEVANT, OPPFYLT, IKKE_OPPFYLT
}

public enum class Vilkårtype {
    ALDERSVILKÅRET,
    SYKDOMSVILKÅRET,
    BISTANDSVILKÅRET,
    MEDLEMSKAP,
    LOVVALG,
    GRUNNLAGET,
    OVERGANGARBEIDVILKÅRET,
    OVERGANGUFØREVILKÅRET,
    STRAFFEGJENNOMFØRING,
    AKTIVITETSPLIKT,
    OPPHOLDSKRAV,
    SYKEPENGEERSTATNING,
    SAMORDNING,
    INNTEKTSBORTFALL,
    SAMORDNING_ANNEN_LOVGIVNING,
    STUDENT,
    ORDINÆR_KVOTE,
    SYKEPENGEERSTATNING_KVOTE
}

public data class VilkårsPeriodeDTO(
    val fraDato: LocalDate,
    val tilDato: LocalDate,
    val utfall: Utfall,
    val manuellVurdering: Boolean,
    val innvilgelsesårsak: String? = null,
    val avslagsårsak: String? = null
)


/**
 * Felter fra BeregningsGrunnlag-interfacet ([no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.Beregningsgrunnlag]),
 * er alltid med. Minst én av grunnlag11_19dto, grunnlagYrkesskade, grunnlagUføre er ikke-null.
 */
public data class BeregningsgrunnlagDTO(
    @Suppress("PropertyName") val grunnlag11_19dto: Grunnlag11_19DTO? = null,
    val grunnlagYrkesskade: GrunnlagYrkesskadeDTO? = null,
    val grunnlagUføre: GrunnlagUføreDTO? = null
) {
    init {
        require(grunnlag11_19dto != null || grunnlagYrkesskade != null || grunnlagUføre != null)
    }
}

public data class Grunnlag11_19DTO(
    val inntekter: Map<String, Double>,
    val grunnlaget: Double,
    val er6GBegrenset: Boolean,
    val erGjennomsnitt: Boolean
)

/**
 * @param [inkludererUføre] Sett til true om [beregningsgrunnlag] er av type [GrunnlagUføreDTO].
 */
public data class GrunnlagYrkesskadeDTO(
    val grunnlaget: BigDecimal,
    val inkludererUføre: Boolean,
    val beregningsgrunnlag: BeregningsgrunnlagDTO,
    val terskelverdiForYrkesskade: Int,
    val andelSomSkyldesYrkesskade: BigDecimal,
    val andelYrkesskade: Int,
    val benyttetAndelForYrkesskade: Int,
    val andelSomIkkeSkyldesYrkesskade: BigDecimal,
    val antattÅrligInntektYrkesskadeTidspunktet: BigDecimal,
    val yrkesskadeTidspunkt: Int,
    val grunnlagForBeregningAvYrkesskadeandel: BigDecimal,
    val yrkesskadeinntektIG: BigDecimal,
    val grunnlagEtterYrkesskadeFordel: BigDecimal
)

/**
 * @property uføreInntekterFraForegåendeÅr Uføre ikke oppjustert
 * @property uføreInntekterFraForegåendeÅr Grunnlaget
 */
public data class GrunnlagUføreDTO(
    val grunnlaget: BigDecimal,
    val type: UføreType,
    val grunnlag: Grunnlag11_19DTO,
    val grunnlagYtterligereNedsatt: Grunnlag11_19DTO,
    @Deprecated("Bruk uføregrader.")
    val uføregrad: Int,
    val uføregrader: List<Uføre>,
    val uføreInntekterFraForegåendeÅr: Map<String, Double>,
    val uføreYtterligereNedsattArbeidsevneÅr: Int,
)

public data class Uføre(val grad: Int, val virkningstidspunkt: LocalDate)

public enum class UføreType {
    STANDARD, YTTERLIGERE_NEDSATT
}