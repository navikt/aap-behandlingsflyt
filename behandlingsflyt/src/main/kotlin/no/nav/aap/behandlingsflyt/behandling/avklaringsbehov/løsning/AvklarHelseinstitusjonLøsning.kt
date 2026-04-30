package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.AvklarHelseinstitusjonLøser
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.flate.HelseinstitusjonVurderingerDto
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AVKLAR_HELSEINSTITUSJON_KODE
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(value = AVKLAR_HELSEINSTITUSJON_KODE)
class AvklarHelseinstitusjonLøsning(
    @param:JsonProperty(
        "helseinstitusjonVurdering",
        required = true
    ) val helseinstitusjonVurdering: HelseinstitusjonVurderingerDto,
    @param:JsonProperty(
        "behovstype",
        required = true,
        defaultValue = AVKLAR_HELSEINSTITUSJON_KODE
    ) val behovstype: AvklaringsbehovKode = AvklaringsbehovKode.`5011`
) :
// PeriodisertAvklaringsbehovLøsning kan ikke brukes her, fordi den krever at alle perioder er vurdert – noe som ikke gjelder for helseinstitusjon.
// For helseinstitusjon vurderes perioder ut fra tidligste reduksjonsdato, eller perioden saksbehandler har angitt.
    EnkeltAvklaringsbehovLøsning {
    override fun løs(repositoryProvider: RepositoryProvider, kontekst: AvklaringsbehovKontekst, gatewayProvider: GatewayProvider): LøsningsResultat {
        return AvklarHelseinstitusjonLøser(repositoryProvider).løs(kontekst, this)
    }
}