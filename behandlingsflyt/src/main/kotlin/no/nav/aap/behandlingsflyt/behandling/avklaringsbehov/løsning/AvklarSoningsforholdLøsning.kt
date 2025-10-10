package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.AvklarSoningsforholdLøser
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.flate.SoningsvurderingerDto
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AVKLAR_SONINGSFORRHOLD_KODE
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(value = AVKLAR_SONINGSFORRHOLD_KODE)
class AvklarSoningsforholdLøsning(
    @param:JsonProperty("soningsvurdering", required = true) val soningsvurdering: SoningsvurderingerDto,
    @param:JsonProperty(
        "behovstype",
        required = true,
        defaultValue = AVKLAR_SONINGSFORRHOLD_KODE
    ) val behovstype: AvklaringsbehovKode = AvklaringsbehovKode.`5010`
) :
    EnkeltAvklaringsbehovLøsning {
    override fun løs(repositoryProvider: RepositoryProvider, kontekst: AvklaringsbehovKontekst, gatewayProvider: GatewayProvider): LøsningsResultat {
        return AvklarSoningsforholdLøser(repositoryProvider).løs(kontekst, this)
    }

}
