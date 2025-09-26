package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.FastsettFullmektigLøser
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.fullmektig.FullmektigVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.fullmektig.IdentMedType
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.fullmektig.NavnOgAdresse
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.FASTSETT_FULLMEKTIG_KODE
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.lookup.repository.RepositoryProvider

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(value = FASTSETT_FULLMEKTIG_KODE)
class FastsettFullmektigLøsning(
    @param:JsonProperty("fullmektigVurdering", required = true)
    val fullmektigVurdering: FullmektigLøsningDto,
    @param:JsonProperty(
        "behovstype",
        required = true,
        defaultValue = FASTSETT_FULLMEKTIG_KODE
    ) val behovstype: AvklaringsbehovKode = AvklaringsbehovKode.`6009`
) : AvklaringsbehovLøsning {
    override fun løs(repositoryProvider: RepositoryProvider, kontekst: AvklaringsbehovKontekst, gatewayProvider: GatewayProvider): LøsningsResultat {
        return FastsettFullmektigLøser(repositoryProvider, gatewayProvider).løs(kontekst, this)
    }
}

data class FullmektigLøsningDto(
    val harFullmektig: Boolean,
    val fullmektigIdentMedType: IdentMedType? = null,
    val fullmektigNavnOgAdresse: NavnOgAdresse? = null,

    ) {
    fun tilVurdering(vurdertAv: Bruker) = FullmektigVurdering(
        harFullmektig = this.harFullmektig,
        fullmektigIdent = this.fullmektigIdentMedType,
        fullmektigNavnOgAdresse = this.fullmektigNavnOgAdresse,
        vurdertAv = vurdertAv.ident,
    )
}
