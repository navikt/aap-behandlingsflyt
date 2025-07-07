package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.AvklarHelseinstitusjonLøser
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.flate.HelseinstitusjonVurderingerDto
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AVKLAR_HELSEINSTITUSJON_KODE
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.lookup.repository.RepositoryProvider

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(value = AVKLAR_HELSEINSTITUSJON_KODE)
class AvklarHelseinstitusjonLøsning(
    @param:JsonProperty(
        "helseinstitusjonVurdering",
        required = true
    ) val helseinstitusjonVurdering: HelseinstitusjonVurderingerDto,
    @param:JsonProperty(
        "behovstype",
        required = true,
        defaultValue = AVKLAR_HELSEINSTITUSJON_KODE
    ) val behovstype: AvklaringsbehovKode = AvklaringsbehovKode.`5011`
) :
    AvklaringsbehovLøsning {
    override fun løs(repositoryProvider: RepositoryProvider, kontekst: AvklaringsbehovKontekst): LøsningsResultat {
        return AvklarHelseinstitusjonLøser(repositoryProvider).løs(kontekst, this)
    }
}