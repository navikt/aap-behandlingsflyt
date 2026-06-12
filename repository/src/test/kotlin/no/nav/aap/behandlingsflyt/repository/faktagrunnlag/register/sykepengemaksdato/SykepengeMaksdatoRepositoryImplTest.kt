package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.register.sykepengemaksdato

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.sykepengemaksdato.MaksdatoHendelse
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.sykepengemaksdato.MaksdatoHendelseKilde
import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SykepengeMaksdatoRepositoryImplTest {
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
    fun `lagrer og henter sykepengemaksdato for person`() {
        dataSource.transaction { connection ->
            val repository = SykepengeMaksdatoRepositoryImpl(connection)
            val person = PersonRepositoryImpl(connection).finnEllerOpprett(listOf(Ident("12345")))

            val maksdatoHendelse = MaksdatoHendelse(
                personId = person.id,
                foreløpigMaksdato = LocalDate.now().plusMonths(3),
                kilde = MaksdatoHendelseKilde.SPEIL
            )

            repository.lagre(maksdatoHendelse, person)

            val hentetHendelse = repository.hentHvisEksisterer(person)

            assertThat(hentetHendelse).isNotNull()
            assertThat(hentetHendelse?.foreløpigMaksdato).isEqualTo(maksdatoHendelse.foreløpigMaksdato)
            assertThat(hentetHendelse?.kilde).isEqualTo(MaksdatoHendelseKilde.SPEIL)
        }
    }

    @Test
    fun `lagring av ny hendelse overskriver gammel hendelse`() {
        dataSource.transaction { connection ->
            val repository = SykepengeMaksdatoRepositoryImpl(connection)
            val person = PersonRepositoryImpl(connection).finnEllerOpprett(listOf(Ident("11111")))

            val førsteMaksdato = MaksdatoHendelse(
                personId = person.id,
                foreløpigMaksdato = LocalDate.of(2025, 6, 1),
                kilde = MaksdatoHendelseKilde.SPEIL
            )
            repository.lagre(førsteMaksdato, person)

            val andreMaksdato = MaksdatoHendelse(
                personId = person.id,
                foreløpigMaksdato = LocalDate.of(2025, 9, 1),
                kilde = MaksdatoHendelseKilde.SPEIL
            )
            repository.lagre(andreMaksdato, person)

            val hentet = repository.hentHvisEksisterer(person)

            assertThat(hentet).isNotNull()
            assertThat(hentet?.foreløpigMaksdato).isEqualTo(andreMaksdato.foreløpigMaksdato)
        }
    }

    @Test
    fun `hentHvisEksisterer returnerer null når maksdato ikke finnes for person`() {
        dataSource.transaction { connection ->
            val repository = SykepengeMaksdatoRepositoryImpl(connection)
            val person = PersonRepositoryImpl(connection).finnEllerOpprett(listOf(Ident("99999")))

            val hentet = repository.hentHvisEksisterer(person)

            assertThat(hentet).isNull()
        }
    }
}