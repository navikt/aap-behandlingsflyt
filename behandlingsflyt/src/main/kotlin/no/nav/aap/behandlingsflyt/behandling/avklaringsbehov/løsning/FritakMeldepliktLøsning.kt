package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.FritakFraMeldepliktLøser
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.MeldepliktRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.flate.FritaksvurderingDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.flate.PeriodisertFritaksvurderingDto
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.FRITAK_MELDEPLIKT_KODE
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.lookup.repository.RepositoryProvider

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(value = FRITAK_MELDEPLIKT_KODE)
class FritakMeldepliktLøsning(
    @param:JsonProperty("fritaksvurderinger", required = true) val fritaksvurderinger: List<FritaksvurderingDto>,
    @param:JsonProperty(
        "behovstype",
        required = true,
        defaultValue = FRITAK_MELDEPLIKT_KODE
    ) val behovstype: AvklaringsbehovKode = AvklaringsbehovKode.`5005`
) : EnkeltAvklaringsbehovLøsning {
    val periodisertFritaksvurdering = fritaksvurderinger.map {
        PeriodisertFritaksvurderingDto(
            begrunnelse = it.begrunnelse,
            fom = it.fraDato,
            tom = null,
            harFritak = it.harFritak
        )
    }

    val løsning = PeriodisertFritakMeldepliktLøsning(
        behovstype = behovstype,
        løsningerForPerioder = periodisertFritaksvurdering
    )

    override fun løs(repositoryProvider: RepositoryProvider, kontekst: AvklaringsbehovKontekst, gatewayProvider: GatewayProvider): LøsningsResultat {
        return FritakFraMeldepliktLøser(repositoryProvider).løs(kontekst, løsning)
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(value = FRITAK_MELDEPLIKT_KODE)
class PeriodisertFritakMeldepliktLøsning(
    @param:JsonProperty(
        "behovstype",
        required = true,
        defaultValue = FRITAK_MELDEPLIKT_KODE
    ) val behovstype: AvklaringsbehovKode = AvklaringsbehovKode.`5005`,
    override val løsningerForPerioder: List<PeriodisertFritaksvurderingDto>
) : PeriodisertAvklaringsbehovLøsning<PeriodisertFritaksvurderingDto> {

    override fun løs(repositoryProvider: RepositoryProvider, kontekst: AvklaringsbehovKontekst, gatewayProvider: GatewayProvider): LøsningsResultat {
        return FritakFraMeldepliktLøser(repositoryProvider).løs(kontekst, this)
    }

    override fun hentTidligereLøstePerioder(
        behandlingId: BehandlingId,
        repositoryProvider: RepositoryProvider
    ): Tidslinje<*> {
        val repository = repositoryProvider.provide<MeldepliktRepository>()
        return repository.hentHvisEksisterer(behandlingId)?.gjeldendeVurderinger() ?: Tidslinje<Unit>()
    }
}