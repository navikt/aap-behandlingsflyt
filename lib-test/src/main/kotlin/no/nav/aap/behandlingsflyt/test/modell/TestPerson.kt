package no.nav.aap.behandlingsflyt.test.modell

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.Dødsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektPerÅr
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.test.FiktivtNavnGenerator
import no.nav.aap.behandlingsflyt.test.FødselsnummerGenerator
import no.nav.aap.behandlingsflyt.test.PersonNavn
import no.nav.aap.institusjon.Institusjonsopphold
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.verdityper.sakogbehandling.Ident
import java.time.LocalDate
import java.time.Year

fun genererIdent(fødselsdato: LocalDate): Ident {
    return Ident(FødselsnummerGenerator.Builder().fodselsdato(fødselsdato).buildAndGenerate())
}

class TestPerson(
    val fødselsdato: Fødselsdato = Fødselsdato(LocalDate.now().minusYears(19)),
    val identer: Set<Ident> = setOf(genererIdent(fødselsdato.toLocalDate())),
    val dødsdato: Dødsdato? = null,
    val barn: List<TestPerson> = emptyList(),
    val navn: PersonNavn = FiktivtNavnGenerator.genererNavn(),
    val yrkesskade: List<TestYrkesskade> = emptyList(),
    val institusjonsopphold: List<Institusjonsopphold> = emptyList(),
    val uføre: Prosent? = Prosent(0),
    inntekter: List<InntektPerÅr> = (1..10).map { InntektPerÅr(Year.now().minusYears(it.toLong()), Beløp("400000.0")) }
) {
    private val inntekter: MutableList<InntektPerÅr> = inntekter.toMutableList()

    fun inntekter(): List<InntektPerÅr> {
        return inntekter.toList()
    }

    fun leggTilInntektHvisÅrMangler(år: Year, beløp: Beløp) {
        if (inntekter.none { it.år == år }) {
            inntekter.add(InntektPerÅr(år, beløp))
        }
    }

    override fun toString(): String {
        return "TestPerson(fødselsdato=$fødselsdato, identer=$identer, dødsdato=$dødsdato, barn=$barn, navn=$navn, yrkesskade=$yrkesskade, institusjonsopphold=$institusjonsopphold, inntekter=$inntekter)"
    }

    fun aktivIdent(): Ident {
        return identer.single { it.aktivIdent }
    }
}
