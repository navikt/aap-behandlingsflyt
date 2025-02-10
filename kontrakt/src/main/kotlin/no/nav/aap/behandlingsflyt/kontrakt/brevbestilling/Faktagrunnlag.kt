package no.nav.aap.behandlingsflyt.kontrakt.brevbestilling

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import com.fasterxml.jackson.annotation.JsonValue
import java.math.BigDecimal
import java.time.LocalDate
import java.time.Year

// TODO: Fjern alt relatert til TESTVERDI når det ikke brukes i brevinnhold
public const val FAKTAGRUNNLAG_TYPE_TESTVERDI: String = "TESTVERDI"
public const val FAKTAGRUNNLAG_TYPE_FRIST_DATO_11_7: String = "FRIST_DATO_11_7"
public const val FAKTAGRUNNLAG_TYPE_GRUNNLAG_BEREGNING: String = "GRUNNLAG_BEREGNING"

public enum class FaktagrunnlagType(@JsonValue public val verdi: String) {
    TESTVERDI(FAKTAGRUNNLAG_TYPE_TESTVERDI),
    FRIST_DATO_11_7(FAKTAGRUNNLAG_TYPE_FRIST_DATO_11_7),
    GRUNNLAG_BEREGNING(FAKTAGRUNNLAG_TYPE_GRUNNLAG_BEREGNING),
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type", visible = true)
public sealed class Faktagrunnlag(public val type: FaktagrunnlagType) {
    @JsonTypeName(FAKTAGRUNNLAG_TYPE_TESTVERDI)
    public data class Testverdi(
        val testString: String
    ) : Faktagrunnlag(FaktagrunnlagType.TESTVERDI)

    @JsonTypeName(FAKTAGRUNNLAG_TYPE_FRIST_DATO_11_7)
    public data class FristDato11_7(
        val frist: LocalDate
    ) : Faktagrunnlag(FaktagrunnlagType.FRIST_DATO_11_7)

    @JsonTypeName(FAKTAGRUNNLAG_TYPE_GRUNNLAG_BEREGNING)
    public data class GrunnlagBeregning(
        val inntekterPerÅr: List<InntektPerÅr>
    ) : Faktagrunnlag(FaktagrunnlagType.GRUNNLAG_BEREGNING) {
        public data class InntektPerÅr(val år: Year, val inntekt: BigDecimal)
    }
}


