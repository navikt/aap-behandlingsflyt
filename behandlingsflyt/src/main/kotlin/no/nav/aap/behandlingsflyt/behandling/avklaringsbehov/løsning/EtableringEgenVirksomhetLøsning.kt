package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.EtableringEgenVirksomhetLøser
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.EtableringEgenVirksomhetLøsningDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.EtableringEgenVirksomhetRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.ETABLERING_EGEN_VIRKSOMHET_KODE
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.lookup.repository.RepositoryProvider

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(value = ETABLERING_EGEN_VIRKSOMHET_KODE)
class EtableringEgenVirksomhetLøsning(
    @param:JsonProperty("løsningerForPerioder", required = true)
    override val løsningerForPerioder: List<EtableringEgenVirksomhetLøsningDto>,
    @param:JsonProperty(
        "behovstype",
        required = true,
        defaultValue = ETABLERING_EGEN_VIRKSOMHET_KODE
    ) val behovstype: AvklaringsbehovKode = AvklaringsbehovKode.`5058`
) : PeriodisertAvklaringsbehovLøsning<EtableringEgenVirksomhetLøsningDto> {
    override fun løs(
        repositoryProvider: RepositoryProvider,
        kontekst: AvklaringsbehovKontekst,
        gatewayProvider: GatewayProvider
    ): LøsningsResultat {
        return EtableringEgenVirksomhetLøser(
            repositoryProvider,
            gatewayProvider
        ).løs(kontekst, this)
    }

    override fun hentLagredeLøstePerioder(
        behandlingId: BehandlingId,
        repositoryProvider: RepositoryProvider
    ): Tidslinje<*> {
        val repository = repositoryProvider.provide<EtableringEgenVirksomhetRepository>()
        return repository.hentHvisEksisterer(behandlingId)?.gjeldendeVurderinger() ?: Tidslinje<Unit>()
    }
}