package no.nav.aap.behandlingsflyt.test.modell

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.Dødsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.verdityper.sakogbehandling.Ident

class TestOpprettPerson (
    val identer: Set<Ident>,
    val fødselsdato: Fødselsdato,
    val dødsdato: Dødsdato? = null,
    val barn: List<TestPerson> = emptyList(),
    val yrkesskade:Boolean = false,
)