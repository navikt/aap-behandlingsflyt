package no.nav.aap.behandlingsflyt.behandling.vilkår.medlemskap

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.aordning.Virksomhet
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.KildesystemMedl
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.AdresseType
import no.nav.aap.komponenter.type.Periode
import java.time.LocalDate
import java.time.LocalDateTime

data class KanBehandlesAutomatiskVurdering(
    val kanBehandlesAutomatisk: Boolean,
    val tilhørighetVurdering: List<TilhørighetVurdering>
)

data class TilhørighetVurdering(
    val kilde: List<Kilde>,
    val indikasjon: Indikasjon,
    val opplysning: String,
    val resultat: Boolean,
    val vurdertPeriode: Periode,
    val vedtakImedlGrunnlag: List<VedtakIMEDLGrunnlag>? = null,
    val mottarSykepengerGrunnlag: List<MottarSykepengerGrunnlag>? = null,
    val arbeidInntektINorgeGrunnlag: List<ArbeidInntektINorgeGrunnlag>? = null,
    val manglerStatsborgerskapGrunnlag: List<ManglerStatsborgerskapGrunnlag>? = null,
    val oppgittJobbetIUtlandGrunnlag: List<OppgittJobbetIUtlandGrunnlag>? = null,
    val oppgittUtenlandsOppholdGrunnlag: List<OppgittUtenlandsOppholdGrunnlag>? = null,
    val utenlandsAddresserGrunnlag: List<UtenlandsAdresseGrunnlag>? = null,
)

data class VedtakIMEDLGrunnlag(
    val periode: Periode,
    val lovvalgsland: String?,
    val grunnlag: String,
    val kilde: KildesystemMedl?
)

data class ArbeidInntektINorgeGrunnlag(
    val virksomhetId: String,
    val virksomhetNavn: String?,
    val beloep: Double,
    val periode: Periode,
)

data class MottarSykepengerGrunnlag(
    val identifikator: String,
    val inntektType: String?,
    val periode: Periode,
)

data class ManglerStatsborgerskapGrunnlag(
    val land: String,
    val gyldigFraOgMed: LocalDate? = null,
    val gyldigTilOgMed: LocalDate? = null,
)

data class OppgittJobbetIUtlandGrunnlag(
    val land: String?,
    val tilDato: LocalDate?,
    val fraDato: LocalDate?,
)

data class OppgittUtenlandsOppholdGrunnlag(
    val land: String?,
    val tilDato: LocalDate?,
    val fraDato: LocalDate?
)

data class UtenlandsAdresseGrunnlag(
    val gyldigFraOgMed: LocalDateTime?,
    val gyldigTilOgMed: LocalDateTime?,
    val adresseNavn: String?,
    val postkode: String?,
    val bySted: String?,
    val landkode: String?,
    val adresseType: AdresseType?
)

enum class Kilde {
    SØKNAD, PDL, MEDL, AA_REGISTERET, A_INNTEKT
}

enum class Indikasjon {
    I_NORGE, UTENFOR_NORGE
}

enum class EØSLand(val alpha2: String) {
    BEL("BE"), BGR("BG"), DNK("DK"), EST("EE"), FIN("FI"),
    FRA("FR"), GRC("GR"), IRL("IE"), ISL("IS"), ITA("IT"),
    HRV("HR"), CYP("CY"), LVA("LV"), LIE("LI"), LTU("LT"),
    LUX("LU"), MLT("MT"), NLD("NL"), NOR("NO"), POL("PL"),
    PRT("PT"), ROU("RO"), SVK("SK"), SVN("SI"), ESP("ES"),
    CHE("CH"), SWE("SE"), CZE("CZ"), DEU("DE"), HUN("HU"), AUT("AT");

    companion object {
        fun erNorge(code: String?): Boolean {
            if (code == null) return false
            return NOR.name == code.uppercase() || NOR.alpha2 == code.uppercase()
        }
    }
}