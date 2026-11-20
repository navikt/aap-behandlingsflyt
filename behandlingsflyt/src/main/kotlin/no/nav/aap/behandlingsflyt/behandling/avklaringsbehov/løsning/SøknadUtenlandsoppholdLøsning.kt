package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.BekreftBrevSendtLøser
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.VurderSøknadUtenlandsoppholdLøser
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.BEKREFT_BREV_SENDT_MANUELT_KODE
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.VURDER_SØKNAD_UTENLANDSOPPHOLD_KODE
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.utenlandsopphold.BekreftBrevSendtDto
import no.nav.aap.utenlandsopphold.SøknadUtenlandsoppholdVurderingDto

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(value = VURDER_SØKNAD_UTENLANDSOPPHOLD_KODE)
class VurderSøknadUtenlandsoppholdLøsning(
    @param:JsonProperty("vurdering", required = true) val vurdering: SøknadUtenlandsoppholdVurderingDto,
    @param:JsonProperty(
        "behovstype",
        required = true,
        defaultValue = VURDER_SØKNAD_UTENLANDSOPPHOLD_KODE
    ) val behovstype: AvklaringsbehovKode = AvklaringsbehovKode.`4401`
) : EnkeltAvklaringsbehovLøsning {
    override fun løs(
        repositoryProvider: RepositoryProvider,
        kontekst: AvklaringsbehovKontekst,
        gatewayProvider: GatewayProvider
    ): LøsningsResultat {
        return VurderSøknadUtenlandsoppholdLøser(repositoryProvider).løs(kontekst, this)
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(value = BEKREFT_BREV_SENDT_MANUELT_KODE)
class BekreftBrevSendtLøsning(
    @param:JsonProperty("bekreftelse", required = true) val bekreftelse: BekreftBrevSendtDto,
    @param:JsonProperty(
        "behovstype",
        required = true,
        defaultValue = BEKREFT_BREV_SENDT_MANUELT_KODE
    ) val behovstype: AvklaringsbehovKode = AvklaringsbehovKode.`4402`
) : EnkeltAvklaringsbehovLøsning {
    override fun løs(
        repositoryProvider: RepositoryProvider,
        kontekst: AvklaringsbehovKontekst,
        gatewayProvider: GatewayProvider
    ): LøsningsResultat {
        return BekreftBrevSendtLøser().løs(kontekst, this)
    }
}
