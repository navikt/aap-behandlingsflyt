package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.VENTE_PÅ_KLAGE_IMPLEMENTASJON_KODE
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(value = VENTE_PÅ_KLAGE_IMPLEMENTASJON_KODE)
class VentePåKlageimplementasjonLøsning(
    @param:JsonProperty("behovstype", required = true, defaultValue = VENTE_PÅ_KLAGE_IMPLEMENTASJON_KODE)
    val behovstype: AvklaringsbehovKode = AvklaringsbehovKode.`5023`
) : AvklaringsbehovLøsning {
    override fun løs(repositoryProvider: RepositoryProvider, kontekst: AvklaringsbehovKontekst, gatewayProvider: GatewayProvider): LøsningsResultat {
        return LøsningsResultat(
            begrunnelse = "",
            kreverToTrinn = false,
        )
    }
}
