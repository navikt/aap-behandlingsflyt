package no.nav.aap.medlemskap

enum class EØSLandEllerLandMedAvtale(val alpha2: String, val eøs: Boolean = true) {
    BEL("BE"), BGR("BG"), DNK("DK"), EST("EE"), FIN("FI"),
    FRA("FR"), GRC("GR"), IRL("IE"), ISL("IS"), ITA("IT"),
    HRV("HR"), CYP("CY"), LVA("LV"), LIE("LI"), LTU("LT"),
    LUX("LU"), MLT("MT"), NLD("NL"), NOR("NO"), POL("PL"),
    PRT("PT"), ROU("RO"), SVK("SK"), SVN("SI"), ESP("ES"),
    CHE("CH"), SWE("SE"), CZE("CZ"), DEU("DE"), HUN("HU"),
    AUT("AT"), GBR("GB", eøs = false), AUS("AU", eøs = false);

    companion object {
        val gyldigeEØSLand = entries.filter { it.eøs }
    }
}