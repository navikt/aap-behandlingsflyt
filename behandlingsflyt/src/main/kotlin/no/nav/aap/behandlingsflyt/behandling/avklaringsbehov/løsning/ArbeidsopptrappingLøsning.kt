package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.ArbeidsopptrappingLøser
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsopptrapping.ArbeidsopptrappingLøsningDto
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.ARBEIDSOPPTRAPPING_KODE
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(value = ARBEIDSOPPTRAPPING_KODE)
class ArbeidsopptrappingLøsning(

    @param:JsonProperty("løsningerForPerioder", required = true)
    override val løsningerForPerioder: List<ArbeidsopptrappingLøsningDto>,

    @param:JsonProperty(
        "behovstype",
        required = true,
        defaultValue = ARBEIDSOPPTRAPPING_KODE
    ) val behovstype: AvklaringsbehovKode = AvklaringsbehovKode.`5057`
) : PeriodisertAvklaringsbehovLøsning<ArbeidsopptrappingLøsningDto> {
    override fun løs(
        repositoryProvider: RepositoryProvider,
        kontekst: AvklaringsbehovKontekst,
        gatewayProvider: GatewayProvider
    ): LøsningsResultat {
        return ArbeidsopptrappingLøser(repositoryProvider).løs(kontekst, this)
    }
}