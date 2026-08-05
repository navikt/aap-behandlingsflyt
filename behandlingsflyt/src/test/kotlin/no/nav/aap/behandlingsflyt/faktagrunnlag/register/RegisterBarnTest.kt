package no.nav.aap.behandlingsflyt.faktagrunnlag.register

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.Barn
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.filtrerBortBarnEldreEnnBruker
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.filtrerBortMigrerteBarn
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag. saksbehandler.barn.BarnIdentifikator
import no. nav.aap.behandlingsflyt.sakogbehandling.Ident
import org.assertj.core.api. Assertions.assertThat
import org.junit.jupiter.api. Test
import java.time.LocalDate

class RegisterBarnTest {

    @Test
    fun `filtrerBortMigrerteBarn skal fjerne migrerte barn når det finnes barn med ident og samme fødselsdato`() {
        val fødselsdato = Fødselsdato(LocalDate.of(2013, 2, 15))

        val barnMedIdent = Barn(
            ident = BarnIdentifikator.BarnIdent(
                ident = Ident("12345678911"),
                navn = "PETER HANSEN",
                fødselsdato = fødselsdato
            ),
            fødselsdato = fødselsdato,
            navn = "PETER HANSEN"
        )

        val migrertBarn = Barn(
            ident = BarnIdentifikator.NavnOgFødselsdato(
                navn = "migrert fra dsf HANSEN PETER",
                fødselsdato = fødselsdato
            ),
            fødselsdato = fødselsdato,
            navn = "migrert fra dsf HANSEN PETER"
        )

        val barnListe = listOf(barnMedIdent, migrertBarn)

        val resultat = barnListe.filtrerBortMigrerteBarn()

        assertThat(resultat).hasSize(1)
        assertThat(resultat).containsExactly(barnMedIdent)
    }

    @Test
    fun `filtrerBortMigrerteBarn skal beholde migrerte barn når det ikke finnes match med ident`() {
        val fødselsdato = Fødselsdato(LocalDate.of(2013, 2, 15))

        val migrertBarn = Barn(
            ident = BarnIdentifikator.NavnOgFødselsdato(
                navn = "migrert fra dsf HANSEN PETER",
                fødselsdato = fødselsdato
            ),
            fødselsdato = fødselsdato,
            navn = "migrert fra dsf HANSEN PETER"
        )

        val barnListe = listOf(migrertBarn)

        val resultat = barnListe.filtrerBortMigrerteBarn()

        assertThat(resultat).hasSize(1)
        assertThat(resultat).containsExactly(migrertBarn)
    }

    @Test
    fun `filtrerBortMigrerteBarn skal beholde alle vanlige barn`() {
        val fødselsdato1 = Fødselsdato(LocalDate.of(2013, 2, 15))
        val fødselsdato2 = Fødselsdato(LocalDate.of(2015, 5, 20))

        val barn1 = Barn(
            ident = BarnIdentifikator.BarnIdent(
                ident = Ident("12345678911"),
                navn = "PETER HANSEN",
                fødselsdato = fødselsdato1
            ),
            fødselsdato = fødselsdato1,
            navn = "PETER HANSEN"
        )

        val barn2 = Barn(
            ident = BarnIdentifikator.BarnIdent(
                ident = Ident("22345678911"),
                navn = "OLE OLSEN",
                fødselsdato = fødselsdato2
            ),
            fødselsdato = fødselsdato2,
            navn = "OLE OLSEN"
        )

        val barnListe = listOf(barn1, barn2)

        val resultat = barnListe.filtrerBortMigrerteBarn()

        assertThat(resultat).hasSize(2)
        assertThat(resultat).containsExactlyInAnyOrder(barn1, barn2)
    }

    @Test
    fun `filtrerBortMigrerteBarn skal håndtere blandings-liste med migrerte og vanlige barn`() {
        val fødselsdato1 = Fødselsdato(LocalDate.of(2013, 2, 15))
        val fødselsdato2 = Fødselsdato(LocalDate.of(2015, 5, 20))

        val barnMedIdent = Barn(
            ident = BarnIdentifikator.BarnIdent(
                ident = Ident("12345678911"),
                navn = "PETER HANSEN",
                fødselsdato = fødselsdato1
            ),
            fødselsdato = fødselsdato1,
            navn = "PETER HANSEN"
        )

        val migrertBarnMedMatch = Barn(
            ident = BarnIdentifikator.NavnOgFødselsdato(
                navn = "migrert fra dsf HANSEN PETER",
                fødselsdato = fødselsdato1
            ),
            fødselsdato = fødselsdato1,
            navn = "migrert fra dsf HANSEN PETER"
        )

        val migrertBarnUtenMatch = Barn(
            ident = BarnIdentifikator.NavnOgFødselsdato(
                navn = "migrert fra dsf OLSEN OLE",
                fødselsdato = fødselsdato2
            ),
            fødselsdato = fødselsdato2,
            navn = "migrert fra dsf OLSEN OLE"
        )

        val barnListe = listOf(barnMedIdent, migrertBarnMedMatch, migrertBarnUtenMatch)

        val resultat = barnListe.filtrerBortMigrerteBarn()

        assertThat(resultat).hasSize(2)
        assertThat(resultat).containsExactlyInAnyOrder(barnMedIdent, migrertBarnUtenMatch)
    }

    @Test
    fun `filtrerBortMigrerteBarn skal returnere tom liste når input er tom`() {
        val barnListe = emptyList<Barn>()

        val resultat = barnListe.filtrerBortMigrerteBarn()

        assertThat(resultat).isEmpty()
    }

    @Test
    fun `filtrerBortBarnEldreEnnBruker skal filtrere bort barn som er født før bruker`() {
        val brukerFødselsdato = LocalDate.of(1990, 1, 1)

        val eldreEnnBruker = Barn(
            ident = BarnIdentifikator.BarnIdent(
                ident = Ident("12345678911"),
                navn = "ELDRE BARN",
                fødselsdato = Fødselsdato(LocalDate.of(1988, 5, 10))
            ),
            fødselsdato = Fødselsdato(LocalDate.of(1988, 5, 10)),
            navn = "ELDRE BARN"
        )

        val yngreEnnBruker = Barn(
            ident = BarnIdentifikator.BarnIdent(
                ident = Ident("22345678911"),
                navn = "YNGRE BARN",
                fødselsdato = Fødselsdato(LocalDate.of(2015, 5, 20))
            ),
            fødselsdato = Fødselsdato(LocalDate.of(2015, 5, 20)),
            navn = "YNGRE BARN"
        )

        val resultat = listOf(eldreEnnBruker, yngreEnnBruker)
            .filtrerBortBarnEldreEnnBruker(brukerFødselsdato)

        assertThat(resultat).containsExactly(yngreEnnBruker)
    }

    @Test
    fun `filtrerBortBarnEldreEnnBruker skal kun beholde barn som er født etter bruker`() {
        val brukerFødselsdato = LocalDate.of(1990, 1, 1)

        val sammeDagSomBruker = Barn(
            ident = BarnIdentifikator.BarnIdent(
                ident = Ident("12345678911"),
                navn = "SAMME DAG",
                fødselsdato = Fødselsdato(LocalDate.of(1990, 1, 1))
            ),
            fødselsdato = Fødselsdato(LocalDate.of(1990, 1, 1)),
            navn = "SAMME DAG"
        )

        val yngreEnnBruker = Barn(
            ident = BarnIdentifikator.BarnIdent(
                ident = Ident("22345678911"),
                navn = "YNGRE BARN",
                fødselsdato = Fødselsdato(LocalDate.of(2012, 3, 4))
            ),
            fødselsdato = Fødselsdato(LocalDate.of(2012, 3, 4)),
            navn = "YNGRE BARN"
        )

        val resultat = listOf(sammeDagSomBruker, yngreEnnBruker)
            .filtrerBortBarnEldreEnnBruker(brukerFødselsdato)

        assertThat(resultat).containsExactly(yngreEnnBruker)
    }
}