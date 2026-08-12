package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.AvklarMigreringsdatoLøser
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AVKLAR_MIGRERINGSDATO_KODE
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.LocalDate

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(value = AVKLAR_MIGRERINGSDATO_KODE)
class AvklarMigreringsdatoLøsning(
    @param:JsonProperty("behovstype", required = true, defaultValue = AVKLAR_MIGRERINGSDATO_KODE)
    val behovstype: AvklaringsbehovKode = AvklaringsbehovKode.`5061`,
    @param:JsonProperty("migreringsdato", required = true)
    val migreringsdato: LocalDate,
) : EnkeltAvklaringsbehovLøsning {
    override fun løs(
        repositoryProvider: RepositoryProvider,
        kontekst: AvklaringsbehovKontekst,
        gatewayProvider: GatewayProvider
    ): LøsningsResultat {
        return AvklarMigreringsdatoLøser(repositoryProvider).løs(kontekst, this)
    }
}
