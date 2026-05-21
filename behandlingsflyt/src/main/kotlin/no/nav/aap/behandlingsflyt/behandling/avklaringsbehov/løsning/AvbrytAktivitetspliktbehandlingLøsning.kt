package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.AvbrytAktivitetspliktbehandlingLøser
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.avbrytaktivitetspliktbehandling.AvbrytAktivitetspliktbehandlingLøsningDto
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AVBRYT_AKTIVITETSPLIKTBEHANDING_KODE
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

@JsonTypeName(value = AVBRYT_AKTIVITETSPLIKTBEHANDING_KODE)
class AvbrytAktivitetspliktbehandlingLøsning(
    @param:JsonProperty("behovstype", required = true, defaultValue = AVBRYT_AKTIVITETSPLIKTBEHANDING_KODE)
    val behovstype: AvklaringsbehovKode = AvklaringsbehovKode.`4301`,
    val vurdering: AvbrytAktivitetspliktbehandlingLøsningDto
) : EnkeltAvklaringsbehovLøsning {
    override fun løs(repositoryProvider: RepositoryProvider, kontekst: AvklaringsbehovKontekst, gatewayProvider: GatewayProvider): LøsningsResultat {
        return AvbrytAktivitetspliktbehandlingLøser(repositoryProvider).løs(kontekst, this)
    }
}