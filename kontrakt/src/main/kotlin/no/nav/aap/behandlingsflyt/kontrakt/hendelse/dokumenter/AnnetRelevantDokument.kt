package no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov

public sealed interface AnnetRelevantDokument : Melding {
    public val årsakerTilBehandling: List<Vurderingsbehov>
    public val begrunnelse: String?
}

@JsonIgnoreProperties(ignoreUnknown = true)
public data class AnnetRelevantDokumentV0(
    public override val årsakerTilBehandling: List<Vurderingsbehov>,
) : AnnetRelevantDokument {
    public override val begrunnelse: String? = null
}

@JsonIgnoreProperties(ignoreUnknown = true)
public data class AnnetRelevantDokumentV1(
    public override val årsakerTilBehandling: List<Vurderingsbehov>,
    public override val begrunnelse: String
) : AnnetRelevantDokument