package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.ForeslåVedtakLøser
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.FORESLÅ_VEDTAK_KODE
import no.nav.aap.lookup.repository.RepositoryProvider

@JsonTypeName(value = FORESLÅ_VEDTAK_KODE)
class ForeslåVedtakLøsning(
    @JsonProperty(
        "behovstype",
        required = true,
        defaultValue = FORESLÅ_VEDTAK_KODE
    ) val behovstype: AvklaringsbehovKode = AvklaringsbehovKode.`5098`
) : AvklaringsbehovLøsning {
    override fun løs(repositoryProvider: RepositoryProvider, kontekst: AvklaringsbehovKontekst): LøsningsResultat {
        return ForeslåVedtakLøser(repositoryProvider).løs(kontekst, this)
    }
}
