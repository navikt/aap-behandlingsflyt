package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.TrekkSøknadLøser
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.VURDER_TREKK_AV_SØKNAD_KODE
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

@JsonTypeName(value = VURDER_TREKK_AV_SØKNAD_KODE)
class TrekkSøknadLøsning(
    @param:JsonProperty("behovstype", required = true, defaultValue = VURDER_TREKK_AV_SØKNAD_KODE)
    val behovstype: AvklaringsbehovKode = AvklaringsbehovKode.`5028`,
    @param:JsonProperty("begrunnelse", required = true) val begrunnelse: String,
) : AvklaringsbehovLøsning {
    override fun løs(repositoryProvider: RepositoryProvider, kontekst: AvklaringsbehovKontekst, gatewayProvider: GatewayProvider): LøsningsResultat {
        return TrekkSøknadLøser(repositoryProvider).løs(kontekst, this)
    }
}
