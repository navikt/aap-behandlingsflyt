package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.Effektuer11_7Løser
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.EFFEKTUER_11_7_KODE
import no.nav.aap.lookup.repository.RepositoryProvider

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(value = EFFEKTUER_11_7_KODE)
class Effektuer11_7Løsning(
    @param:JsonProperty("begrunnelse", required = true) val begrunnelse: String,
    @param:JsonProperty("behovstype", required = true, defaultValue = EFFEKTUER_11_7_KODE)
    val behovstype: AvklaringsbehovKode = AvklaringsbehovKode.`5015`
) : AvklaringsbehovLøsning {
    override fun løs(repositoryProvider: RepositoryProvider, kontekst: AvklaringsbehovKontekst): LøsningsResultat {
        return Effektuer11_7Løser(repositoryProvider).løs(kontekst, this)
    }
}