package no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov

public sealed interface AnnetRelevantDokument : Melding {
    public val årsakerTilBehandling: List<Vurderingsbehov>
    public val begrunnelse: String?
    public val underkategori: AnnetRelevantDokumentUnderkategori?
}

public enum class AnnetRelevantDokumentUnderkategori {
    ARBEIDSUTPROVING,
    BARNETILLEGG,
    ETABLERING,
    INSTITUSJONSOPPHOLD,
    FENGSEL_VARETEKT,
    KLAGE,
    LAERLING,
    MEDLEMSKAP,
    PARTSINNSYN,
    REFUSJONSKRAV,
    SLUTTAVTALE,
    STUDENTBESTEMMELSEN,
    YRKESSKADE,
    KARAKTERUTSKRIFTER_OG_CV,
    HELSEOPPLYSNINGER,
    ETTERSENDELSE_TIL_KLAGE,
    ETTERSENDELSE_TIL_FEILUTBETALING,
    TILTAKSRAPPORT,
    MELDEKORT
}

@JsonIgnoreProperties(ignoreUnknown = true)
public data class AnnetRelevantDokumentV0(
    public override val årsakerTilBehandling: List<Vurderingsbehov>,
) : AnnetRelevantDokument {
    public override val begrunnelse: String? = null
    public override val underkategori: AnnetRelevantDokumentUnderkategori? = null
}

@JsonIgnoreProperties(ignoreUnknown = true)
public data class AnnetRelevantDokumentV1(
    public override val årsakerTilBehandling: List<Vurderingsbehov>,
    public override val begrunnelse: String,
    public override val underkategori: AnnetRelevantDokumentUnderkategori? = null,
) : AnnetRelevantDokument

