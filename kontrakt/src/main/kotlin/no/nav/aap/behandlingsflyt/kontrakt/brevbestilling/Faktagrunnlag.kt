package no.nav.aap.behandlingsflyt.kontrakt.brevbestilling

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import com.fasterxml.jackson.annotation.JsonValue
import java.time.LocalDate

// TODO: Fjern alt relatert til TESTVERDI når det ikke brukes i brevinnhold
public const val FAKTAGRUNNLAG_TYPE_TESTVERDI: String = "TESTVERDI"
public const val FAKTAGRUNNLAG_TYPE_FRIST_DATO_11_7: String = "FRIST_DATO_11_7"

public enum class FaktagrunnlagType(@JsonValue public val verdi: String) {
    TESTVERDI(FAKTAGRUNNLAG_TYPE_TESTVERDI),
    FRIST_DATO_11_7(FAKTAGRUNNLAG_TYPE_FRIST_DATO_11_7)
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
}


