package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.AvklarBistandLøser
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.flate.BistandLøsningDto
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AVKLAR_BISTANDSBEHOV_KODE
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider


@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(value = AVKLAR_BISTANDSBEHOV_KODE)
class AvklarBistandsbehovEnkelLøsning(
    @param:JsonProperty("bistandsVurdering", required = true)
    val bistandsVurdering: BistandLøsningDto,
    @param:JsonProperty(
        "behovstype",
        required = true,
        defaultValue = AVKLAR_BISTANDSBEHOV_KODE
    ) val behovstype: AvklaringsbehovKode = AvklaringsbehovKode.`5006`
) : EnkeltAvklaringsbehovLøsning {
    override fun løs(
        repositoryProvider: RepositoryProvider,
        kontekst: AvklaringsbehovKontekst,
        gatewayProvider: GatewayProvider
    ): LøsningsResultat {
        return AvklarBistandLøser(repositoryProvider).løs(kontekst, this.tilNyLøsning())
    }

    fun tilNyLøsning(): AvklarBistandsbehovLøsning {
        return AvklarBistandsbehovLøsning(
            løsningerForPerioder = listOf(this.bistandsVurdering)
        )
    }
}


