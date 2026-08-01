package no.nav.aap.behandlingsflyt.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.AvklarOvergangUføreLøser
import no.nav.aap.behandlingsflyt.steg.overgangufore.OvergangUføreRepository
import no.nav.aap.behandlingsflyt.steg.overgangufore.OvergangUføreLøsningDto
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AVKLAR_OVERGANG_UFORE
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.behandling.BehandlingId
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.lookup.repository.RepositoryProvider

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(value = AVKLAR_OVERGANG_UFORE)
class AvklarOvergangUføreLøsning(
    @param:JsonProperty("løsningerForPerioder")
    override val løsningerForPerioder: List<OvergangUføreLøsningDto>,
    @param:JsonProperty(
        "behovstype",
        required = true,
        defaultValue = AVKLAR_OVERGANG_UFORE
    ) val behovstype: AvklaringsbehovKode = AvklaringsbehovKode.`5031`
) : PeriodisertAvklaringsbehovLøsning<OvergangUføreLøsningDto> {
    override fun løs(
        repositoryProvider: RepositoryProvider,
        kontekst: AvklaringsbehovKontekst,
        gatewayProvider: GatewayProvider
    ): LøsningsResultat {
        return AvklarOvergangUføreLøser(repositoryProvider, gatewayProvider).løs(kontekst, this)
    }


    override fun hentLagredeLøstePerioder(
        behandlingId: BehandlingId,
        repositoryProvider: RepositoryProvider
    ): Tidslinje<*> {
        val repository = repositoryProvider.provide<OvergangUføreRepository>()
        return repository.hentHvisEksisterer(behandlingId)?.somOvergangUforevurderingstidslinje() ?: Tidslinje<Unit>()
    }
}




