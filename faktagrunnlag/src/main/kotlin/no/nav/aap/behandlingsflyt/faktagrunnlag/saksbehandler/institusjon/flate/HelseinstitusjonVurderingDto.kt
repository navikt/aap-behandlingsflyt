import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.flate.HelseinstitusjonVurdering
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.verdityper.dokument.JournalpostId
import java.time.LocalDate

data class HelseinstitusjonVurderingDto(
    val dokumenterBruktIVurdering: List<JournalpostId>,
    val begrunnelse: String,
    val faarFriKostOgLosji: Boolean,
    val forsoergerEktefelle: Boolean? = null,
    val harFasteUtgifter: Boolean? = null,
) {

    fun tilDomeneobjekt() = HelseinstitusjonVurdering(
        dokumenterBruktIVurdering = listOf(),
        begrunnelse = begrunnelse,
        faarFriKostOgLosji = faarFriKostOgLosji,
        forsoergerEktefelle = forsoergerEktefelle,
        harFasteUtgifter = harFasteUtgifter,
        periode = Periode(LocalDate.now(), LocalDate.now())
    )

    companion object {
        fun toDto(helseinstitusjonVurdering: HelseinstitusjonVurdering?) = if (helseinstitusjonVurdering != null) HelseinstitusjonVurderingDto(
            dokumenterBruktIVurdering = helseinstitusjonVurdering.dokumenterBruktIVurdering,
            begrunnelse = helseinstitusjonVurdering.begrunnelse,
            faarFriKostOgLosji = helseinstitusjonVurdering.faarFriKostOgLosji,
            forsoergerEktefelle = helseinstitusjonVurdering.forsoergerEktefelle,
            harFasteUtgifter = helseinstitusjonVurdering.harFasteUtgifter,
        ) else null
    }
}