package no.nav.aap.behandlingsflyt.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.AvklarSykepengerErstatningLøser
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.misc.gjeldendeVurderinger
import no.nav.aap.behandlingsflyt.steg.sykepengeerstatning.SykepengerErstatningRepository
import no.nav.aap.behandlingsflyt.steg.sykepengeerstatning.PeriodisertSykepengerVurderingDto
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.VURDER_SYKEPENGEERSTATNING_KODE
import no.nav.aap.behandling.BehandlingId
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.orEmpty
import no.nav.aap.lookup.repository.RepositoryProvider

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(value = VURDER_SYKEPENGEERSTATNING_KODE)
class PeriodisertAvklarSykepengerErstatningLøsning(
    @param:JsonProperty(
        "behovstype",
        required = true,
        defaultValue = VURDER_SYKEPENGEERSTATNING_KODE
    ) val behovstype: AvklaringsbehovKode = AvklaringsbehovKode.`5007`,
    override val løsningerForPerioder: List<PeriodisertSykepengerVurderingDto>,
): PeriodisertAvklaringsbehovLøsning<PeriodisertSykepengerVurderingDto> {
    override fun løs(
        repositoryProvider: RepositoryProvider,
        kontekst: AvklaringsbehovKontekst,
        gatewayProvider: GatewayProvider
    ): LøsningsResultat {
        return AvklarSykepengerErstatningLøser(repositoryProvider, gatewayProvider).løs(kontekst, this)
    }

    override fun hentLagredeLøstePerioder(
        behandlingId: BehandlingId,
        repositoryProvider: RepositoryProvider
    ): Tidslinje<*> {
        val repository = repositoryProvider.provide<SykepengerErstatningRepository>()
        return repository.hentHvisEksisterer(behandlingId)?.vurderinger?.gjeldendeVurderinger().orEmpty()
    }
}