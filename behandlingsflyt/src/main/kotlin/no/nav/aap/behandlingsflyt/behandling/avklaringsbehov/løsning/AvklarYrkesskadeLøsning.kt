package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.AvklarYrkesskadeLøser
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AVKLAR_YRKESSKADE_KODE
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.LocalDate

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(value = AVKLAR_YRKESSKADE_KODE)
class AvklarYrkesskadeLøsning(
    @param:JsonProperty("yrkesskadesvurdering", required = true) val yrkesskadesvurdering: YrkesskadevurderingDto,
    @param:JsonProperty(
        "behovstype",
        required = true,
        defaultValue = AVKLAR_YRKESSKADE_KODE
    ) val behovstype: AvklaringsbehovKode = AvklaringsbehovKode.`5013`
) : EnkeltAvklaringsbehovLøsning {
    override fun løs(
        repositoryProvider: RepositoryProvider,
        kontekst: AvklaringsbehovKontekst,
        gatewayProvider: GatewayProvider
    ): LøsningsResultat {
        return AvklarYrkesskadeLøser(repositoryProvider).løs(kontekst, this)
    }
}


data class YrkesskadevurderingDto(
    val begrunnelse: String,
    @Deprecated("Bruk relevanteYrkesskadeSaker")
    val relevanteSaker: List<String>,
    val relevanteYrkesskadeSaker: List<YrkesskadeSakDto>,
    val andelAvNedsettelsen: Int?,
    val erÅrsakssammenheng: Boolean
) {
    fun relevanteSaker(): List<YrkesskadeSakDto> {
        // Fjern denne når relevanteSaker er fjernet
        return relevanteYrkesskadeSaker + relevanteSaker.map { YrkesskadeSakDto(it, null) }
            .filter { it.referanse !in relevanteYrkesskadeSaker.map { sak -> sak.referanse } }
    }
}

data class YrkesskadeSakDto(
    val referanse: String,
    val manuellYrkesskadeDato: LocalDate?
)