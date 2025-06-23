package no.nav.aap.behandlingsflyt.faktagrunnlag.klage.fullmektig

import java.time.Instant

data class FullmektigGrunnlag(
    val vurdering: FullmektigVurdering
)

data class FullmektigVurdering(
    val harFullmektig: Boolean,
    val fullmektigIdent: String? = null,
    val fullmektigNavnOgAdresse: NavnOgAdresse? = null,
    val vurdertAv: String,
    val opprettet: Instant? = null
) {
    init {
        require(harFullmektig || (fullmektigIdent == null && fullmektigNavnOgAdresse == null)) {
            "Hvis bruker ikke har fullmektig, må fullmektig-ident og navn og adresse være null"
        }
        require(!harFullmektig || (fullmektigIdent == null) xor (fullmektigNavnOgAdresse == null)) {
            "Hvis bruker har fullmektig, må enten fullmektig-ident eller navn og adresse være satt"
        }
    }
}

data class NavnOgAdresse(
    val navn: String,
    val adresse: Adresse
)

data class Adresse(
    val postnummer: String,
    val poststed: String,
    val adresselinje1: String,
    val adresselinje2: String? = null,
    val adresselinje3: String? = null,
    val landkode: String
)