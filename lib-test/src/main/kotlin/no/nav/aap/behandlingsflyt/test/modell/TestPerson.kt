package no.nav.aap.behandlingsflyt.test.modell

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.tjenestepensjon.gateway.TjenestePensjonRespons
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.Dødsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektPerÅr
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.MedlemskapDataIntern
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.integrasjon.institusjonsopphold.InstitusjonsoppholdJSON
import no.nav.aap.behandlingsflyt.integrasjon.pdl.PdlFolkeregisterPersonStatus
import no.nav.aap.behandlingsflyt.integrasjon.pdl.PdlStatsborgerskap
import no.nav.aap.behandlingsflyt.integrasjon.pdl.PersonStatus
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.test.FakePersoner
import no.nav.aap.behandlingsflyt.test.FiktivtNavnGenerator
import no.nav.aap.behandlingsflyt.test.FødselsnummerGenerator
import no.nav.aap.behandlingsflyt.test.PersonNavn
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.Prosent
import java.time.LocalDate
import java.time.Year

fun genererIdent(fødselsdato: LocalDate): Ident {
    return Ident(FødselsnummerGenerator.Builder().fodselsdato(fødselsdato).buildAndGenerate())
}

fun defaultInntekt(): List<InntektPerÅr> {
    return (1..10).map { InntektPerÅr(Year.now().minusYears(it.toLong()), Beløp("400000.0")) }
}

class TestPerson(
    val fødselsdato: Fødselsdato = Fødselsdato(LocalDate.now().minusYears(19)),
    val identer: Set<Ident> = setOf(genererIdent(fødselsdato.toLocalDate())),
    val dødsdato: Dødsdato? = null,
    var barn: List<TestPerson> = emptyList(),
    val navn: PersonNavn = FiktivtNavnGenerator.genererNavn(),
    val yrkesskade: List<TestYrkesskade> = emptyList(),
    var institusjonsopphold: List<InstitusjonsoppholdJSON> = emptyList(),
    var uføre: Prosent? = null,
    inntekter: List<InntektPerÅr> = defaultInntekt(),
    val personStatus: List<PdlFolkeregisterPersonStatus> = listOf(
        PdlFolkeregisterPersonStatus(
            PersonStatus.bosatt,
            null
        )
    ),
    val statsborgerskap: List<PdlStatsborgerskap> = listOf(
        PdlStatsborgerskap(
            "NOR",
            LocalDate.now().minusYears(5),
            null
        )
    ),
    val medlStatus: List<MedlemskapDataIntern> = listOf(),
    var sykepenger: List<Sykepenger>? = null,
    val foreldrepenger: List<ForeldrePenger>? = null,
    val tjenestePensjon: TjenestePensjonRespons? = null
) {
    data class Sykepenger(val grad: Int, val periode: Periode)
    data class ForeldrePenger(val grad: Number, val periode: Periode)


    private val inntekter: MutableList<InntektPerÅr> = inntekter.toMutableList()

    fun inntekter(): List<InntektPerÅr> {
        return inntekter.toList()
    }

    fun aktivIdent(): Ident {
        return identer.single { it.aktivIdent }
    }

    override fun toString(): String {
        return "TestPerson(barn=$barn, fødselsdato=$fødselsdato, identer=$identer, dødsdato=$dødsdato, navn=$navn, yrkesskade=$yrkesskade, institusjonsopphold=$institusjonsopphold, uføre=$uføre, personStatus=$personStatus, statsborgerskap=$statsborgerskap, sykepenger=$sykepenger, foreldrepenger=$foreldrepenger, inntekter=$inntekter)"
    }

    fun sykepenger(): List<Sykepenger> {
        return sykepenger ?: emptyList()
    }

    fun medBarn(barn: List<TestPerson>): TestPerson {
        this.barn = barn
        this.barn.forEach { FakePersoner.leggTil(it) }
        return this
    }

    fun medUføre(uføre: Prosent?): TestPerson {
        this.uføre = uføre
        return this
    }

    fun medSykepenger(sykepenger: List<Sykepenger>): TestPerson {
        this.sykepenger = sykepenger
        return this
    }
}
