package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.AvklarOppfølgingLokalkontorLøser
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.AvklarOppfølgingNAYLøser
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.behandling.oppfølgingsbehandling.OppfølgingsoppgaveGrunnlagDto
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AVKLAR_OPPFØLGINGSBEHOV_LOKALKONTOR
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AVKLAR_OPPFØLGINGSBEHOV_NAY
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.lookup.repository.RepositoryProvider

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(value = AVKLAR_OPPFØLGINGSBEHOV_LOKALKONTOR)
class AvklarOppfølgingLokalkontorLøsning(
    @param:JsonProperty(
        "avklarOppfølgingsbehovVurdering",
        required = true
    ) val avklarOppfølgingsbehovVurdering: OppfølgingsoppgaveGrunnlagDto,
    @param:JsonProperty(
        "behovstype",
        required = true,
        defaultValue = AVKLAR_OPPFØLGINGSBEHOV_LOKALKONTOR
    ) val behovstype: AvklaringsbehovKode = AvklaringsbehovKode.`8001`
) :
    AvklaringsbehovLøsning {
    override fun løs(repositoryProvider: RepositoryProvider, kontekst: AvklaringsbehovKontekst): LøsningsResultat {
        return AvklarOppfølgingLokalkontorLøser(repositoryProvider).løs(kontekst, this)
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(value = AVKLAR_OPPFØLGINGSBEHOV_NAY)
class AvklarOppfølgingNAYLøsning(
    @param:JsonProperty(
        "avklarOppfølgingsbehovVurdering",
        required = true
    ) val avklarOppfølgingsbehovVurdering: OppfølgingsoppgaveGrunnlagDto,
    @param:JsonProperty(
        "behovstype",
        required = true,
        defaultValue = AVKLAR_OPPFØLGINGSBEHOV_NAY
    ) val behovstype: AvklaringsbehovKode = AvklaringsbehovKode.`8002`
) :
    AvklaringsbehovLøsning {
    override fun løs(repositoryProvider: RepositoryProvider, kontekst: AvklaringsbehovKontekst): LøsningsResultat {
        return AvklarOppfølgingNAYLøser(repositoryProvider).løs(kontekst, this)
    }
}