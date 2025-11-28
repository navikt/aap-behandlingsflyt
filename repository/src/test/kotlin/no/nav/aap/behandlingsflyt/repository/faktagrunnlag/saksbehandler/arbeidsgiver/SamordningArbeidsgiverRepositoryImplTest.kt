package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.arbeidsgiver

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.arbeidsgiver.SamordningArbeidsgiverVurdering
import no.nav.aap.behandlingsflyt.help.FakePdlGateway
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.arbeidsevne.ArbeidsevneRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.desember
import no.nav.aap.behandlingsflyt.test.februar
import no.nav.aap.behandlingsflyt.test.ident
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SamordningArbeidsgiverRepositoryImplTest {
    private lateinit var dataSource: TestDataSource

    @BeforeEach
    fun setUp() {
        dataSource = TestDataSource()
    }

    @AfterEach
    fun tearDown() {
        dataSource.close()
    }

    @Test
    fun `lagre, hent, kopier og slett arbeidsgiver perioder`() {

        val behandling = dataSource.transaction {
            val sak = sak(it, Periode(1 januar 2023, 31 desember 2023))
            finnEllerOpprettBehandling(it, sak)
        }

        val behandling2 = dataSource.transaction {
            val sak = sak(it, Periode(1 januar 2023, 31 desember 2023))
            finnEllerOpprettBehandling(it, sak)
        }

        val perioder = listOf(Periode(1 januar 2023, 1 februar 2023), Periode(2 februar 2023, 12 februar 2023))

        dataSource.transaction {
            val arbeidsgiverRepositoryImpl = SamordningArbeidsgiverRepositoryImpl(it)
            arbeidsgiverRepositoryImpl.lagre(
                sakId = behandling.sakId,
                behandlingId = behandling.id,
                refusjonkravVurderinger = SamordningArbeidsgiverVurdering(
                    begrunnelse = "begrunnelse",
                    perioder = perioder,
                    vurdertAv = "vurdert_av",
                )
            )

            val refusjonskrav = arbeidsgiverRepositoryImpl.hentHvisEksisterer(behandling.id)

            assertNotNull(refusjonskrav)
            assertThat(refusjonskrav?.vurdering?.perioder).isEqualTo(perioder)

            val ingenRefusjonskrav = arbeidsgiverRepositoryImpl.hentHvisEksisterer(behandling2.id)
            assertNull(ingenRefusjonskrav)

            arbeidsgiverRepositoryImpl.kopier(behandling.id, behandling2.id)
            val kopiertRefusjonskrav = arbeidsgiverRepositoryImpl.hentHvisEksisterer(behandling2.id)
            assertThat(kopiertRefusjonskrav).isEqualTo(refusjonskrav)

            arbeidsgiverRepositoryImpl.slett(behandling.id)
            val slettetRefusjonskrav = arbeidsgiverRepositoryImpl.hentHvisEksisterer(behandling.id)
            assertNull(slettetRefusjonskrav)

            val refusjonsKravIkkeSlettet = arbeidsgiverRepositoryImpl.hentHvisEksisterer(behandling2.id)

            assertNotNull(refusjonsKravIkkeSlettet)
            assertThat(refusjonsKravIkkeSlettet).isEqualTo(refusjonskrav)

            val nyePerioder = listOf(Periode(2 februar 2023, 12 februar 2023))

            arbeidsgiverRepositoryImpl.lagre(
                sakId = behandling.sakId,
                behandlingId = behandling2.id,
                refusjonkravVurderinger = SamordningArbeidsgiverVurdering(
                    begrunnelse = "begrunnelse",
                    perioder = nyePerioder,
                    vurdertAv = "vurdert_av",
                )
            )

            val nyeRefusjonskrav = arbeidsgiverRepositoryImpl.hentHvisEksisterer(behandling2.id)
            assertThat(nyeRefusjonskrav).isNotEqualTo(refusjonskrav)
            assertThat(nyeRefusjonskrav?.vurdering?.perioder).isEqualTo(nyePerioder)

        }

    }


    private fun sak(connection: DBConnection, periode: Periode): Sak {
        return PersonOgSakService(
            FakePdlGateway,
            PersonRepositoryImpl(connection),
            SakRepositoryImpl(connection)
        ).finnEllerOpprett(ident(), periode)
    }
}