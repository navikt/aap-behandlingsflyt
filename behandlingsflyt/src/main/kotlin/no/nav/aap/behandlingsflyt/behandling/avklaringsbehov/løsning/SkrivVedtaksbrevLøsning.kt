package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.SkrivVedtaksbrevLøser
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.SKRIV_VEDTAKSBREV_KODE
import no.nav.aap.brev.kontrakt.MottakerDto
import no.nav.aap.lookup.repository.RepositoryProvider
import java.util.*

@JsonTypeName(value = SKRIV_VEDTAKSBREV_KODE)
class SkrivVedtaksbrevLøsning(
    @param:JsonProperty("brevbestillingReferanse", required = true) val brevbestillingReferanse: UUID,
    @param:JsonProperty("handling", required = true) val handling: SkrivBrevAvklaringsbehovLøsning.Handling,
    @param:JsonProperty("mottakere") val mottakere: List<MottakerDto> = emptyList(),
    @param:JsonProperty("behovstype", required = true, defaultValue = SKRIV_VEDTAKSBREV_KODE)
    val behovstype: AvklaringsbehovKode = AvklaringsbehovKode.`5051`
) : AvklaringsbehovLøsning {
    override fun løs(repositoryProvider: RepositoryProvider, kontekst: AvklaringsbehovKontekst): LøsningsResultat {
        return SkrivVedtaksbrevLøser(repositoryProvider).løs(kontekst, this)
    }
}
