package no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.adapter

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.Barn
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.BarnIdentifikator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

class BarnInnhentingResponsTest {

    @Test
    fun `alleBarn skal returnere alle barn når ingen duplikater`() {
        val barn1 = Barn(
            ident = BarnIdentifikator.BarnIdent("12345678901"),
            fødselsdato = Fødselsdato(LocalDate.of(2020, 1, 1)),
            navn = "Sofia Andersen"
        )
        val barn2 = Barn(
            ident = BarnIdentifikator.BarnIdent("23456789012"),
            fødselsdato = Fødselsdato(LocalDate.of(2021, 2, 2)),
            navn = "Emil Hansen"
        )
        val barn3 = Barn(
            ident = BarnIdentifikator.BarnIdent("34567890123"),
            fødselsdato = Fødselsdato(LocalDate.of(2022, 3, 3)),
            navn = "Lina Johansen"
        )

        val respons = BarnInnhentingRespons(
            registerBarn = listOf(barn1),
            oppgitteBarnFraPDL = listOf(barn2),
            saksbehandlerOppgitteBarnPDL = listOf(barn3)
        )

        val result = respons.alleBarn()

        assertEquals(3, result.size)
        assertTrue(result.contains(barn1))
        assertTrue(result.contains(barn2))
        assertTrue(result.contains(barn3))
    }

    @Test
    fun `alleBarn skal filtrere bort duplikater basert på ident`() {
        val barn1 = Barn(
            ident = BarnIdentifikator.BarnIdent("12345678901"),
            fødselsdato = Fødselsdato(LocalDate.of(2020, 1, 1)),
            navn = "Lea Pedersen"
        )
        val barn2 = Barn(
            ident = BarnIdentifikator.BarnIdent("12345678901"),
            fødselsdato = Fødselsdato(LocalDate.of(2020, 1, 1)),
            navn = "Lea Pedersen"
        )
        val barn3 = Barn(
            ident = BarnIdentifikator.BarnIdent("23456789012"),
            fødselsdato = Fødselsdato(LocalDate.of(2021, 2, 2)),
            navn = "Tone Larsen"
        )

        val respons = BarnInnhentingRespons(
            registerBarn = listOf(barn1),
            oppgitteBarnFraPDL = listOf(barn2),
            saksbehandlerOppgitteBarnPDL = listOf(barn3)
        )

        val result = respons.alleBarn()

        assertEquals(2, result.size)
        assertEquals(BarnIdentifikator.BarnIdent("12345678901"), result[0].ident)
        assertEquals(BarnIdentifikator.BarnIdent("23456789012"), result[1].ident)
    }

    @Test
    fun `alleBarn skal returnere tom liste når alle lister er tomme`() {
        val respons = BarnInnhentingRespons(
            registerBarn = emptyList(),
            oppgitteBarnFraPDL = emptyList(),
            saksbehandlerOppgitteBarnPDL = emptyList()
        )

        val result = respons.alleBarn()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `alleBarn skal beholde første forekomst ved duplikater`() {
        val barn1 = Barn(
            ident = BarnIdentifikator.BarnIdent("12345678901"),
            fødselsdato = Fødselsdato(LocalDate.of(2020, 1, 1)),
            navn = "Mia Olsen"
        )
        val barn2 = Barn(
            ident = BarnIdentifikator.BarnIdent("12345678901"),
            fødselsdato = Fødselsdato(LocalDate.of(2020, 1, 1)),
            navn = "Mia Olsen"
        )

        val respons = BarnInnhentingRespons(
            registerBarn = listOf(barn1),
            oppgitteBarnFraPDL = listOf(barn2),
            saksbehandlerOppgitteBarnPDL = emptyList()
        )

        val result = respons.alleBarn()

        assertEquals(1, result.size)
        assertSame(barn1, result[0])
    }
}