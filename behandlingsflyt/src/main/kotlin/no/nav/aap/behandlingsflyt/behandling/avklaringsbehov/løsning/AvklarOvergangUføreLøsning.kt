package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.AvklarOvergangUføreLøser
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.PeriodisertVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangufore.OvergangUføreRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangufore.OvergangUføreVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangufore.flate.OvergangUføreLøsningDto
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AVKLAR_OVERGANG_UFORE
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.lookup.repository.RepositoryProvider
import kotlin.collections.orEmpty

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
) : PeriodisertAvklaringsbehovLøsning<OvergangUføreLøsningDto>, LøsningMedPeriodiserteVurderinger {
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

    override fun hentVurderinger(
        behandlingId: BehandlingId,
        repositoryProvider: RepositoryProvider
    ): List<PeriodisertVurdering> {
        val repository = repositoryProvider.provide<OvergangUføreRepository>()
        return repository.hentHvisEksisterer(behandlingId)?.vurderinger.orEmpty()
    }

    override fun somVurderinger(bruker: Bruker, behandlingId: BehandlingId): List<OvergangUføreVurdering> {
        return løsningerForPerioder.map { it.tilOvergangUføreVurdering(bruker, behandlingId) }
    }
}




