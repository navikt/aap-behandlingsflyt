package no.nav.aap.behandlingsflyt.behandling.barnetillegg

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.Barn
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.Dødsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.OppgitteBarn
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.Relasjon
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.BarnIdentifikator
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.VurderingAvForeldreAnsvar
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.VurdertBarn
import no.nav.aap.behandlingsflyt.help.assertTidslinje
import no.nav.aap.behandlingsflyt.integrasjon.createGatewayProvider
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.FakeUnleash
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryBarnRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryBehandlingRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryPersonRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemorySakRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.inMemoryRepositoryProvider
import no.nav.aap.behandlingsflyt.test.modell.genererIdent
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

class BarnetilleggServiceTest {
    val gatewayProvider = createGatewayProvider {
        register<FakeUnleash>()
    }

    @BeforeEach
    fun setup() {
        System.setProperty("NAIS_CLUSTER_NAME", "LOCAL")
    }

    @Test
    fun `ingen barn gir ingen rett til barnetillegg`() {
        val service = BarnetilleggService(inMemoryRepositoryProvider, gatewayProvider)

        val (sak, behandling) = opprettPersonBehandlingOgSak()

        lagreRegisterOpplysninger(behandling, emptyList())

        val res = service.beregn(behandlingId = behandling.id)

        assertTidslinje(res, sak.rettighetsperiode to {
            assertThat(it).isEqualTo(RettTilBarnetillegg())
        })
    }

    @Test
    fun `tidslinjen stopper når barnet blir 18 år`() {
        val service = BarnetilleggService(inMemoryRepositoryProvider, gatewayProvider)

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
    fun `barnetillegg fram til dødsdato`() {
        val service = BarnetilleggService(inMemoryRepositoryProvider, gatewayProvider)

        val (sak, behandling) = opprettPersonBehandlingOgSak()

        val fødseldatoUngtBarn = LocalDate.now().minusYears(5)
        val dødsdato = LocalDate.now().plusMonths(6)

        val dødtBarn = Barn(
            ident = BarnIdentifikator.BarnIdent(genererIdent(fødseldatoUngtBarn)),
            fødselsdato = Fødselsdato(fødseldatoUngtBarn),
            dødsdato = Dødsdato(dødsdato)
        )

        lagreRegisterOpplysninger(
            behandling,
            listOf(dødtBarn)
        )

        val res = service.beregn(behandlingId = behandling.id)

        // Verifiserer at barnetillegg gis _til og med_ dødsdato
        // Men ikke etter
        assertTidslinje(res, Periode(sak.rettighetsperiode.fom, dødsdato) to {
            assertThat(it.barnMedRettTil()).hasSize(1)
            assertThat(it.barnTilAvklaring()).hasSize(0)
        }, Periode(dødsdato.plusDays(1), sak.rettighetsperiode.tom) to {
            assertThat(it.barnMedRettTil()).isEmpty()
            assertThat(it.barnTilAvklaring()).hasSize(0)
        })
    }

    @Test
    fun `barnetillegg for folkeregistrerte barn skal gi rett på barnetillegg om man ikke har lagt inn perioder`() {
        val service = BarnetilleggService(inMemoryRepositoryProvider, gatewayProvider)
        val (sak, behandling) = opprettPersonBehandlingOgSak()
        val fødseldatoUngtBarn = LocalDate.now().minusYears(5)

        val barn = Barn(
            ident = BarnIdentifikator.BarnIdent(genererIdent(fødseldatoUngtBarn)),
            fødselsdato = Fødselsdato(fødseldatoUngtBarn),
        )

        lagreRegisterOpplysninger(
            behandling,
            listOf(barn)
        )

        val res = service.beregn(behandlingId = behandling.id)

        assertTidslinje(res, Periode(sak.rettighetsperiode.fom, sak.rettighetsperiode.tom) to {
            assertThat(it.barnMedRettTil()).hasSize(1)
        })
    }

    @Test
    fun `barnetillegg for folkeregistrerte barn skal ikke gi barnetillegg for perioder man sier man ikke har rett`() {
        val service = BarnetilleggService(inMemoryRepositoryProvider, gatewayProvider)
        val (sak, behandling) = opprettPersonBehandlingOgSak()

        val barn = Barn(
            ident = BarnIdentifikator.BarnIdent(genererIdent(LocalDate.now().minusYears(5))),
            fødselsdato = Fødselsdato(LocalDate.now().minusYears(5)),
        )

        val periodeUtenRett = Periode(sak.rettighetsperiode.fom, sak.rettighetsperiode.fom.plusMonths(1))
        val periodeMedRett = Periode(periodeUtenRett.tom.plusDays(1), sak.rettighetsperiode.tom)

        lagreRegisterOpplysninger(
            behandling,
            listOf(barn)
        )

        lagreVurdering(behandling, VurdertBarn(ident = barn.ident, vurderinger = listOf(
            VurderingAvForeldreAnsvar(fraDato = periodeUtenRett.fom, harForeldreAnsvar = false, begrunnelse = "Uten rett"),
            VurderingAvForeldreAnsvar(fraDato = periodeMedRett.fom, harForeldreAnsvar = true, begrunnelse = "Med rett")
        )))

        val res = service.beregn(behandlingId = behandling.id)

        assertTidslinje(res,
            periodeUtenRett to { assertThat(it.barnMedRettTil()).hasSize(0) },
            periodeMedRett to { assertThat(it.barnMedRettTil()).hasSize(1) }
        )
    }


    @Test
    fun `avklarer manuelt barn, får barnetillegg fram til 18 år`() {
        val service = BarnetilleggService(inMemoryRepositoryProvider, gatewayProvider)

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
                        relasjon = Relasjon.FOSTERFORELDER
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

    private fun lagreVurdering(behandling: Behandling, vurdertBarn: VurdertBarn) {
        InMemoryPersonRepository.finnEllerOpprett(listOf((vurdertBarn.ident as BarnIdentifikator.BarnIdent).ident))

        InMemoryBarnRepository.lagreVurderinger(
            behandling.id,
            "test-ident",
            listOf(vurdertBarn)
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
