package no.nav.aap.behandlingsflyt.kontrakt.brevbestilling

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import com.fasterxml.jackson.annotation.JsonValue

// TODO: Fjern alt relatert til TESTVERDI når et ekte faktagrunnlag er lagt til
public const val FAKTAGRUNNLAG_TYPE_TESTVERDI: String = "TESTVERDI"

public enum class FaktagrunnlagType(@JsonValue public val verdi: String) {
    TESTVERDI(FAKTAGRUNNLAG_TYPE_TESTVERDI)
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type", visible = true)
public sealed class Faktagrunnlag(public val type: FaktagrunnlagType) {
    @JsonTypeName(FAKTAGRUNNLAG_TYPE_TESTVERDI)
    public data class Testverdi(
        val testString: String
    ) : Faktagrunnlag(FaktagrunnlagType.TESTVERDI)

}


