package no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap

data class Medlemskap(val unntak: List<Unntak>)

data class Unntak(
    val status: String,
    val statusaarsak: String?,
    val medlem: Boolean,
    val grunnlag: String,
    val lovvalg: String,
    val helsedel: Boolean,
    val lovvalgsland: String?,
)

data class MedlemskapDataIntern(
    val unntakId: Number,
    val ident: String,
    val fraOgMed: String,
    val tilOgMed: String,
    val status: String,
    val statusaarsak: String?,
    val medlem: Boolean,
    val grunnlag: String,
    val lovvalg: String,
    val helsedel: Boolean,
    val lovvalgsland: String?,
    val kilde: KildesystemMedl?
)

data class KildesystemMedl(
    val kilde: KildesystemKode,
    val kildeNavn: String
)

enum class KildesystemKode{
    APPBRK,
    AVGSYS,
    E500,
    INFOTR,
    LAANEKASSEN,
    MEDL,
    PP01,
    srvgosys,
    srvmelosys,
    TP,
    TPS,
}