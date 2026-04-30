package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.OppgitteBarn
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.BarnIdentifikator
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.VurderingAvForeldreAnsvar
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.VurdertBarn
import no.nav.aap.behandlingsflyt.help.opprettInMemorySakOgBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class InMemoryBarnRepositoryTest {


    @Test
    fun `Finner ikke barn hvis det ikke finnes barn`() {
        val (_, behandling) = opprettInMemorySakOgBehandling()

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
                BarnIdentifikator.BarnIdent(it),
                Fødselsdato(LocalDate.now().minusYears(18))
            )
        }

        val (_, behandling) = opprettInMemorySakOgBehandling()

        val barnRepository = InMemoryBarnRepository

        barnRepository.lagreRegisterBarn(
            behandling.id,
            barnListe.associateWith {
                when (it.ident) {
                    is BarnIdentifikator.BarnIdent -> InMemoryPersonRepository.finnEllerOpprett(
                        listOf((it.ident as BarnIdentifikator.BarnIdent).ident)
                    ).id
                    is BarnIdentifikator.NavnOgFødselsdato -> TODO()
                }

            })
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
}
