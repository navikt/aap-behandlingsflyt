package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.VENTE_PÅ_FIRST_EFFEKTUER_11_7_KODE
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(value = VENTE_PÅ_FIRST_EFFEKTUER_11_7_KODE)
class VentePåFristEffektuer11_7Løsning(
    @param:JsonProperty("behovstype", required = true, defaultValue = VENTE_PÅ_FIRST_EFFEKTUER_11_7_KODE)
    val behovstype: AvklaringsbehovKode = AvklaringsbehovKode.`5018`
) : AvklaringsbehovLøsning {
    override fun løs(repositoryProvider: RepositoryProvider, kontekst: AvklaringsbehovKontekst, gatewayProvider: GatewayProvider): LøsningsResultat {
        return LøsningsResultat(
            begrunnelse = "",
            kreverToTrinn = false,
        )
    }
}
