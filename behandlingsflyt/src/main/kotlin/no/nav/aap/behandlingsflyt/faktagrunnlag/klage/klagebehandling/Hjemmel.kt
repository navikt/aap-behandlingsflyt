package no.nav.aap.behandlingsflyt.faktagrunnlag.klage.klagebehandling

// TODO: Fyll på, evt. se om vi skal bruke Vilkårtype
enum class Hjemmel(val hjemmel: String) {
    FOLKETRYGDLOVEN_11_5("§ 11-5"),
    FOLKETRYGDLOVEN_11_6("§ 11-6");
    
    companion object {
        fun fraHjemmel(hjemmel: String): Hjemmel? {
            return entries.firstOrNull { it.hjemmel == hjemmel }
        }
    }
}