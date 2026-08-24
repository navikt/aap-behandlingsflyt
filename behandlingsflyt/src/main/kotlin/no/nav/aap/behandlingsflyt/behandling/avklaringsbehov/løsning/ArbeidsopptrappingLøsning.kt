package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.ArbeidsopptrappingLøser
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.PeriodisertVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsopptrapping.ArbeidsopptrappingLøsningDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsopptrapping.ArbeidsopptrappingRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsopptrapping.ArbeidsopptrappingVurdering
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.ARBEIDSOPPTRAPPING_KODE
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.lookup.repository.RepositoryProvider
import kotlin.collections.orEmpty

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
) : PeriodisertAvklaringsbehovLøsning<ArbeidsopptrappingLøsningDto>, LøsningMedPeriodiserteVurderinger {
    override fun løs(
        repositoryProvider: RepositoryProvider,
        kontekst: AvklaringsbehovKontekst,
        gatewayProvider: GatewayProvider
    ): LøsningsResultat {
        return ArbeidsopptrappingLøser(repositoryProvider).løs(kontekst, this)
    }

    override fun hentLagredeLøstePerioder(
        behandlingId: BehandlingId,
        repositoryProvider: RepositoryProvider,
        gatewayProvider: GatewayProvider
    ): Tidslinje<*> {
        val repository = repositoryProvider.provide<ArbeidsopptrappingRepository>()
        return repository.hentHvisEksisterer(behandlingId)?.gjeldendeVurderinger() ?: Tidslinje<Unit>()
    }

    override fun hentVurderinger(
        behandlingId: BehandlingId,
        repositoryProvider: RepositoryProvider
    ): List<PeriodisertVurdering> {
        val repository = repositoryProvider.provide<ArbeidsopptrappingRepository>()
        return repository.hentHvisEksisterer(behandlingId)?.vurderinger.orEmpty()
    }

    override fun somVurderinger(bruker: Bruker, behandlingId: BehandlingId): List<ArbeidsopptrappingVurdering> {
        return løsningerForPerioder.map { it.toArbeidsopptrappingVurdering(bruker, behandlingId) }
    }
}