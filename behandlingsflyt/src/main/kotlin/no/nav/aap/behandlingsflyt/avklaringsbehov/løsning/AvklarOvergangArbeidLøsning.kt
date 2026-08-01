package no.nav.aap.behandlingsflyt.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.AvklarOvergangArbeidLøser
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.steg.overgangarbeid.OvergangArbeidRepository
import no.nav.aap.behandlingsflyt.steg.overgangarbeid.OvergangArbeidVurderingLøsningDto
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AVKLAR_OVERGANG_ARBEID
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.behandling.BehandlingId
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.orEmpty
import no.nav.aap.lookup.repository.RepositoryProvider


@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(value = AVKLAR_OVERGANG_ARBEID)
class AvklarOvergangArbeidLøsning(
    @param:JsonProperty("løsningerForPerioder", required = true)
    override val løsningerForPerioder: List<OvergangArbeidVurderingLøsningDto>,

    @param:JsonProperty(
        "behovstype",
        required = true,
        defaultValue = AVKLAR_OVERGANG_ARBEID
    ) val behovstype: AvklaringsbehovKode = AvklaringsbehovKode.`5032`
) : PeriodisertAvklaringsbehovLøsning<OvergangArbeidVurderingLøsningDto> {
    override fun løs(repositoryProvider: RepositoryProvider, kontekst: AvklaringsbehovKontekst, gatewayProvider: GatewayProvider): LøsningsResultat {
        return AvklarOvergangArbeidLøser(repositoryProvider).løs(kontekst, this)
    }

    override fun hentLagredeLøstePerioder(
        behandlingId: BehandlingId,
        repositoryProvider: RepositoryProvider
    ): Tidslinje<*> {
        val repository = repositoryProvider.provide<OvergangArbeidRepository>()
        return repository.hentHvisEksisterer(behandlingId)?.gjeldendeVurderinger().orEmpty()
    }
}
