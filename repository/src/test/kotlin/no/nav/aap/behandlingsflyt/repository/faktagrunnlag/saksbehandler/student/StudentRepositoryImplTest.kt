package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.student

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.ErStudentStatus
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.OppgittStudent
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.SkalGjenopptaStudieStatus
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentVurdering
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.repository.avklaringsbehov.FakePdlGateway
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
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class StudentRepositoryImplTest {
    @Test
    fun `lagre, hente ut, slette`() {
        val dataSource = InitTestDatabase.freshDatabase()

        val sak = dataSource.transaction { sak(it) }

        val behandling = dataSource.transaction {
            finnEllerOpprettBehandling(it, sak)
        }


        val periode = Periode(1 januar 2022, 31.desember(2023))

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
            begrunnelse = "xasdasd",
            harAvbruttStudie = true,
            godkjentStudieAvLånekassen = true,
            avbruttPgaSykdomEllerSkade = false,
            harBehovForBehandling = true,
            avbruttStudieDato = 13 februar 1989,
            avbruddMerEnn6Måneder = true,
            vurdertAv = "Gokken Gokkestad",
        )
        dataSource.transaction {
            StudentRepositoryImpl(it).lagre(
                behandling.id, studentvurdering
            )
        }

        val uthentet = dataSource.transaction {
            StudentRepositoryImpl(it).hent(behandling.id)
        }
        assertThat(uthentet.studentvurdering)
            .usingRecursiveComparison().ignoringFields("id", "vurdertTidspunkt")
            .isEqualTo(studentvurdering)
        assertThat(uthentet.oppgittStudent)
            .usingRecursiveComparison().ignoringFields("id", "vurdertTidspunkt")
            .isEqualTo(oppgittStudent)

        // SLETT

        dataSource.transaction {
            StudentRepositoryImpl(it).slett(behandling.id)
        }
    }


    private fun sak(connection: DBConnection): Sak {
        return PersonOgSakService(
            FakePdlGateway,
            PersonRepositoryImpl(connection),
            SakRepositoryImpl(connection)
        ).finnEllerOpprett(ident(), Periode(1 januar 2022, 31.desember(2023)))
    }
}