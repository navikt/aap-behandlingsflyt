package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.UtenlandskVidereføringLøser
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AVKLAR_UTENLANDSK_MEDLEMSKAP_KODE
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.lookup.repository.RepositoryProvider

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(value = AVKLAR_UTENLANDSK_MEDLEMSKAP_KODE)
class UtenlandskVidereføringLøsning(
    @JsonProperty(
        "behovstype", required = true, defaultValue = AVKLAR_UTENLANDSK_MEDLEMSKAP_KODE
    ) val behovstype: AvklaringsbehovKode = AvklaringsbehovKode.`5019`
) : AvklaringsbehovLøsning {
    override fun løs(repositoryProvider: RepositoryProvider, kontekst: AvklaringsbehovKontekst): LøsningsResultat {
        return UtenlandskVidereføringLøser(repositoryProvider).løs(kontekst, this)
    }
}
