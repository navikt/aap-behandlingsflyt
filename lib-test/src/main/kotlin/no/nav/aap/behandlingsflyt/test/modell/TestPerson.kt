package no.nav.aap.behandlingsflyt.test.modell

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.Dødsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektPerÅr
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.test.genererIdent
import no.nav.aap.verdityper.Beløp
import no.nav.aap.verdityper.sakogbehandling.Ident
import java.time.LocalDate
import java.time.Year

class TestPerson (
    val fødselsdato: Fødselsdato = Fødselsdato(LocalDate.now().minusYears(19)),
    val identer: Set<Ident> = setOf( genererIdent(fødselsdato.toLocalDate())),
    val dødsdato: Dødsdato? = null,
    val barn: List<TestPerson> = emptyList(),
    val yrkesskade:List<TestYrkesskade> = emptyList(),
    var inntekter: List<InntektPerÅr> = (1..10).map { InntektPerÅr(Year.now().minusYears(it.toLong()), Beløp("1000000.0")) }
)