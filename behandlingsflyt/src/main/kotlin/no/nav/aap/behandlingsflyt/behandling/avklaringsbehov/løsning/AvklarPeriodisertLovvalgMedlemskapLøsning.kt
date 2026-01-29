package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.AvklarPeriodisertLovvalgMedlemskapLøser
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.PeriodisertManuellVurderingForLovvalgMedlemskapDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.MedlemskapArbeidInntektRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.MedlemskapRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsopptrapping.ArbeidsopptrappingRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AVKLAR_LOVVALG_MEDLEMSKAP_KODE
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.lookup.repository.RepositoryProvider

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(value = AVKLAR_LOVVALG_MEDLEMSKAP_KODE)
class AvklarPeriodisertLovvalgMedlemskapLøsning(
    @param:JsonProperty(
        "behovstype",
        required = true,
        defaultValue = AVKLAR_LOVVALG_MEDLEMSKAP_KODE
    ) val behovstype: AvklaringsbehovKode = AvklaringsbehovKode.`5017`,
    override val løsningerForPerioder: List<PeriodisertManuellVurderingForLovvalgMedlemskapDto>
) : PeriodisertAvklaringsbehovLøsning<PeriodisertManuellVurderingForLovvalgMedlemskapDto> {
    override fun løs(repositoryProvider: RepositoryProvider, kontekst: AvklaringsbehovKontekst, gatewayProvider: GatewayProvider): LøsningsResultat {
        return AvklarPeriodisertLovvalgMedlemskapLøser(repositoryProvider).løs(kontekst, this)
    }

    override fun hentTidligereLøstePerioder(
        behandlingId: BehandlingId,
        repositoryProvider: RepositoryProvider
    ): Tidslinje<*> {
        val repository = repositoryProvider.provide<MedlemskapArbeidInntektRepository>()
        return repository.hentHvisEksisterer(behandlingId)?.gjeldendeVurderinger() ?: Tidslinje<Unit>()
    }
}
