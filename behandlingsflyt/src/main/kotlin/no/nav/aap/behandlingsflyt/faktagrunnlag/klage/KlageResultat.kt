package no.nav.aap.behandlingsflyt.faktagrunnlag.klage

sealed interface KlageResultat

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
    INKONSISTENT_VURDERING
}

enum class ÅrsakTilAvslag {
    IKKE_OVERHOLDT_FORMKRAV
}