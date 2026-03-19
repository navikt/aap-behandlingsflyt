package no.nav.aap.behandlingsflyt.test

object FiktivtHelseoppholdNavnGenerator {
    val navnListe = FiktivtNavnGenerator.Navnelager.loadNames("/fiktive_helseopphold_navn.txt")

    fun generer(): String {
        check(navnListe.isNotEmpty()) { "Ingen helseoppholdnavn funnet" }
        return navnListe.random()
    }
}