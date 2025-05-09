package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.SattPåVentLøser
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.MANUELT_SATT_PÅ_VENT_KODE
import no.nav.aap.lookup.repository.RepositoryProvider

@JsonTypeName(value = MANUELT_SATT_PÅ_VENT_KODE)
class SattPåVentLøsning(
    @JsonProperty(
        "behovstype",
        required = true,
        defaultValue = MANUELT_SATT_PÅ_VENT_KODE
    ) val behovstype: AvklaringsbehovKode = AvklaringsbehovKode.`9001`
) : AvklaringsbehovLøsning {
    override fun løs(repositoryProvider: RepositoryProvider, kontekst: AvklaringsbehovKontekst): LøsningsResultat {
        return SattPåVentLøser(repositoryProvider).løs(kontekst, this)
    }
}