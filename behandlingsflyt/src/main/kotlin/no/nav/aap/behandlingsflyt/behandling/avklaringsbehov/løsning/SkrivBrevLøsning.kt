package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.SkrivBrevLøser
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.SKRIV_BREV_KODE
import no.nav.aap.lookup.repository.RepositoryProvider
import java.util.UUID

@JsonTypeName(value = SKRIV_BREV_KODE)
class SkrivBrevLøsning(
    @param:JsonProperty("brevbestillingReferanse", required = true) val brevbestillingReferanse: UUID,
    @param:JsonProperty("handling", required = true) val handling: SkrivBrevAvklaringsbehovLøsning.Handling,
    @param:JsonProperty("behovstype", required = true, defaultValue = SKRIV_BREV_KODE)
    val behovstype: AvklaringsbehovKode = AvklaringsbehovKode.`5050`
) : AvklaringsbehovLøsning {
    override fun løs(repositoryProvider: RepositoryProvider, kontekst: AvklaringsbehovKontekst): LøsningsResultat {
        return SkrivBrevLøser(repositoryProvider).løs(kontekst, this)
    }
}
