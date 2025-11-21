package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering

import no.nav.aap.behandlingsflyt.behandling.samordning.Ytelse
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav
import no.nav.aap.behandlingsflyt.integrasjon.samordning.AbakusForeldrepengerGateway
import no.nav.aap.behandlingsflyt.integrasjon.samordning.AbakusSykepengerGateway
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.samordning.SamordningYtelseRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.samordning.ytelsesvurdering.SamordningVurderingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.postgresRepositoryRegistry
import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.behandlingsflyt.test.FakePersoner
import no.nav.aap.behandlingsflyt.test.FakeTidligereVurderinger
import no.nav.aap.behandlingsflyt.test.Fakes
import no.nav.aap.behandlingsflyt.test.modell.TestPerson
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Prosent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.LocalDate

@Fakes
class SamordningYtelseVurderingServiceTest {
    companion object {
        private lateinit var dataSource: TestDataSource

        @BeforeAll
        @JvmStatic
        fun setup() {
            dataSource = TestDataSource()
        }

        @AfterAll
        @JvmStatic
        fun tearDown() = dataSource.close()
    }

    @Test
    fun `fpabakus tar ikke med perioder utenfor oppslagsperioden`() {
        val person = TestPerson(
            foreldrepenger = listOf(
                TestPerson.ForeldrePenger(
                    50,
                    Periode(LocalDate.now().minusYears(1), LocalDate.now())
                ),
                TestPerson.ForeldrePenger(
                    60,
                    Periode(LocalDate.now().minusYears(5), LocalDate.now().minusYears(4))
                ),
                TestPerson.ForeldrePenger(
                    52,
                    Periode(LocalDate.now().minusYears(5), LocalDate.now())
                ),
                TestPerson.ForeldrePenger(
                    51,
                    Periode(LocalDate.now().minusWeeks(5), LocalDate.now())
                ),
                TestPerson.ForeldrePenger(
                    49,
                    Periode(LocalDate.now().minusWeeks(3), LocalDate.now())
                )
            )
        )
        FakePersoner.leggTil(person)

        dataSource.transaction { connection ->
            val samordningYtelseVurderingInformasjonskrav = SamordningYtelseVurderingInformasjonskrav(
                SamordningYtelseRepositoryImpl(connection),
                SamordningVurderingRepositoryImpl(connection),
                FakeTidligereVurderinger(),
                AbakusForeldrepengerGateway(),
                AbakusSykepengerGateway(),
                SakService(postgresRepositoryRegistry.provider(connection))
            )
            val foreldrePerson = PersonRepositoryImpl(connection).finnEllerOpprett(
                listOf(
                    Ident(
                        person.identer.first().identifikator,
                        true
                    )
                )
            )
            val kontekst =
                opprettSakdata(connection, foreldrePerson, Periode(LocalDate.now(), LocalDate.now().plusYears(1)))

            val input = samordningYtelseVurderingInformasjonskrav.klargjør(kontekst)
            val data = samordningYtelseVurderingInformasjonskrav.hentData(input)

            val ytelsePerioder = data.samordningYtelser.flatMap { it.ytelsePerioder }
            assertThat(ytelsePerioder.size).isEqualTo(1)
        }
    }

    @Test
    fun `k9 tar ikke med perioder utenfor oppslagsperioden`() {
        val person = TestPerson(
            sykepenger = listOf(
                TestPerson.Sykepenger(
                    52,
                    Periode(LocalDate.now().minusYears(5), LocalDate.now())
                ),
                TestPerson.Sykepenger(
                    51,
                    Periode(LocalDate.now().minusWeeks(5), LocalDate.now())
                ),
                TestPerson.Sykepenger(
                    49,
                    Periode(LocalDate.now().minusWeeks(3), LocalDate.now())
                )
            )
        )
        FakePersoner.leggTil(person)

        dataSource.transaction { connection ->
            val samordningYtelseVurderingInformasjonskrav = SamordningYtelseVurderingInformasjonskrav(
                SamordningYtelseRepositoryImpl(connection),
                SamordningVurderingRepositoryImpl(connection),
                FakeTidligereVurderinger(),
                AbakusForeldrepengerGateway(),
                AbakusSykepengerGateway(),
                SakService(postgresRepositoryRegistry.provider(connection))
            )
            val sykepengerPerson = PersonRepositoryImpl(connection).finnEllerOpprett(
                listOf(
                    Ident(
                        person.identer.first().identifikator,
                        true
                    )
                )
            )
            val kontekst =
                opprettSakdata(connection, sykepengerPerson, Periode(LocalDate.now(), LocalDate.now().plusYears(1)))

            val input = samordningYtelseVurderingInformasjonskrav.klargjør(kontekst)
            val data = samordningYtelseVurderingInformasjonskrav.hentData(input)

            val ytelsePerioder = data.samordningYtelser.flatMap { it.ytelsePerioder }
            assertThat(ytelsePerioder.size).isEqualTo(1)
        }
    }

    @Test
    fun `krever avklaring når endringer kommer`() {
        dataSource.transaction { connection ->
            val ytelseRepo = SamordningYtelseRepositoryImpl(connection)
            val repo = SamordningVurderingRepositoryImpl(connection)
            val samordningYtelseVurderingInformasjonskrav = SamordningYtelseVurderingInformasjonskrav(
                SamordningYtelseRepositoryImpl(connection),
                SamordningVurderingRepositoryImpl(connection),
                FakeTidligereVurderinger(),
                AbakusForeldrepengerGateway(),
                AbakusSykepengerGateway(),
                SakService(postgresRepositoryRegistry.provider(connection))
            )
            val kontekst = opprettSakdata(connection)

            // Når vi for første gang får opplysninger er det en endring
            val ingenData = klargjørOgOppdater(samordningYtelseVurderingInformasjonskrav, kontekst)
            assertThat(ingenData).isEqualTo(Informasjonskrav.Endret.ENDRET)

            // Data er uforandret
            val sammeData = klargjørOgOppdater(samordningYtelseVurderingInformasjonskrav, kontekst)
            assertThat(sammeData).isEqualTo(Informasjonskrav.Endret.IKKE_ENDRET)

            // Ny data har kommet inn
            opprettYtelseData(ytelseRepo, kontekst.behandlingId)
            opprettVurderingData(repo, kontekst.behandlingId)
            val nyData = klargjørOgOppdater(samordningYtelseVurderingInformasjonskrav, kontekst)
            assertThat(nyData).isEqualTo(Informasjonskrav.Endret.ENDRET)
        }
    }

    private fun klargjørOgOppdater(
        samordningYtelseVurderingInformasjonskrav: SamordningYtelseVurderingInformasjonskrav,
        kontekst: FlytKontekstMedPerioder
    ): Informasjonskrav.Endret {
        val input = samordningYtelseVurderingInformasjonskrav.klargjør(kontekst)
        val data = samordningYtelseVurderingInformasjonskrav.hentData(input)
        val ingenData = samordningYtelseVurderingInformasjonskrav.oppdater(input, data, kontekst)
        return ingenData
    }

    @Test
    fun `Rekkefølge i lister skal ikke gi endring`() {
        val nå = LocalDate.now()

        val eksisterendeGrunnlag = SamordningYtelseGrunnlag(
            1, setOf(
                SamordningYtelse(
                    Ytelse.SYKEPENGER,
                    setOf(
                        SamordningYtelsePeriode(
                            Periode(nå.plusDays(5), nå.plusDays(10)),
                            Prosent(50),
                            0
                        ),
                        SamordningYtelsePeriode(
                            Periode(nå.plusDays(5), nå.plusDays(10)),
                            Prosent(40),
                            0
                        ),
                    ),
                    "kilde",
                    "ref"
                ),
                SamordningYtelse(
                    Ytelse.SYKEPENGER,
                    setOf(
                        SamordningYtelsePeriode(
                            Periode(nå.plusDays(5), nå.plusDays(10)),
                            Prosent(50),
                            0
                        ),
                    ),
                    "kilde",
                    "ref"
                ),
            )
        )

        val ny = setOf(
            SamordningYtelse(
                Ytelse.SYKEPENGER,
                setOf(
                    SamordningYtelsePeriode(
                        Periode(nå.plusDays(5), nå.plusDays(10)),
                        Prosent(50),
                        0
                    ),
                ),
                "kilde",
                "ref"
            ),
            SamordningYtelse(
                Ytelse.SYKEPENGER,
                setOf(
                    SamordningYtelsePeriode(
                        Periode(nå.plusDays(5), nå.plusDays(10)),
                        Prosent(40),
                        0
                    ),
                    SamordningYtelsePeriode(
                        Periode(nå.plusDays(5), nå.plusDays(10)),
                        Prosent(50),
                        0
                    ),
                ),
                "kilde",
                "ref"
            ),
        )

        assertThat(
            SamordningYtelseVurderingInformasjonskrav.harEndringerIYtelser(
                eksisterendeGrunnlag, ny
            )
        ).isFalse()
    }

    private fun opprettVurderingData(
        samordningVurderingRepo: SamordningVurderingRepositoryImpl,
        behandlingId: BehandlingId
    ) {
        samordningVurderingRepo.lagreVurderinger(
            behandlingId,
            SamordningVurderingGrunnlag(
                begrunnelse = "En god begrunnelse",
                vurderinger = setOf(
                    SamordningVurdering(
                        Ytelse.SYKEPENGER,
                        setOf(
                            SamordningVurderingPeriode(
                                Periode(LocalDate.now(), LocalDate.now().plusDays(5)),
                                Prosent(50),
                                0,
                                false
                            )
                        )
                    )
                ),
                vurdertAv = "ident"
            )
        )
    }

    private fun opprettYtelseData(samordningYtelseRepo: SamordningYtelseRepositoryImpl, behandlingId: BehandlingId) {
        samordningYtelseRepo.lagre(
            behandlingId,
            setOf(
                SamordningYtelse(
                    Ytelse.SYKEPENGER,
                    setOf(
                        SamordningYtelsePeriode(
                            Periode(LocalDate.now(), LocalDate.now().plusDays(5)),
                            Prosent(50),
                            0
                        )
                    ),
                    "kilde",
                    "ref"
                )
            )
        )
    }

    private fun opprettSakdata(
        connection: DBConnection,
        foreldrePerson: Person? = null,
        rettighetsPeriode: Periode? = null
    ): FlytKontekstMedPerioder {
        val person = foreldrePerson ?: PersonRepositoryImpl(connection).finnEllerOpprett(listOf(Ident("ident", true)))
        val rettighetsperiode = rettighetsPeriode ?: Periode(LocalDate.now(), LocalDate.now().plusDays(5))
        val sakId = SakRepositoryImpl(connection).finnEllerOpprett(
            person,
            rettighetsperiode
        ).id
        val behandlingId = BehandlingRepositoryImpl(connection).opprettBehandling(
            sakId,
            TypeBehandling.Førstegangsbehandling,
            null,
            vurderingsbehovOgÅrsak = VurderingsbehovOgÅrsak(
                vurderingsbehov = listOf(VurderingsbehovMedPeriode(Vurderingsbehov.MOTTATT_SØKNAD)),
                årsak = ÅrsakTilOpprettelse.SØKNAD,
            ),
        ).id
        return FlytKontekstMedPerioder(
            sakId, behandlingId, null, TypeBehandling.Førstegangsbehandling,
            VurderingType.FØRSTEGANGSBEHANDLING, rettighetsperiode, emptySet()
        )
    }
}