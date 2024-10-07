import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.flate.HelseinstitusjonVurdering
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.verdityper.dokument.JournalpostId

data class HelseinstitusjonVurderingDto(
    val dokumenterBruktIVurdering: List<JournalpostId>,
    val begrunnelse: String,
    val faarFriKostOgLosji: Boolean,
    val forsoergerEktefelle: Boolean? = null,
    val harFasteUtgifter: Boolean? = null,
    val periode: Periode,
) {

    fun tilDomeneobjekt() = HelseinstitusjonVurdering(
        dokumenterBruktIVurdering = listOf(),
        begrunnelse = begrunnelse,
        faarFriKostOgLosji = faarFriKostOgLosji,
        forsoergerEktefelle = forsoergerEktefelle,
        harFasteUtgifter = harFasteUtgifter,
        periode = periode
    )

    companion object {
        fun toDto(helseinstitusjonVurdering: HelseinstitusjonVurdering?) = if (helseinstitusjonVurdering != null) HelseinstitusjonVurderingDto(
            dokumenterBruktIVurdering = helseinstitusjonVurdering.dokumenterBruktIVurdering,
            begrunnelse = helseinstitusjonVurdering.begrunnelse,
            faarFriKostOgLosji = helseinstitusjonVurdering.faarFriKostOgLosji,
            forsoergerEktefelle = helseinstitusjonVurdering.forsoergerEktefelle,
            harFasteUtgifter = helseinstitusjonVurdering.harFasteUtgifter,
            periode = helseinstitusjonVurdering.periode
        ) else null
    }
}