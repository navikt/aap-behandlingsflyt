package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.samordning.ytelsesvurdering

import no.nav.aap.behandlingsflyt.behandling.samordning.Ytelse
import no.nav.aap.behandlingsflyt.behandling.søknad.TrukketSøknadService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningVurderingGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningVurderingPeriode
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.repository.avklaringsbehov.AvklaringsbehovRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.avklaringsbehov.FakePdlGateway
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.søknad.TrukketSøknadRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.ident
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryAvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryTrukketSøknadRepository
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Prosent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.time.LocalDate


internal class SamordningVurderingRepositoryImplTest {
    private val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))

    private val dataSource = InitTestDatabase.freshDatabase()

    @Test
    fun `lagre og hente ut igjen`() {
        val behandling = dataSource.transaction { finnEllerOpprettBehandling(it, sak(it)) }

        // Lagre vurdering
        val vurdering = SamordningVurdering(
            ytelseType = Ytelse.SYKEPENGER,
            vurderingPerioder = listOf(
                SamordningVurderingPeriode(
                    periode = Periode(LocalDate.now().minusYears(3), LocalDate.now().minusDays(1)),
                    gradering = Prosent(40),
                    kronesum = null,
                    manuell = false,
                ),
                SamordningVurderingPeriode(
                    periode = Periode(LocalDate.now().minusYears(6), LocalDate.now().minusYears(5)),
                    gradering = Prosent(40),
                    kronesum = null,
                    manuell = false,
                )
            )
        )
        // Lagre vurdering
        val vurdering2 = SamordningVurdering(
            ytelseType = Ytelse.OPPLÆRINGSPENGER,
            vurderingPerioder = listOf(
                SamordningVurderingPeriode(
                    periode = Periode(LocalDate.now().minusYears(3), LocalDate.now().minusDays(1)),
                    gradering = Prosent(40),
                    kronesum = null,
                    manuell = false,
                ),
                SamordningVurderingPeriode(
                    periode = Periode(LocalDate.now().minusYears(6), LocalDate.now().minusYears(5)),
                    gradering = Prosent(40),
                    kronesum = null,
                    manuell = false,
                )
            )
        )
        dataSource.transaction {
            SamordningVurderingRepositoryImpl(it).lagreVurderinger(
                behandlingId = behandling.id,
                samordningVurderinger = SamordningVurderingGrunnlag(
                    begrunnelse = "En god begrunnelse",
                    maksDatoEndelig = false,
                    maksDato = LocalDate.now().plusYears(1),
                    vurderinger = listOf(vurdering, vurdering2)
                )
            )
        }

        val uthentet = dataSource.transaction {
            SamordningVurderingRepositoryImpl(it).hentHvisEksisterer(behandling.id)
        }

        assertThat(uthentet?.vurderinger).containsExactlyInAnyOrder(vurdering, vurdering2)
    }

    @Test
    fun `lagre en vurdering uten perioder`() {
        val behandling = dataSource.transaction { finnEllerOpprettBehandling(it, sak(it)) }

        dataSource.transaction {
            SamordningVurderingRepositoryImpl(it).lagreVurderinger(behandling.id, SamordningVurderingGrunnlag(
                begrunnelse = "xxxx",
                maksDatoEndelig = true,
                maksDato = LocalDate.now().plusYears(1),
                vurderinger = listOf()
            )
            )
        }

        val uthentet = dataSource.transaction {
            SamordningVurderingRepositoryImpl(it).hentHvisEksisterer(behandling.id)
        }

        assertThat(uthentet?.begrunnelse).isEqualTo("xxxx")
        assertThat(uthentet?.maksDatoEndelig).isTrue()
        assertThat(uthentet?.maksDato).isEqualTo(LocalDate.now().plusYears(1))
    }

    @Test
    fun `å lagre en vurdering før ytelse eksisterer gir ikke feil`() {
        val behandling = dataSource.transaction {
            finnEllerOpprettBehandling(it, sak(it))
        }

        // Lagre vurdering
        val vurdering = SamordningVurdering(
            ytelseType = Ytelse.SYKEPENGER,

            vurderingPerioder = listOf(
                SamordningVurderingPeriode(
                    periode = Periode(LocalDate.now().minusYears(3), LocalDate.now().minusDays(1)),
                    gradering = Prosent(40),
                    kronesum = null,
                    manuell = false,
                )
            )
        )
        assertDoesNotThrow {
            dataSource.transaction {
                SamordningVurderingRepositoryImpl(it).lagreVurderinger(
                    behandlingId = behandling.id,
                    samordningVurderinger = SamordningVurderingGrunnlag(
                        begrunnelse = "En god begrunnelse",
                        maksDatoEndelig = false,
                        maksDato = LocalDate.now().plusYears(1),
                        vurderinger = listOf(vurdering)
                    )
                )
            }
        }
    }

    @Test
    fun `lagre flere vurderinger og verifiser at nyeste hentes ut`() {
        val behandling = dataSource.transaction { finnEllerOpprettBehandling(it, sak(it)) }

        // Create fixed dates for the periods to avoid test flakiness
        val førstePeriodeStart = LocalDate.of(2022, 1, 1)
        val førstePeriodeEnd = LocalDate.of(2023, 1, 1)
        val andrePeriodeStart = LocalDate.of(2024, 1, 1)
        val andrePeriodeEnd = LocalDate.of(2025, 1, 1)

        // Create the first vurdering
        val førsteVurdering = SamordningVurdering(
            ytelseType = Ytelse.SYKEPENGER,
            vurderingPerioder = listOf(
                SamordningVurderingPeriode(
                    periode = Periode(førstePeriodeStart, førstePeriodeEnd),
                    gradering = Prosent(40),
                    kronesum = null,
                    manuell = false,
                )
            )
        )

        // Save the first vurdering
        dataSource.transaction {
            SamordningVurderingRepositoryImpl(it).lagreVurderinger(
                behandlingId = behandling.id,
                samordningVurderinger = SamordningVurderingGrunnlag(
                    begrunnelse = "Første begrunnelse",
                    maksDatoEndelig = false,
                    maksDato = LocalDate.of(2025, 1, 1),
                    vurderinger = listOf(førsteVurdering)
                )
            )
        }

        // Create the second vurdering
        val andreVurdering1 = SamordningVurdering(
            ytelseType = Ytelse.FORELDREPENGER,
            vurderingPerioder = listOf(
                SamordningVurderingPeriode(
                    periode = Periode(andrePeriodeStart, andrePeriodeEnd),
                    gradering = Prosent(50),
                    manuell = false,
                )
            )
        )
        val andreVurdering2 = SamordningVurdering(
            ytelseType = Ytelse.OPPLÆRINGSPENGER,
            vurderingPerioder = listOf(
                SamordningVurderingPeriode(
                    periode = Periode(andrePeriodeStart.plusDays(1), andrePeriodeStart.plusMonths(6)),
                    gradering = Prosent(30),
                    manuell = false,
                ),
                SamordningVurderingPeriode(
                    periode = Periode(andrePeriodeStart.plusMonths(7), andrePeriodeEnd.plusDays(6)),
                    gradering = Prosent(33),
                    manuell = false,
                )
            )
        )

        // Save the second vurdering
        val andreBegrunnelse = "Andre begrunnelse"
        val andreMaksDato = LocalDate.of(2026, 1, 1)

        dataSource.transaction {
            SamordningVurderingRepositoryImpl(it).lagreVurderinger(
                behandlingId = behandling.id,
                samordningVurderinger = SamordningVurderingGrunnlag(
                    begrunnelse = andreBegrunnelse,
                    maksDatoEndelig = true,
                    maksDato = andreMaksDato,
                    vurderinger = listOf(andreVurdering1, andreVurdering2)
                )
            )
        }

        // Retrieve the vurdering
        val uthentet = dataSource.transaction {
            SamordningVurderingRepositoryImpl(it).hentHvisEksisterer(behandling.id)
        }!!

        // Verify that the retrieved vurdering has the expected properties
        assertThat(uthentet.begrunnelse).isEqualTo(andreBegrunnelse)
        assertThat(uthentet.maksDatoEndelig).isTrue()
        assertThat(uthentet.maksDato).isEqualTo(andreMaksDato)

        // Verify that the retrieved vurdering has the expected number of vurderinger
        assertThat(uthentet.vurderinger).hasSize(2)

        // Verify that the retrieved vurderinger have the expected ytelse types
        val actualVurderinger = uthentet.vurderinger

        // Find the vurderinger by their ytelse types
        val foreldrepengerVurdering = actualVurderinger.find { it.ytelseType == Ytelse.FORELDREPENGER }
        val opplæringspengerVurdering = actualVurderinger.find { it.ytelseType == Ytelse.OPPLÆRINGSPENGER }

        // Verify that both vurderinger were found
        assertThat(foreldrepengerVurdering).isNotNull()
        assertThat(opplæringspengerVurdering).isNotNull()

        // Verify the first vurdering (FORELDREPENGER)
        assertThat(foreldrepengerVurdering?.ytelseType).isEqualTo(Ytelse.FORELDREPENGER)
        assertThat(foreldrepengerVurdering?.vurderingPerioder).hasSize(1)

        // Verify the properties of the first vurdering's periode
        val foreldrepengerPeriode = foreldrepengerVurdering?.vurderingPerioder?.firstOrNull()
        assertThat(foreldrepengerPeriode?.periode?.fom).isEqualTo(andrePeriodeStart)
        assertThat(foreldrepengerPeriode?.periode?.tom).isEqualTo(andrePeriodeEnd)
        assertThat(foreldrepengerPeriode?.gradering?.prosentverdi()).isEqualTo(50)
        assertThat(foreldrepengerPeriode?.kronesum).isNull()
        assertThat(foreldrepengerPeriode?.manuell).isFalse()

        // Verify the second vurdering (OPPLÆRINGSPENGER)
        assertThat(opplæringspengerVurdering?.ytelseType).isEqualTo(Ytelse.OPPLÆRINGSPENGER)
        assertThat(opplæringspengerVurdering?.vurderingPerioder).hasSize(2)

        // Verify the properties of the second vurdering's first periode
        val opplæringspengerPeriode1 = opplæringspengerVurdering?.vurderingPerioder?.get(0)!!
        assertThat(opplæringspengerPeriode1.periode.fom).isEqualTo(andrePeriodeStart.plusDays(1))
        assertThat(opplæringspengerPeriode1.periode.tom).isEqualTo(andrePeriodeStart.plusMonths(6))
        assertThat(opplæringspengerPeriode1.gradering?.prosentverdi()).isEqualTo(30)
        assertThat(opplæringspengerPeriode1.kronesum).isNull()
        assertThat(opplæringspengerPeriode1.manuell).isFalse()

        // Verify the properties of the second vurdering's second periode
        val opplæringspengerPeriode2 = opplæringspengerVurdering.vurderingPerioder[1]
        assertThat(opplæringspengerPeriode2.periode.fom).isEqualTo(andrePeriodeStart.plusMonths(7))
        assertThat(opplæringspengerPeriode2.periode.tom).isEqualTo(andrePeriodeEnd.plusDays(6))
        assertThat(opplæringspengerPeriode2.gradering?.prosentverdi()).isEqualTo(33)
        assertThat(opplæringspengerPeriode2.kronesum).isNull()
        assertThat(opplæringspengerPeriode2.manuell).isFalse()
    }

    private fun sak(connection: DBConnection): Sak {
        return PersonOgSakService(
            FakePdlGateway,
            PersonRepositoryImpl(connection),
            SakRepositoryImpl(connection),
            BehandlingRepositoryImpl(connection),
            TrukketSøknadService(
                AvklaringsbehovRepositoryImpl(connection),
                TrukketSøknadRepositoryImpl(connection)
            ),
        ).finnEllerOpprett(
            ident(),
            periode
        )
    }
}
