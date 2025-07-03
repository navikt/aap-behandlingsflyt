package no.nav.aap.behandlingsflyt.faktagrunnlag.klage.fullmektig

import java.time.Instant

data class FullmektigGrunnlag(
    val vurdering: FullmektigVurdering
)

data class FullmektigVurdering(
    val harFullmektig: Boolean,
    val fullmektigIdent: IdentMedType? = null,
    val fullmektigNavnOgAdresse: NavnOgAdresse? = null,
    val vurdertAv: String,
    val opprettet: Instant? = null
) {
    init {
        require(harFullmektig || (fullmektigIdent == null && fullmektigNavnOgAdresse == null)) {
            "Hvis bruker ikke har fullmektig, må fullmektig-ident og navn og adresse være null"
        }
        require(
            !harFullmektig || (fullmektigIdent?.type in listOf(
                IdentType.FNR_DNR,
                IdentType.ORGNR
            )) xor (fullmektigNavnOgAdresse != null)
        ) {
            "Hvis bruker har fullmektig, må enten fullmektig-ident være av type fødselsesnummer, eller navn og adresse være satt"
        }
    }
}

data class IdentMedType(
    val ident: String,
    val type: IdentType
)

enum class IdentType {
    ORGNR,
    UTL_ORGNR,
    FNR_DNR,
}

data class NavnOgAdresse(
    val navn: String,
    val adresse: Adresse
)

data class Adresse(
    val landkode: String,
    val adresselinje2: String? = null,
    val adresselinje3: String? = null,
    val postnummer: String? = null,
    val poststed: String? = null,
    val adresselinje1: String,
) {
    init {
        require(
            (landkode == "NOR" && postnummer != null && poststed != null) ||
                    (landkode != "NOR" && postnummer == null && poststed == null)
        ) {
            "Postnummer og poststed må være satt for norsk adresse, men skal ikke være satt for utenlandsk adresse"
        }
    }
}