package no.nav.aap.behandlingsflyt.behandling.barnetillegg

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.Barn
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.OppgitteBarn
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.BarnIdentifikator
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.VurderingAvForeldreAnsvar
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.VurdertBarn
import no.nav.aap.behandlingsflyt.help.assertTidslinje
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryBarnRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryBehandlingRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryPersonRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemorySakRepository
import no.nav.aap.behandlingsflyt.test.inmemoryservice.InMemorySakOgBehandlingService
import no.nav.aap.behandlingsflyt.test.modell.genererIdent
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

class BarnetilleggServiceTest {

    @Test
    fun `ingen barn gir ingen rett til barnetillegg`() {
        val service = BarnetilleggService(
            sakOgBehandlingService = InMemorySakOgBehandlingService,
            barnRepository = InMemoryBarnRepository
        )

        val (sak, behandling) = opprettPersonBehandlingOgSak()

        lagreRegisterOpplysninger(behandling, emptyList())

        val res = service.beregn(behandlingId = behandling.id)

        assertTidslinje(res, sak.rettighetsperiode to {
            assertThat(it).isEqualTo(RettTilBarnetillegg())
        })
    }

    @Test
    fun `tidslinjen stopper når barnet blir 18 år`() {
        val service = BarnetilleggService(
            sakOgBehandlingService = InMemorySakOgBehandlingService,
            barnRepository = InMemoryBarnRepository
        )

        val (sak, behandling) = opprettPersonBehandlingOgSak()

        val fødselsdatoGammeltBarn = LocalDate.now().minusYears(21)
        val fødseldatoUngtBarn = LocalDate.now().minusYears(5)

        val gammeltBarn = Barn(
            ident = BarnIdentifikator.BarnIdent(genererIdent(fødselsdatoGammeltBarn)),
            fødselsdato = Fødselsdato(fødselsdatoGammeltBarn)
        )

        val ungtBarn = Barn(
            ident = BarnIdentifikator.BarnIdent(genererIdent(fødseldatoUngtBarn)),
            fødselsdato = Fødselsdato(fødseldatoUngtBarn)
        )

        lagreRegisterOpplysninger(
            behandling,
            listOf(gammeltBarn, ungtBarn)
        )

        val res = service.beregn(behandlingId = behandling.id)

        assertTidslinje(res, sak.rettighetsperiode to {
            assertThat(it).isEqualTo(
                RettTilBarnetillegg(
                    // Kun det unge barnet gir rett til barnetillegg
                    barn = setOf(ungtBarn.identifikator())
                )
            )
        })
    }

    @Test
    fun `avklarer manuelt barn, får barnetillegg fram til 18 år`() {
        val service = BarnetilleggService(
            sakOgBehandlingService = InMemorySakOgBehandlingService,
            barnRepository = InMemoryBarnRepository
        )

        val (sak, behandling) = opprettPersonBehandlingOgSak()

        // 17 år, så skal bare ha ett år med barnetillegg
        val fødselsdato = Fødselsdato(LocalDate.now().minusYears(17))
        val navn = "Gregor Gregersen"

        InMemoryBarnRepository.lagreOppgitteBarn(
            behandling.id, OppgitteBarn(
                oppgitteBarn = listOf(
                    OppgitteBarn.OppgittBarn(
                        ident = null,
                        navn = navn,
                        fødselsdato = fødselsdato,
                        relasjon = OppgitteBarn.Relasjon.FOSTERFORELDER
                    )
                )
            )
        )

        val res = service.beregn(behandlingId = behandling.id)

        val barnFremdelesSyttenÅr =
            Periode(sak.rettighetsperiode.fom, sak.rettighetsperiode.fom.plusYears(1).minusDays(1))
        val ingenBarnUnderAttenÅr = Periode(sak.rettighetsperiode.fom.plusYears(1), sak.rettighetsperiode.tom)

        assertTidslinje(
            res,
            barnFremdelesSyttenÅr to {
                assertThat(it.barnTilAvklaring()).isEqualTo(
                    setOf(BarnIdentifikator.NavnOgFødselsdato(navn, fødselsdato))
                )
            },
            ingenBarnUnderAttenÅr to {
                assertThat(it.barnTilAvklaring()).isEmpty()
            },
        )

        // Lagre vurdering
        InMemoryBarnRepository.lagreVurderinger(
            behandling.id,
            vurdertAv = "Fredrik",
            vurderteBarn = listOf(
                VurdertBarn(
                    ident = BarnIdentifikator.NavnOgFødselsdato(navn, fødselsdato),
                    vurderinger = listOf(
                        VurderingAvForeldreAnsvar(
                            fraDato = fødselsdato.toLocalDate(),
                            harForeldreAnsvar = true,
                            begrunnelse = "..."
                        )
                    )
                )
            )
        )

        val res2 = service.beregn(behandlingId = behandling.id)
        assertTidslinje(
            res2,
            barnFremdelesSyttenÅr to {
                assertThat(it.barnMedRettTil()).containsExactly(
                    BarnIdentifikator.NavnOgFødselsdato(navn, fødselsdato)
                )
            },
            ingenBarnUnderAttenÅr to {
                assertThat(it.barnMedRettTil()).isEmpty()
            }
        )
    }

    private fun lagreRegisterOpplysninger(
        behandling: Behandling,
        barn: List<Barn>
    ) {
        InMemoryBarnRepository.lagreRegisterBarn(
            behandling.id,
            barn.associateWith { InMemoryPersonRepository.finnEllerOpprett(listOf((it.ident as BarnIdentifikator.BarnIdent).ident)).id }
        )
    }

    private fun opprettPersonBehandlingOgSak(): Pair<Sak, Behandling> {
        val person =
            Person(
                PersonId(Random().nextLong()),
                UUID.randomUUID(),
                listOf(genererIdent(LocalDate.now().minusYears(23)))
            )
        val sak = InMemorySakRepository.finnEllerOpprett(
            person,
            periode = Periode(LocalDate.now(), LocalDate.now().plusYears(5)),
        )
        val behandling = InMemoryBehandlingRepository.opprettBehandling(
            sakId = sak.id,
            typeBehandling = TypeBehandling.Førstegangsbehandling,
            forrigeBehandlingId = null,
            vurderingsbehovOgÅrsak = VurderingsbehovOgÅrsak(
                vurderingsbehov = listOf(VurderingsbehovMedPeriode(Vurderingsbehov.MOTTATT_SØKNAD)),
                årsak = ÅrsakTilOpprettelse.SØKNAD,
            ),
        )
        return Pair(sak, behandling)
    }
}
