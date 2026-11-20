package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.stønadsperiode

/**
 * Hvorfor kravet er eller ikke er kant i kant med Arena, og hvilket Arena-oppslag vurderingen bygde på.
 *
 * @param arenaMaksdatoId Raden i `arena_maksdato` som ble brukt. Satt når Arena svarte med en sak.
 */
data class KantIKantMedArena(
    val utfall: KantIKantUtfall,
    val arenaMaksdatoId: Long?,
) {
    val erKantIKant: Boolean get() = utfall == KantIKantUtfall.KANT_I_KANT

    init {
        if (utfall.harArenaSak) {
            requireNotNull(arenaMaksdatoId) { "[Utviklerfeil] $utfall krever peker til Arena-maksdato" }
        } else {
            require(arenaMaksdatoId == null) { "[Utviklerfeil] $utfall kan ikke ha peker til Arena-maksdato" }
        }
    }
}

/**
 * Verdiene lagres i databasen. Legg til nye, aldri endre eller slett.
 */
enum class KantIKantUtfall(val harArenaSak: Boolean) {
    /** Kravet dekker dagen etter maksdato i Arena-saken. */
    KANT_I_KANT(harArenaSak = true),

    /** Et annet krav i saken dekker dagen etter maksdato. */
    ANNET_KRAV_ER_KANT_I_KANT(harArenaSak = true),

    /** Alle krav starter etter dagen etter maksdato. */
    OPPHOLD_ETTER_MAKSDATO(harArenaSak = true),

    /** Arena-saken har ingen maksdato. */
    INGEN_MAKSDATO(harArenaSak = true),

    /** Arena er spurt uten treff. */
    INGEN_ARENASAK(harArenaSak = false),

    /** Arena er ikke spurt i behandlingen. */
    IKKE_VURDERT(harArenaSak = false),
}
