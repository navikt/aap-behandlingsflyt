package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.AvklarOverstyrtForutgåendeMedlemskapLøser
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.ManuellVurderingForForutgåendeMedlemskap
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.PeriodisertManuellVurderingForForutgåendeMedlemskapDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.MedlemskapArbeidInntektForutgåendeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.PeriodisertVurdering
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.MANUELL_OVERSTYRING_MEDLEMSKAP
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.lookup.repository.RepositoryProvider


@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(value = MANUELL_OVERSTYRING_MEDLEMSKAP)
class AvklarPeriodisertOverstyrtForutgåendeMedlemskapLøsning(
    @param:JsonProperty(
        "behovstype",
        required = true,
        defaultValue = MANUELL_OVERSTYRING_MEDLEMSKAP
    ) val behovstype: AvklaringsbehovKode = AvklaringsbehovKode.`5022`,
    override val løsningerForPerioder: List<PeriodisertManuellVurderingForForutgåendeMedlemskapDto>
) : PeriodisertAvklaringsbehovLøsning<PeriodisertManuellVurderingForForutgåendeMedlemskapDto>, LøsningMedPeriodiserteVurderinger {
    override fun løs(repositoryProvider: RepositoryProvider, kontekst: AvklaringsbehovKontekst, gatewayProvider: GatewayProvider): LøsningsResultat {
        return AvklarOverstyrtForutgåendeMedlemskapLøser(repositoryProvider).løs(kontekst, this)
    }

    override fun hentLagredeLøstePerioder(
        behandlingId: BehandlingId,
        repositoryProvider: RepositoryProvider,
        gatewayProvider: GatewayProvider
    ): Tidslinje<*> {
        val repository = repositoryProvider.provide<MedlemskapArbeidInntektForutgåendeRepository>()
        return repository.hentHvisEksisterer(behandlingId)?.gjeldendeVurderinger() ?: Tidslinje<Unit>()
    }

    override fun hentVurderinger(
        behandlingId: BehandlingId,
        repositoryProvider: RepositoryProvider
    ): List<PeriodisertVurdering> {
        val repository = repositoryProvider.provide<MedlemskapArbeidInntektForutgåendeRepository>()
        return repository.hentHvisEksisterer(behandlingId)?.vurderinger.orEmpty()
    }

    override fun somVurderinger(bruker: Bruker, behandlingId: BehandlingId): List<ManuellVurderingForForutgåendeMedlemskap> {
        return løsningerForPerioder.map { it.toManuellVurderingForForutgåendeMedlemskap(overstyrt = true, bruker, behandlingId) }
    }
}