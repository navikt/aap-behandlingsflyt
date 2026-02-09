package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.student

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.ErStudentStatus
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.OppgittStudent
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.SkalGjenopptaStudieStatus
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentVurdering
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.help.sak
import no.nav.aap.behandlingsflyt.test.april
import no.nav.aap.behandlingsflyt.test.desember
import no.nav.aap.behandlingsflyt.test.februar
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class StudentRepositoryImplTest {
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
    fun `lagre, hente ut, slette`() {
        val sak = dataSource.transaction { sak(it, Periode(1 januar 2022, 31.desember(2023))) }

        val behandling = dataSource.transaction {
            finnEllerOpprettBehandling(it, sak)
        }

        val oppgittStudent = OppgittStudent(
            avbruttDato = 2 januar 2023,
            erStudentStatus = ErStudentStatus.JA,
            skalGjenopptaStudieStatus = SkalGjenopptaStudieStatus.VET_IKKE
        )
        dataSource.transaction {
            StudentRepositoryImpl(it).lagre(
                behandling.id, oppgittStudent
            )
        }
        val studentvurdering = StudentVurdering(
            fom = 1 januar 2020,
            tom = 1 april 2020,
            begrunnelse = "xasdasd",
            harAvbruttStudie = true,
            godkjentStudieAvLånekassen = true,
            avbruttPgaSykdomEllerSkade = false,
            harBehovForBehandling = true,
            avbruttStudieDato = 13 februar 1989,
            avbruddMerEnn6Måneder = true,
            vurdertAv = "Gokken Gokkestad",
            vurdertTidspunkt = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS),
            vurdertIBehandling = behandling.id
        )
        val studentvurdering2 = StudentVurdering(
            fom = 2 april 2020,
            tom = null,
            begrunnelse = "Nei",
            harAvbruttStudie = false,
            godkjentStudieAvLånekassen = null,
            avbruttPgaSykdomEllerSkade = null,
            harBehovForBehandling = null,
            avbruttStudieDato = null,
            avbruddMerEnn6Måneder = true,
            vurdertAv = "Gokken Gokkestad",
            vurdertTidspunkt = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS),
            vurdertIBehandling = behandling.id
        )
        
        dataSource.transaction {
            StudentRepositoryImpl(it).lagre(
                behandling.id, setOf(studentvurdering, studentvurdering2)
            )
        }

        val uthentet = dataSource.transaction {
            StudentRepositoryImpl(it).hent(behandling.id)
        }
        assertThat(uthentet.vurderinger).hasSize(2)
        assertThat(uthentet.vurderinger!!.first{it.fom.isEqual(1 januar 2020)})
            .usingRecursiveComparison().ignoringFields("id")
            .isEqualTo(studentvurdering)
        assertThat(uthentet.vurderinger!!.first{it.fom.isEqual(2 april 2020)})
            .usingRecursiveComparison().ignoringFields("id")
            .isEqualTo(studentvurdering2)
        assertThat(uthentet.oppgittStudent)
            .usingRecursiveComparison().ignoringFields("id")
            .isEqualTo(oppgittStudent)

        // SLETT

        dataSource.transaction {
            StudentRepositoryImpl(it).slett(behandling.id)
        }
    }
}