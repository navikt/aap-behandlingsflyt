package no.nav.aap.misc.inntekt

data class AndreYtelserSøknad(
    val ekstraLønn: Boolean?,
    val afpKilder: String? = null,
    val stønad: List<AndreUtbetalingerYtelser>?
)

enum class AndreUtbetalingerYtelser {
    ØKONOMISK_SOSIALHJELP,
    OMSORGSSTØNAD,
    INTRODUKSJONSSTØNAD,
    KVALIFISERINGSSTØNAD,
    GODGJØRELSE_ELLER_LØNN_FRA_VERV,
    YTELSE_FRA_UTENLANDSKE_TRYGDEMYNDIGHETER,
    AFP,
    STIPEND_FRA_LÅNEKASSEN, //Sykestipend
    LÅN_FRA_LÅNEKASSEN,
    INGEN_AV_DISSE;
}