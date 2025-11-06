package no.nav.aap.behandlingsflyt.repository.klage

import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingReferanse
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.formkrav.FormkravVurdering
import no.nav.aap.behandlingsflyt.help.FakePdlGateway
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.klage.FormkravRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.ident
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.util.*

internal class FormkravRepositoryImplTest {
    companion object {
        private val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))

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
    fun `Lagrer og henter formkrav uten varsel`() {
        dataSource.transaction { connection ->

            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)

            val formkravRepository = FormkravRepositoryImpl(connection)
            val formkrav = FormkravVurdering(
                begrunnelse = "Begrunnelse",
                erBrukerPart = true,
                erFristOverholdt = true,
                erKonkret = true,
                erSignert = true,
                vurdertAv = "ident",
                opprettet = Instant.parse("2023-01-01T12:00:00Z"),
                likevelBehandles = null
            )


            formkravRepository.lagre(behandling.id, formkrav)
            val grunnlag = formkravRepository.hentHvisEksisterer(behandling.id)

            assertThat(grunnlag?.vurdering).isEqualTo(formkrav)
        }
    }

    @Test
    fun `Lagrer og henter formkrav med varsel`() {
        dataSource.transaction { connection ->

            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)

            val formkravRepository = FormkravRepositoryImpl(connection)
            val formkrav = FormkravVurdering(
                begrunnelse = "Begrunnelse",
                erBrukerPart = true,
                erFristOverholdt = true,
                erKonkret = true,
                erSignert = true,
                vurdertAv = "ident",
                opprettet = Instant.parse("2023-01-01T12:00:00Z"),
                likevelBehandles = null
            )

            val varselSendt = LocalDate.now()
            val varselFrist = varselSendt.plusWeeks(3)
            val brevReferanse = BrevbestillingReferanse(UUID.randomUUID())

            formkravRepository.lagre(behandling.id, formkrav)
            formkravRepository.lagreVarsel(
                behandlingId = behandling.id,
                varsel = brevReferanse
            )
            formkravRepository.lagreFrist(
                behandlingId = behandling.id,
                datoVarslet = varselSendt,
                svarfrist = varselFrist
            )

            val grunnlag = formkravRepository.hentHvisEksisterer(behandling.id)

            assertThat(grunnlag?.vurdering).isEqualTo(formkrav)
            assertThat(grunnlag?.varsel?.svarfrist).isEqualTo(varselFrist)
            assertThat(grunnlag?.varsel?.sendtDato).isEqualTo(varselSendt)
            assertThat(grunnlag?.varsel?.varselId).isEqualTo(brevReferanse)
        }
    }

    private fun sak(connection: DBConnection): Sak {
        return PersonOgSakService(
            FakePdlGateway,
            PersonRepositoryImpl(connection),
            SakRepositoryImpl(connection)
        ).finnEllerOpprett(ident(), periode)
    }
}
