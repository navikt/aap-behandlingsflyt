package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.SykdomsvurderingForBrevLøser
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.SKRIV_SYKDOMSVURDERING_BREV_KODE
import no.nav.aap.lookup.repository.RepositoryProvider

@JsonTypeName(value = SKRIV_SYKDOMSVURDERING_BREV_KODE)
class SykdomsvurderingForBrevLøsning(
    @param:JsonProperty("behovstype", required = true, defaultValue = SKRIV_SYKDOMSVURDERING_BREV_KODE)
    val behovstype: AvklaringsbehovKode = AvklaringsbehovKode.`5053`,
    @param:JsonProperty("vurdering", required = true) val vurdering: String,
) : AvklaringsbehovLøsning {
    override fun løs(repositoryProvider: RepositoryProvider, kontekst: AvklaringsbehovKontekst): LøsningsResultat {
        return SykdomsvurderingForBrevLøser(repositoryProvider).løs(kontekst, this)
    }
}
