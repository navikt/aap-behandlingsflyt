package no.nav.aap.behandlingsflyt.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.ArbeidsopptrappingLøser
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.steg.arbeidsopptrapping.ArbeidsopptrappingLøsningDto
import no.nav.aap.behandlingsflyt.steg.arbeidsopptrapping.ArbeidsopptrappingRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.ARBEIDSOPPTRAPPING_KODE
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.behandling.BehandlingId
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.lookup.repository.RepositoryProvider

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
) : PeriodisertAvklaringsbehovLøsning<ArbeidsopptrappingLøsningDto> {
    override fun løs(
        repositoryProvider: RepositoryProvider,
        kontekst: AvklaringsbehovKontekst,
        gatewayProvider: GatewayProvider
    ): LøsningsResultat {
        return ArbeidsopptrappingLøser(repositoryProvider).løs(kontekst, this)
    }

    override fun hentLagredeLøstePerioder(
        behandlingId: BehandlingId,
        repositoryProvider: RepositoryProvider
    ): Tidslinje<*> {
        val repository = repositoryProvider.provide<ArbeidsopptrappingRepository>()
        return repository.hentHvisEksisterer(behandlingId)?.gjeldendeVurderinger() ?: Tidslinje<Unit>()
    }
}