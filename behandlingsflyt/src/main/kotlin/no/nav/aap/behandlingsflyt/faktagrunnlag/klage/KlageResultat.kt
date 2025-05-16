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
    val avslagsÅrsak: AvslagsÅrsak
) : KlageResultat


enum class AvslagsÅrsak {
    IKKE_OVERHOLDT_FORMKRAV
}