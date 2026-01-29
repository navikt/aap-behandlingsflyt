package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.AvklarForutgåendeMedlemskapLøser
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.PeriodisertManuellVurderingForForutgåendeMedlemskapDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.MedlemskapArbeidInntektForutgåendeRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AVKLAR_FORUTGÅENDE_MEDLEMSKAP_KODE
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.lookup.repository.RepositoryProvider


@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(value = AVKLAR_FORUTGÅENDE_MEDLEMSKAP_KODE)
class AvklarPeriodisertForutgåendeMedlemskapLøsning(
    @param:JsonProperty(
        "behovstype",
        required = true,
        defaultValue = AVKLAR_FORUTGÅENDE_MEDLEMSKAP_KODE
    ) val behovstype: AvklaringsbehovKode = AvklaringsbehovKode.`5020`,
    override val løsningerForPerioder: List<PeriodisertManuellVurderingForForutgåendeMedlemskapDto>
) : PeriodisertAvklaringsbehovLøsning<PeriodisertManuellVurderingForForutgåendeMedlemskapDto> {
    override fun løs(repositoryProvider: RepositoryProvider, kontekst: AvklaringsbehovKontekst, gatewayProvider: GatewayProvider): LøsningsResultat {
        return AvklarForutgåendeMedlemskapLøser(repositoryProvider).løs(kontekst, this)
    }

    override fun hentTidligereLøstePerioder(
        behandlingId: BehandlingId,
        repositoryProvider: RepositoryProvider
    ): Tidslinje<*> {
        val repository = repositoryProvider.provide<MedlemskapArbeidInntektForutgåendeRepository>()
        return repository.hentHvisEksisterer(behandlingId)?.gjeldendeVurderinger() ?: Tidslinje<Unit>()
    }
}