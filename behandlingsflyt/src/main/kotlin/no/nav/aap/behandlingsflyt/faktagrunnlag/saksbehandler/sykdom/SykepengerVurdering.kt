package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom

import no.nav.aap.verdityper.dokument.JournalpostId

data class SykepengerVurdering(
    val begrunnelse: String,
    val dokumenterBruktIVurdering: List<JournalpostId>,
    val harRettPå: Boolean,
    val grunn: SykepengerGrunn? = null
)

enum class SykepengerGrunn {
    ANNEN_SYKDOM_INNEN_SEKS_MND,
    SAMME_SYKDOM_INNEN_ETT_AAR,
    SYKEPENGER_IGJEN_ARBEIDSUFOR,
    SYKEPENGER_FORTSATT_ARBEIDSUFOR,
    FORELDREPENGER_INNEN_SEKS_MND;
}