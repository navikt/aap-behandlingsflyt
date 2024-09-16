package no.nav.aap.behandlingsflyt.faktagrunnlag.bruddaktivitetsplikt

import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.komponenter.type.Periode
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.time.LocalDate

@Disabled
class BruddAktivitetspliktRepositoryTest() {
    @Disabled
    @Test
    fun `kanLagreHendelser` () {
        val saksnummer = "mittSaksnummer"
        InitTestDatabase.dataSource.transaction { connection ->
            val repo = BruddAktivitetspliktRepository(connection)

            repo.lagreBruddAktivitetspliktHendelse(
                BruddAktivitetspliktRequest(
                    saksnummer,
                    AktivitetTypeDto.IKKE_AKTIVT_BIDRAG,
                    ParagrafDto.PARAGRAF_11_7,
                    "Orket ikke",
                    listOf(
                        Periode(LocalDate.now(), LocalDate.now().plusDays(5))
                    ),
                )
            )

            val lagretHendelse = repo.hentBruddAktivitetspliktHendelser(saksnummer)
            assertEquals(1, lagretHendelse.size)
        }
    }

    @Disabled
    @Test
    fun `kanLagreFlereHendelserPåSammeSak` () {
        val saksnummer = "saksnummerSammeSak"
        InitTestDatabase.dataSource.transaction { connection ->
            val repo = BruddAktivitetspliktRepository(connection)
            repo.lagreBruddAktivitetspliktHendelse(
                BruddAktivitetspliktRequest(
                    saksnummer,
                    AktivitetTypeDto.IKKE_MØTT_TIL_BEHANDLING,
                    ParagrafDto.PARAGRAF_11_7,
                    "Ville ikke",
                    listOf(
                        Periode(LocalDate.now(), LocalDate.now().plusDays(5))
                    ),
                )
            )

            repo.lagreBruddAktivitetspliktHendelse(
                BruddAktivitetspliktRequest(
                    saksnummer,
                    AktivitetTypeDto.IKKE_MØTT_TIL_BEHANDLING,
                    ParagrafDto.PARAGRAF_11_7,
                    "Fant ikke fram",
                    listOf(
                        Periode(LocalDate.now().plusDays(5), LocalDate.now().plusDays(10))
                    ),
                )
            )

            val lagretHendelse = repo.hentBruddAktivitetspliktHendelser(saksnummer)
            assertEquals(2, lagretHendelse.size)
        }
    }

    @Disabled
    @Test
    fun `vilIkkeLagreMedFeilInput` () {
        val saksnummer = "myIkkeLagreSaksnummer"
        InitTestDatabase.dataSource.transaction { connection ->
            val repo = BruddAktivitetspliktRepository(connection)
            repo.lagreBruddAktivitetspliktHendelse(
                BruddAktivitetspliktRequest(
                    saksnummer,
                    AktivitetTypeDto.IKKE_AKTIVT_BIDRAG,
                    ParagrafDto.PARAGRAF_11_8,
                    "Skal ikke lagres",
                    listOf(),
                )
            )

            val lagretHendelse = repo.hentBruddAktivitetspliktHendelser(saksnummer)
            assertEquals(0, lagretHendelse.size)
        }
    }

    @Disabled
    @Test
    fun `lagrerToHendelserNårFlerePerioderSettes` () {
        val saksnummer = "myDobbelPeriodeSak"
        InitTestDatabase.dataSource.transaction { connection ->
            val repo = BruddAktivitetspliktRepository(connection)
            repo.lagreBruddAktivitetspliktHendelse(
                BruddAktivitetspliktRequest(
                    saksnummer,
                    AktivitetTypeDto.IKKE_MØTT_TIL_BEHANDLING,
                    ParagrafDto.PARAGRAF_11_7,
                    "Dobbel periode uten oppmøte",
                    listOf(
                        Periode(LocalDate.now(), LocalDate.now().plusDays(5)),
                        Periode(LocalDate.now().plusDays(6), LocalDate.now().plusDays(10))
                    ),
                )
            )

            val lagretHendelse = repo.hentBruddAktivitetspliktHendelser(saksnummer)
            assertEquals(2, lagretHendelse.size)
        }
    }
}