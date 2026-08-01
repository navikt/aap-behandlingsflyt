package no.nav.aap.behandlingsflyt.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.AvklarSamordningSykestipendLøser
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.student.sykestipend.SamordningSykestipendVurderingDto
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AVKLAR_SAMORDNING_SYKESTIPEND_KODE
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(value = AVKLAR_SAMORDNING_SYKESTIPEND_KODE)
class AvklarSamordningSykestipendLøsning(
    @param:JsonProperty("sykestipendVurdering", required = true)
    val sykestipendVurdering: SamordningSykestipendVurderingDto,
    @param:JsonProperty(
        "behovstype",
        required = true,
        defaultValue = AVKLAR_SAMORDNING_SYKESTIPEND_KODE
    )
    val behovstype: AvklaringsbehovKode = AvklaringsbehovKode.`5034`
): EnkeltAvklaringsbehovLøsning {
    override fun løs(
        repositoryProvider: RepositoryProvider,
        kontekst: AvklaringsbehovKontekst,
        gatewayProvider: GatewayProvider
    ): LøsningsResultat {
        return AvklarSamordningSykestipendLøser(repositoryProvider).løs(kontekst, this)
    }
}