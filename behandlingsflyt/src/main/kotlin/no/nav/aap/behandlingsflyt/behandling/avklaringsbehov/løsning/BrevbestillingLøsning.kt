package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.BrevbestillingLøser
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.BESTILL_BREV_KODE
import no.nav.aap.behandlingsflyt.kontrakt.brevbestilling.LøsBrevbestillingDto
import no.nav.aap.lookup.repository.RepositoryProvider

@JsonTypeName(value = BESTILL_BREV_KODE)
class BrevbestillingLøsning(
    @JsonProperty("oppdatertStatusForBestilling", required = true) val oppdatertStatusForBestilling: LøsBrevbestillingDto,
    @JsonProperty(
        "behovstype",
        required = true,
        defaultValue = BESTILL_BREV_KODE
    ) val behovstype: AvklaringsbehovKode = AvklaringsbehovKode.`9002`
) : AvklaringsbehovLøsning {
    override fun løs(repositoryProvider: RepositoryProvider, kontekst: AvklaringsbehovKontekst): LøsningsResultat {
        return BrevbestillingLøser(repositoryProvider).løs(kontekst, this)
    }
}
