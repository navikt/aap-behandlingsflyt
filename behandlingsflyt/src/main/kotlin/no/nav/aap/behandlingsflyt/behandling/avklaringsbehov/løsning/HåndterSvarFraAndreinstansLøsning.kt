package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.HåndterSvarFraAndreinstansLøser
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.Hjemmel
import no.nav.aap.behandlingsflyt.faktagrunnlag.svarfraandreinstans.SvarFraAndreinstansKonsekvens
import no.nav.aap.behandlingsflyt.faktagrunnlag.svarfraandreinstans.SvarFraAndreinstansVurdering
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.HÅNDTER_SVAR_FRA_ANDREINSTANS_KODE
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.komponenter.httpklient.exception.IkkeTillattException
import no.nav.aap.lookup.repository.RepositoryProvider

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(value = HÅNDTER_SVAR_FRA_ANDREINSTANS_KODE)
class HåndterSvarFraAndreinstansLøsning(
    @param:JsonProperty("svarFraAndreinstansVurdering", required = true)
    val svarFraAndreinstansVurdering: HåndterSvarFraAndreinstansLøsningDto,
    @param:JsonProperty(
        "behovstype",
        required = true,
        defaultValue = HÅNDTER_SVAR_FRA_ANDREINSTANS_KODE
    ) val behovstype: AvklaringsbehovKode = AvklaringsbehovKode.`6008`
) : AvklaringsbehovLøsning {
    override fun løs(repositoryProvider: RepositoryProvider, kontekst: AvklaringsbehovKontekst): LøsningsResultat {
        // TODO: Støtt konsekvens 'BEHANDLE_PÅ_NYTT' i steget
        if (svarFraAndreinstansVurdering.konsekvens == SvarFraAndreinstansKonsekvens.BEHANDLE_PÅ_NYTT) {
            throw IkkeTillattException("Opprettelse av ny klagebehandling er enda ikke implementert")
        }
        
        return HåndterSvarFraAndreinstansLøser(repositoryProvider).løs(kontekst, this)
    }
}

data class HåndterSvarFraAndreinstansLøsningDto(
    val begrunnelse: String,
    val konsekvens: SvarFraAndreinstansKonsekvens,
    val vilkårSomOmgjøres: List<Hjemmel>,

    ) {
    fun tilVurdering(vurdertAv: Bruker) = SvarFraAndreinstansVurdering(
        vurdertAv = vurdertAv.ident,
        begrunnelse = begrunnelse,
        konsekvens = konsekvens,
        vilkårSomOmgjøres = vilkårSomOmgjøres
    )
}
