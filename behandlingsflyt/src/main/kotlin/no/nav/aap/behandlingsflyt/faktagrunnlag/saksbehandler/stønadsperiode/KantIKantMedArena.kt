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
 *
 * Overgangen fra Arena går fra dagen etter maksdato til og med første hverdag etter maksdato. Maksdato fredag
 * og krav fra mandag er derfor kant i kant. Helligdager regnes som hverdager.
 */
enum class KantIKantUtfall(val harArenaSak: Boolean) {
    /** Kravet er det tidligste som dekker overgangen fra Arena. */
    KANT_I_KANT(harArenaSak = true),

    /** Et annet krav i saken dekker overgangen fra Arena før dette kravet. */
    ANNET_KRAV_ER_KANT_I_KANT(harArenaSak = true),

    /** Ingen krav dekker overgangen fra Arena: alle starter etter første hverdag etter maksdato. */
    OPPHOLD_ETTER_MAKSDATO(harArenaSak = true),

    /** Arena-saken har ingen maksdato. */
    INGEN_MAKSDATO(harArenaSak = true),

    /** Arena er spurt uten treff. */
    INGEN_ARENASAK(harArenaSak = false),

    /** Arena er ikke spurt i behandlingen. */
    IKKE_VURDERT(harArenaSak = false),
}
