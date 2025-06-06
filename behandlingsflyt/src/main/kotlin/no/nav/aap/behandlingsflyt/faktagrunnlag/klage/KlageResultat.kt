package no.nav.aap.behandlingsflyt.faktagrunnlag.klage

sealed interface KlageResultat {
    fun hjemlerSomSkalOpprettholdes(): List<Hjemmel> {
        return when (this) {
            is Opprettholdes -> vilkårSomSkalOpprettholdes
            is DelvisOmgjøres -> vilkårSomSkalOpprettholdes
            else -> emptyList()
        }
    }
    fun hjemlerSomSkalOmgjøres(): List<Hjemmel> {
        return when (this) {
            is Omgjøres -> vilkårSomSkalOmgjøres
            is DelvisOmgjøres -> vilkårSomSkalOmgjøres
            else -> emptyList()
        }
    }
}

data class Opprettholdes(
    val vilkårSomSkalOpprettholdes: List<Hjemmel>
) : KlageResultat

data class Omgjøres(
    val vilkårSomSkalOmgjøres: List<Hjemmel>,
) : KlageResultat

data class DelvisOmgjøres(
    val vilkårSomSkalOmgjøres: List<Hjemmel>,
    val vilkårSomSkalOpprettholdes: List<Hjemmel>
) : KlageResultat

data class Avslått(
    val årsak: ÅrsakTilAvslag
) : KlageResultat

data class Ufullstendig(
    val årsak: ÅrsakTilUfullstendigResultat
) : KlageResultat

enum class ÅrsakTilUfullstendigResultat {
    MANGLER_VURDERING,
    INKONSISTENT_VURDERING,
    VENTER_PÅ_SVAR_FRA_BRUKER
}

enum class ÅrsakTilAvslag {
    IKKE_OVERHOLDT_FORMKRAV,
    IKKE_OVERHOLDT_FRIST
}