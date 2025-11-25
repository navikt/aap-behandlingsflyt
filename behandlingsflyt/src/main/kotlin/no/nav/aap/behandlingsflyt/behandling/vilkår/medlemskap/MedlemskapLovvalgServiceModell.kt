package no.nav.aap.behandlingsflyt.behandling.vilkår.medlemskap

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.KildesystemMedl
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.AdresseType
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonStatus
import no.nav.aap.komponenter.type.Periode
import java.time.LocalDate

data class KanBehandlesAutomatiskVurdering(
    val kanBehandlesAutomatisk: Boolean,
    val tilhørighetVurdering: List<TilhørighetVurdering>
)

data class TilhørighetVurdering(
    val kilde: List<Kilde>,
    val indikasjon: Indikasjon,
    val opplysning: String,
    val resultat: Boolean,
    val vurdertPeriode: String,
    val vedtakImedlGrunnlag: List<VedtakIMEDLGrunnlag>? = null,
    val mottarSykepengerGrunnlag: List<MottarSykepengerGrunnlag>? = null,
    val arbeidInntektINorgeGrunnlag: List<ArbeidInntektINorgeGrunnlag>? = null,
    val manglerStatsborgerskapGrunnlag: List<ManglerStatsborgerskapGrunnlag>? = null,
    val oppgittJobbetIUtlandGrunnlag: List<OppgittJobbetIUtlandGrunnlag>? = null,
    val oppgittUtenlandsOppholdGrunnlag: List<OppgittUtenlandsOppholdGrunnlag>? = null,
    val utenlandsAddresserGrunnlag: UtenlandsAdresserGrunnlag? = null,
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
    val fraDato: LocalDate?,
    val tilDato: LocalDate?,
)

data class OppgittUtenlandsOppholdGrunnlag(
    val land: String?,
    val fraDato: LocalDate?,
    val tilDato: LocalDate?,
)

data class UtenlandsAdresserGrunnlag(
    val adresser: List<UtenlandskAdresseDto>?,
    val personStatus: List<FolkeregisterStatusDto>?
)

data class FolkeregisterStatusDto(
    val status: PersonStatus?,
    val gyldighetstidspunkt: LocalDate?,
    val opphoerstidspunkt: LocalDate?
)

data class UtenlandskAdresseDto(
    val gyldigFraOgMed: LocalDate?,
    val gyldigTilOgMed: LocalDate?,
    val adresseNavn: String?,
    val postkode: String?,
    val bySted: String?,
    val landkode: String?,
    val adresseType: AdresseType?
)

enum class Kilde {
    SØKNAD, PDL, MEDL, AA_REGISTERET, A_INNTEKT, EREG
}

enum class Indikasjon {
    I_NORGE, UTENFOR_NORGE
}

enum class VurdertPeriode(val beskrivelse: String) {
    INNEVÆRENDE_OG_FORRIGE_MND("Inneværende og forrige måned"),
    SØKNADSTIDSPUNKT("Søknadstidspunkt"),
    SISTE_5_ÅR("Siste 5 år")
}

enum class EØSLandEllerLandMedAvtale(val alpha2: String) {
    BEL("BE"), BGR("BG"), DNK("DK"), EST("EE"), FIN("FI"),
    FRA("FR"), GRC("GR"), IRL("IE"), ISL("IS"), ITA("IT"),
    HRV("HR"), CYP("CY"), LVA("LV"), LIE("LI"), LTU("LT"),
    LUX("LU"), MLT("MT"), NLD("NL"), NOR("NO"), POL("PL"),
    PRT("PT"), ROU("RO"), SVK("SK"), SVN("SI"), ESP("ES"),
    CHE("CH"), SWE("SE"), CZE("CZ"), DEU("DE"), HUN("HU"),
    AUT("AT"), GBR("GB"), AUS("AU");

    companion object {
        fun erNorge(code: String?): Boolean {
            if (code == null) return false
            return NOR.name == code.uppercase() || NOR.alpha2 == code.uppercase()
        }
    }
}