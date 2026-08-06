package no.nav.aap.behandlingsflyt.behandling.vilkår.innsikt

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.komponenter.gateway.Gateway

interface PdfGeneratorGateway : Gateway {
    fun genererVurderingerOppsummeringDokument(request: Dokument): ByteArray
}

data class Dokument(
    val tittel: String,
    val body: List<DOM>,
    val header: String? = null,
)

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type",
    visible = true
)
sealed interface DOM {
    @JsonTypeName("overskrift")
    data class Header(val nivå: Int?, val overskrift: String) : DOM

    @JsonTypeName("avsnitt")
    class Avsnitt(avsnitt: String) : DOM {
        val avsnitt = avsnitt.trim().replace(Regex("""[ \t]+"""), " ")
    }

    @JsonTypeName("liste")
    class List(liste: kotlin.collections.List<Pair<String, String>>) : DOM {
        val liste = liste.map { it.toList() }
    }

    @JsonTypeName("tabell")
    class Tabell(
        val kolonner: kotlin.collections.List<String>,
        val rader: kotlin.collections.List<kotlin.collections.List<String>>
    ) : DOM {
        init {
            require(kolonner.isNotEmpty()) {
                "Tabeller må ha minst en kolonne."
            }
            rader.forEachIndexed { index, rad ->
                require(rad.size == kolonner.size) {
                    "Tabellen har ${kolonner.size} kolonner, men rad på index $index har ${rad.size} kolonner"
                }
            }
        }
    }
}