package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.AvklarForutgåendeMedlemskapLøser
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.ManuellVurderingForForutgåendeMedlemskapDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.PeriodisertManuellVurderingForForutgåendeMedlemskapDto
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AVKLAR_FORUTGÅENDE_MEDLEMSKAP_KODE
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(value = AVKLAR_FORUTGÅENDE_MEDLEMSKAP_KODE)
class AvklarForutgåendeMedlemskapLøsning(
    @param:JsonProperty(
        "manuellVurderingForForutgåendeMedlemskap",
        required = true
    ) val manuellVurderingForForutgåendeMedlemskap: ManuellVurderingForForutgåendeMedlemskapDto,
    @param:JsonProperty(
        "behovstype",
        required = true,
        defaultValue = AVKLAR_FORUTGÅENDE_MEDLEMSKAP_KODE
    ) val behovstype: AvklaringsbehovKode = AvklaringsbehovKode.`5020`
) : EnkeltAvklaringsbehovLøsning {
    override fun løs(repositoryProvider: RepositoryProvider, kontekst: AvklaringsbehovKontekst, gatewayProvider: GatewayProvider): LøsningsResultat {
        // TODO denne løsningen utgår når vi er helt over på periodisert løsning
        val sakRepository: SakRepository = repositoryProvider.provide()
        val sak = sakRepository.hent(kontekst.kontekst.sakId)

        val løsning = AvklarPeriodisertForutgåendeMedlemskapLøsning(
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

        return AvklarForutgåendeMedlemskapLøser(repositoryProvider).løs(kontekst, løsning)
    }
}

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
}