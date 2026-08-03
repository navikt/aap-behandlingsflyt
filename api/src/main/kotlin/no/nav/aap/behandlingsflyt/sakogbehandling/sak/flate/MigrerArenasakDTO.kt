package no.nav.aap.behandlingsflyt.sakogbehandling.sak.flate

import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.aap.tilgang.plugin.kontrakt.Personreferanse

data class MigrerArenasakDTO(
    @param:JsonProperty(value = "saksnummerArena", required = true) val saksnummerArena: String,
    @param:JsonProperty(value = "ident", required = true) val ident: String,
): Personreferanse {
    override fun hentPersonreferanse(): String {
        return ident
    }
}
