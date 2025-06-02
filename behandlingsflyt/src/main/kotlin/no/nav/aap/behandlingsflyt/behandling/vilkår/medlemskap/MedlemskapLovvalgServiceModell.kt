package no.nav.aap.behandlingsflyt.behandling.vilkår.medlemskap

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.KildesystemMedl
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.AdresseType
import no.nav.aap.komponenter.type.Periode
import java.time.LocalDate
import java.time.LocalDateTime

data class KanBehandlesAutomatiskVurdering(
    val kanBehandlesAutomatisk: Boolean,
    val tilhørighetVurdering: List<TilhørighetVurdering>
)

data class TilhørighetVurdering (
    val kilde: List<Kilde>,
    val indikasjon: Indikasjon,
    val opplysning: String,
    val resultat: Boolean,
    val fordypelse: String?, // Todo: fjern meg når FE er klar
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

enum class EØSLand{
    BEL,
    BGR,
    DNK,
    EST,
    FIN,
    FRA,
    GRC,
    IRL,
    ISL,
    ITA,
    HRV,
    CYP,
    LVA,
    LIE,
    LTU,
    LUX,
    MLT,
    NLD,
    NOR,
    POL,
    PRT,
    ROU,
    SVK,
    SVN,
    ESP,
    CHE,
    SWE,
    CZE,
    DEU,
    HUN,
    AUT,
}