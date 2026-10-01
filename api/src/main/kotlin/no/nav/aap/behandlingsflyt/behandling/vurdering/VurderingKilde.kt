package no.nav.aap.behandlingsflyt.behandling.vurdering

import no.nav.aap.behandlingsflyt.ARENA_MIGRERING_BRUKER
import no.nav.aap.behandlingsflyt.SYSTEMBRUKER

enum class VurderingKilde {
    SAKSBEHANDLER,
    AUTOMATISK,
    MIGRERT_FRA_ARENA;

    companion object {
        /**
         * Utledes kun fra identen som er lagret på vurderingen, ikke fra behandlingen.
         * En migreringsbehandling kan også inneholde vurderinger Kelvin har gjort automatisk.
         */
        fun fraIdent(ident: String): VurderingKilde = when (ident) {
            ARENA_MIGRERING_BRUKER.ident -> MIGRERT_FRA_ARENA
            SYSTEMBRUKER.ident -> AUTOMATISK
            else -> SAKSBEHANDLER
        }
    }
}
