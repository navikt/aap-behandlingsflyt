package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.KvalitetssikrerLøser
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.vedtak.TotrinnsVurdering
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.KVALITETSSIKRING_KODE
import no.nav.aap.lookup.repository.RepositoryProvider

@JsonTypeName(value = KVALITETSSIKRING_KODE)
class KvalitetssikringLøsning(
    @param:JsonProperty("vurderinger", required = true) val vurderinger: List<TotrinnsVurdering>,
    @param:JsonProperty(
        "behovstype",
        required = true,
        defaultValue = KVALITETSSIKRING_KODE
    ) val behovstype: AvklaringsbehovKode = AvklaringsbehovKode.`5097`
) : AvklaringsbehovLøsning {
    override fun løs(repositoryProvider: RepositoryProvider, kontekst: AvklaringsbehovKontekst): LøsningsResultat {
        return KvalitetssikrerLøser(repositoryProvider).løs(kontekst, this)
    }
}
