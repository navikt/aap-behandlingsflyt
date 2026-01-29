package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.AvklarOvergangUføreLøser
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsopptrapping.ArbeidsopptrappingRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangufore.OvergangUføreRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangufore.flate.OvergangUføreLøsningDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangufore.flate.OvergangUføreVurderingLøsningDto
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AVKLAR_OVERGANG_UFORE
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.lookup.repository.RepositoryProvider


@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(value = AVKLAR_OVERGANG_UFORE)
class AvklarOvergangUføreEnkelLøsning(
    @Deprecated("Bruk løsninger for perioder")
    @param:JsonProperty("overgangUføreVurdering", required = true)
    val overgangUføreVurdering: OvergangUføreVurderingLøsningDto? = null,
    @param:JsonProperty("løsningerForPerioder")
    val løsningerForPerioder: List<OvergangUføreVurderingLøsningDto>? = null,
    @param:JsonProperty(
        "behovstype",
        required = true,
        defaultValue = AVKLAR_OVERGANG_UFORE
    ) val behovstype: AvklaringsbehovKode = AvklaringsbehovKode.`5031`
) : EnkeltAvklaringsbehovLøsning {
    override fun løs(
        repositoryProvider: RepositoryProvider,
        kontekst: AvklaringsbehovKontekst,
        gatewayProvider: GatewayProvider
    ): LøsningsResultat {
        if (løsningerForPerioder.isNullOrEmpty()) {
            if (overgangUføreVurdering == null) {
                throw IllegalArgumentException("Enten overgangUføreVurdering eller løsningerForPerioder må være satt")
            }
        }
        return AvklarOvergangUføreLøser(repositoryProvider, gatewayProvider).løs(kontekst, this)
    }
}

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
        return AvklarOvergangUføreLøser(repositoryProvider, gatewayProvider).løs(
            kontekst,
            this.tilGammelLøsning()
        )
    }
    
    fun tilGammelLøsning(): AvklarOvergangUføreEnkelLøsning {
        return AvklarOvergangUføreEnkelLøsning(
            overgangUføreVurdering = null,
            løsningerForPerioder = løsningerForPerioder.map { it.tilGammelDto() }
        )
    }

    override fun hentTidligereLøstePerioder(
        behandlingId: BehandlingId,
        repositoryProvider: RepositoryProvider
    ): Tidslinje<*> {
        val repository = repositoryProvider.provide<OvergangUføreRepository>()
        return repository.hentHvisEksisterer(behandlingId)?.somOvergangUforevurderingstidslinje() ?: Tidslinje<Unit>()
    }
}




