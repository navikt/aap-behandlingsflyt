package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.OppgitteBarn
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.BarnIdentifikator
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.VurderingAvForeldreAnsvar
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.VurdertBarn
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.modell.genererIdent
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

class InMemoryBarnRepositoryTest {


    @Test
    fun `Finner ikke barn hvis det ikke finnes barn`() {
        val (_, behandling) = opprettPersonBehandlingOgSak()

        val barnRepository = InMemoryBarnRepository
        val barn = barnRepository.hentHvisEksisterer(behandling.id)
        assertThat(barn?.registerbarn?.barn).isNullOrEmpty()
    }


    @Test
    fun `Lagrer og henter barn`() {
        val vurderteBarn = listOf(
            VurdertBarn(
                ident = BarnIdentifikator.BarnIdent("12345"),
                vurderinger = listOf(
                    VurderingAvForeldreAnsvar(
                        fraDato = LocalDate.now(),
                        harForeldreAnsvar = true,
                        begrunnelse = "fsdf"
                    )
                )
            )
        )
        val barnListe = listOf(Ident("12345678910"), Ident("12345")).map {
            no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.Barn(
                it,
                Fødselsdato(LocalDate.now().minusYears(18))
            )
        }

        val (_, behandling) = opprettPersonBehandlingOgSak()

        val barnRepository = InMemoryBarnRepository

        barnRepository.lagreRegisterBarn(
            behandling.id,
            barnListe.associateWith { InMemoryPersonRepository.finnEllerOpprett(listOf(it.ident)).id })
        barnRepository.lagreOppgitteBarn(
            behandling.id,
            OppgitteBarn(oppgitteBarn = listOf(OppgitteBarn.OppgittBarn(Ident("1"))))
        )
        barnRepository.lagreVurderinger(behandling.id, "ident", vurderteBarn)


        val barn = barnRepository.hent(behandling.id)

        assertThat(barn.registerbarn?.barn).containsExactlyInAnyOrderElementsOf(barnListe)
        assertThat(barn.oppgitteBarn?.oppgitteBarn).containsExactly(
            OppgitteBarn.OppgittBarn(
                ident = Ident("1"),
                navn = null
            )
        )
        assertThat(barn.vurderteBarn?.barn).isEqualTo(vurderteBarn)


        // Slette
        barnRepository.slett(behandling.id)
        assertThat(barnRepository.hentHvisEksisterer(behandling.id)).isNull()

    }

    private fun opprettPersonBehandlingOgSak(): Pair<Sak, Behandling> {
        val person =
            Person(
                Random().nextLong().let(::PersonId),
                UUID.randomUUID(),
                listOf(genererIdent(LocalDate.now().minusYears(23)))
            )
        val sak = InMemorySakRepository.finnEllerOpprett(
            person,
            periode = Periode(LocalDate.now(), LocalDate.now().plusDays(5)),
        )
        val behandling = InMemoryBehandlingRepository.opprettBehandling(
            sakId = sak.id,
            vurderingsbehov = listOf(),
            typeBehandling = TypeBehandling.Førstegangsbehandling,
            forrigeBehandlingId = null
        )
        return Pair(sak, behandling)
    }

}
