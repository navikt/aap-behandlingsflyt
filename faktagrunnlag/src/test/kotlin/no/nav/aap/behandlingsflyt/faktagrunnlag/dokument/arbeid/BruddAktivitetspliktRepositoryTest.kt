package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid

import no.nav.aap.behandlingsflyt.dbtestdata.ident
import no.nav.aap.behandlingsflyt.faktagrunnlag.FakePdlGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.BruddAktivitetsplikt.Paragraf.PARAGRAF_11_7
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.BruddAktivitetsplikt.Type.IKKE_AKTIVT_BIDRAG
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.BruddAktivitetsplikt.Type.IKKE_MØTT_TIL_BEHANDLING
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.verdityper.sakogbehandling.NavIdent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class BruddAktivitetspliktRepositoryTest {
    @Test
    fun `kan lagre brudd på sak`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val sak = nySak(connection)
            val repo = BruddAktivitetspliktRepository(connection)

            nyeBrudd(connection, sak,
                brudd = IKKE_AKTIVT_BIDRAG,
                paragraf = PARAGRAF_11_7,
                begrunnelse = "Orket ikke",
                perioder = listOf(Periode(LocalDate.now(), LocalDate.now().plusDays(5))),
            )

            val lagretHendelse = repo.hentBrudd(sak.id)
            assertEquals(1, lagretHendelse.size)
        }
    }

    @Test
    fun `kan lagre flere hendelser på samme sak hver for seg`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val sak = nySak(connection)
            val repo = BruddAktivitetspliktRepository(connection)
            nyeBrudd(connection, sak,
                brudd = IKKE_MØTT_TIL_BEHANDLING,
                paragraf = PARAGRAF_11_7,
                begrunnelse = "Ville ikke",
                perioder = listOf(Periode(LocalDate.now(), LocalDate.now().plusDays(5))),
            )

            nyeBrudd(connection, sak,
                brudd = IKKE_MØTT_TIL_BEHANDLING,
                paragraf = PARAGRAF_11_7,
                begrunnelse = "Fant ikke fram",
                perioder = listOf(Periode(LocalDate.now().plusDays(5), LocalDate.now().plusDays(10))),
            )

            val lagretHendelse = repo.hentBrudd(sak.id)
            assertEquals(2, lagretHendelse.size)
        }
    }

    @Test
    fun `kan lagre flere hendelser på samme sak samtidig`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val sak = nySak(connection)
            val repo = BruddAktivitetspliktRepository(connection)
            nyeBrudd(connection, sak,
                brudd = IKKE_MØTT_TIL_BEHANDLING,
                paragraf = PARAGRAF_11_7,
                begrunnelse = "Dobbel periode uten oppmøte",
                perioder = listOf(
                    Periode(LocalDate.now(), LocalDate.now().plusDays(5)),
                    Periode(LocalDate.now().plusDays(6), LocalDate.now().plusDays(10)),
                ),
            )
            val lagretHendelse = repo.hentBrudd(sak.id)
            assertEquals(2, lagretHendelse.size)
        }
    }

    @Test
    fun `nytt grunnlag endrer ikke gammelt grunnlag`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val sak = nySak(connection)
            val behandling = BehandlingRepositoryImpl(connection).opprettBehandling(sak.id, listOf(), TypeBehandling.Førstegangsbehandling)

            val førsteBrudd = nyeBrudd(connection, sak).toSet()
            nyttGrunnlag(connection, behandling, førsteBrudd)
            val førsteInnsendingId = førsteBrudd.first().innsendingId

            val andreBrudd = nyeBrudd(connection, sak).toSet()
            nyttGrunnlag(connection, behandling, førsteBrudd + andreBrudd)
            val andreInnsendingId = andreBrudd.first().innsendingId

            val alleGrunnlag = BruddAktivitetspliktRepository(connection).hentAlleGrunnlagKunTestIkkeProd(behandling.id)

            assertEquals(
                setOf(
                    setOf(førsteInnsendingId),
                    setOf(førsteInnsendingId, andreInnsendingId)
                ),
                alleGrunnlag.mapTo(HashSet()) { grunnlag ->
                    grunnlag.bruddene.mapTo(HashSet()) { brudd -> brudd.innsendingId }
                })
        }
    }

}

fun nySak(connection: DBConnection): Sak {
    return PersonOgSakService(connection, FakePdlGateway)
        .finnEllerOpprett(
            ident(),
            Periode(fom = LocalDate.of(2020, 1, 1), tom = LocalDate.of(2020, 2, 2))
        )
}

fun nyeBrudd(
    connection: DBConnection,
    sak: Sak,
    brudd: BruddAktivitetsplikt.Type = IKKE_MØTT_TIL_BEHANDLING,
    paragraf: BruddAktivitetsplikt.Paragraf = PARAGRAF_11_7,
    begrunnelse: String = "En begrunnnelse",
    perioder: List<Periode> = listOf(Periode(LocalDate.now(), LocalDate.now().plusDays(5)))
): List<BruddAktivitetsplikt> {
    val repo = BruddAktivitetspliktRepository(connection)
    val innsendingId = repo.lagreBrudd(
        perioder.map { periode ->
            BruddAktivitetspliktRepository.LagreBruddInput(
                sakId = sak.id,
                brudd = brudd,
                paragraf = paragraf,
                begrunnelse = begrunnelse,
                periode = periode,
                navIdent = NavIdent("Z000000"),
            )
        }
    )
    return repo.hentBruddForInnsending(innsendingId)
}

fun nyttGrunnlag(connection: DBConnection, behandling: Behandling, brudd: Set<BruddAktivitetsplikt>): BruddAktivitetspliktGrunnlag {
    val repo = BruddAktivitetspliktRepository(connection)
    repo.nyttGrunnlag(behandling.id, brudd)
    return repo.hentGrunnlagHvisEksisterer(behandling.id)!!
}
