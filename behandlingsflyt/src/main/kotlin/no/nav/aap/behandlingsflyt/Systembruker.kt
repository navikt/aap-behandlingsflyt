package no.nav.aap.behandlingsflyt

import no.nav.aap.komponenter.verdityper.Bruker

val SYSTEMBRUKER = Bruker("Kelvin")

/**
 * Brukes for vurderinger som er oversatt fra Arena under migrering. Identen lagres i databasen
 * og må ikke endres. Vurderinger Kelvin selv utleder (f.eks. fra registerdata) skal bruke [SYSTEMBRUKER],
 * også i migreringsbehandlinger.
 */
val ARENA_MIGRERING_BRUKER = Bruker("ArenaMigrering")

/**
 * Sann for alle vurderinger som ikke er gjort av en saksbehandler, inkludert migrerte vurderinger fra Arena.
 */
fun Bruker.erSystembruker(): Boolean = this == SYSTEMBRUKER || this == ARENA_MIGRERING_BRUKER
