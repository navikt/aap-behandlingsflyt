package no.nav.aap.behandlingsflyt.kontrakt.statistikk

import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * @param beregningsGrunnlag Beregningsgrunnlag. Kan være null om behandlingen avsluttes før inntekt hentes inn.
 */
public data class AvsluttetBehandlingDTO(
    val tilkjentYtelse: TilkjentYtelseDTO,
    val vilkårsResultat: VilkårsResultatDTO,
    val beregningsGrunnlag: BeregningsgrunnlagDTO?,
    val diagnoser: Diagnoser? = null,
    val rettighetstypePerioder: List<RettighetstypePeriode>,
    val resultat: ResultatKode?,
    val vedtakstidspunkt: LocalDateTime?
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
    val barnetilleggSats: Double,
    val barnetillegg: Double
)

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

    @Deprecated("Skal fases ut.")
    SYKEPENGEERSTATNING,
    SAMORDNING,
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
    val grunnlag11_19dto: Grunnlag11_19DTO? = null,
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
    val uføregrad: Int,
    val uføreInntekterFraForegåendeÅr: Map<String, Double>,
    val uføreYtterligereNedsattArbeidsevneÅr: Int,
)

public enum class UføreType {
    STANDARD, YTTERLIGERE_NEDSATT
}