package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.FritakFraMeldepliktLøser
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.flate.FritaksvurderingDto
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.FRITAK_MELDEPLIKT_KODE
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
) : AvklaringsbehovLøsning {

    override fun løs(repositoryProvider: RepositoryProvider, kontekst: AvklaringsbehovKontekst): LøsningsResultat {
        return FritakFraMeldepliktLøser(repositoryProvider).løs(kontekst, this)
    }
}
