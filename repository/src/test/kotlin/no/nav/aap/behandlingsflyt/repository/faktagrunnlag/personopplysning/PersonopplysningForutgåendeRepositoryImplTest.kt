package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.personopplysning

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.Dødsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.AdresseType
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.FolkeregisterStatus
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonStatus
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonopplysningMedHistorikk
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Statsborgerskap
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.UtenlandsAdresse
import no.nav.aap.behandlingsflyt.help.FakePdlGateway
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
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
import org.junit.jupiter.api.AutoClose
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.time.LocalDate

class PersonopplysningForutgåendeRepositoryImplTest {

    @AutoClose
    private val dataSource = TestDataSource()

    @Test
    fun `lagre, hente ut`() {
        val personopplysningMedHistorikk = PersonopplysningMedHistorikk(
            fødselsdato = Fødselsdato(LocalDate.now().minusYears(18)),
            id = 1,
            dødsdato = Dødsdato(LocalDate.now().plusYears(80)),
            statsborgerskap = listOf(Statsborgerskap("NOR")),
            folkeregisterStatuser = listOf(
                FolkeregisterStatus(
                    status = PersonStatus.bosatt,
                    gyldighetstidspunkt = LocalDate.now(),
                    opphoerstidspunkt = LocalDate.now()
                )
            ),
            utenlandsAddresser = listOf(
                UtenlandsAdresse(
                    gyldigFraOgMed = LocalDate.now().minusYears(1),
                    gyldigTilOgMed = LocalDate.now().minusYears(2),
                    adresseNavn = "En adresse",
                    postkode = "0665",
                    bySted = "Bern",
                    landkode = "SK",
                    adresseType = AdresseType.KONTAKT_ADRESSE
                )
            ),
        )
        val behandling = dataSource.transaction { finnEllerOpprettBehandling(it, sak(it)) }
        dataSource.transaction {
            PersonopplysningForutgåendeRepositoryImpl(it).lagre(behandling.id, personopplysningMedHistorikk)
        }

        val uthentet = dataSource.transaction {
            PersonopplysningForutgåendeRepositoryImpl(it).hentHvisEksisterer(behandling.id)
        }

        assertThat(uthentet!!.brukerPersonopplysning).usingRecursiveComparison().ignoringFields("id")
            .isEqualTo(personopplysningMedHistorikk)

        // SLETT
        assertDoesNotThrow {
            dataSource.transaction {
                PersonopplysningForutgåendeRepositoryImpl(it).slett(behandling.id)
            }
        }
    }

    private fun sak(connection: DBConnection): Sak {
        return PersonOgSakService(
            FakePdlGateway, PersonRepositoryImpl(connection), SakRepositoryImpl(connection)
        ).finnEllerOpprett(ident(), Periode(LocalDate.now(), LocalDate.now().plusYears(3)))
    }
}