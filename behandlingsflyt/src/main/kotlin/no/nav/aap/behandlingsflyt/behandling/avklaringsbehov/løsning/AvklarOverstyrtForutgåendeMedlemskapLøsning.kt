package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.AvklarOverstyrtForutgåendeMedlemskapLøser
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.ManuellVurderingForForutgåendeMedlemskapDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.PeriodisertManuellVurderingForForutgåendeMedlemskapDto
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.MANUELL_OVERSTYRING_MEDLEMSKAP
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(value = MANUELL_OVERSTYRING_MEDLEMSKAP)
class AvklarOverstyrtForutgåendeMedlemskapLøsning(
    @param:JsonProperty(
        "manuellVurderingForForutgåendeMedlemskap",
        required = true
    ) val manuellVurderingForForutgåendeMedlemskap: ManuellVurderingForForutgåendeMedlemskapDto,
    @param:JsonProperty(
        "behovstype",
        required = true,
        defaultValue = MANUELL_OVERSTYRING_MEDLEMSKAP
    ) val behovstype: AvklaringsbehovKode = AvklaringsbehovKode.`5022`
) : EnkeltAvklaringsbehovLøsning {
    override fun løs(repositoryProvider: RepositoryProvider, kontekst: AvklaringsbehovKontekst, gatewayProvider: GatewayProvider): LøsningsResultat {
        // TODO denne løsningen utgår når vi er helt over på periodisert løsning
        val sakRepository: SakRepository = repositoryProvider.provide()
        val sak = sakRepository.hent(kontekst.kontekst.sakId)

        val løsning = AvklarPeriodisertOverstyrtForutgåendeMedlemskapLøsning(
            løsningerForPerioder = listOf(
                PeriodisertManuellVurderingForForutgåendeMedlemskapDto(
                    tom = null,
                    fom = sak.rettighetsperiode.fom,
                    begrunnelse = manuellVurderingForForutgåendeMedlemskap.begrunnelse,
                    harForutgåendeMedlemskap = manuellVurderingForForutgåendeMedlemskap.harForutgåendeMedlemskap,
                    varMedlemMedNedsattArbeidsevne = manuellVurderingForForutgåendeMedlemskap.varMedlemMedNedsattArbeidsevne,
                    medlemMedUnntakAvMaksFemAar = manuellVurderingForForutgåendeMedlemskap.medlemMedUnntakAvMaksFemAar
                ))
        )

        return AvklarOverstyrtForutgåendeMedlemskapLøser(repositoryProvider).løs(kontekst, løsning)
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(value = MANUELL_OVERSTYRING_MEDLEMSKAP)
class AvklarPeriodisertOverstyrtForutgåendeMedlemskapLøsning(
    @param:JsonProperty(
        "behovstype",
        required = true,
        defaultValue = MANUELL_OVERSTYRING_MEDLEMSKAP
    ) val behovstype: AvklaringsbehovKode = AvklaringsbehovKode.`5022`,
    override val løsningerForPerioder: List<PeriodisertManuellVurderingForForutgåendeMedlemskapDto>
) : PeriodisertAvklaringsbehovLøsning<PeriodisertManuellVurderingForForutgåendeMedlemskapDto> {
    override fun løs(repositoryProvider: RepositoryProvider, kontekst: AvklaringsbehovKontekst, gatewayProvider: GatewayProvider): LøsningsResultat {
        return AvklarOverstyrtForutgåendeMedlemskapLøser(repositoryProvider).løs(kontekst, this)
    }
}