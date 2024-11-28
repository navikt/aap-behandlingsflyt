package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid

import no.nav.aap.behandlingsflyt.test.ident
import no.nav.aap.behandlingsflyt.faktagrunnlag.FakePdlGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Brudd.Paragraf.PARAGRAF_11_7
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Brudd.Paragraf.PARAGRAF_11_8
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Brudd.Paragraf.PARAGRAF_11_9
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.BruddType.IKKE_AKTIVT_BIDRAG
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.BruddType.IKKE_MØTT_TIL_BEHANDLING_ELLER_UTREDNING
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.komponenter.httpklient.auth.Bruker
import no.nav.aap.komponenter.type.Periode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class AktivitetspliktRepositoryTest {
    @Test
    fun `kan lagre feilregistrering brudd på sak`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val sak = nySak(connection)
            val repo = AktivitetspliktRepository(connection)
            val periode = Periode(LocalDate.now(), LocalDate.now().plusDays(5))

            val input = nyeFeilregistrering(
                connection, sak,
                bruddType = IKKE_AKTIVT_BIDRAG,
                paragraf = PARAGRAF_11_7,
                begrunnelse = "",
                periode = periode,
            ).first()

            val lagretHendelse = repo.hentBrudd(sak.id)
            assertEquals(1, lagretHendelse.size)
            lagretHendelse[0].also {
                it as AktivitetspliktFeilregistrering
                assertEquals(IKKE_AKTIVT_BIDRAG, it.brudd.bruddType)
                assertEquals(PARAGRAF_11_7, it.brudd.paragraf)
                assertEquals("", it.begrunnelse)
                assertEquals(periode, it.brudd.periode)
            }

            repo.hentBrudd(input.brudd).also {
                val dokument = it.first()
                dokument as AktivitetspliktFeilregistrering
                assertEquals(IKKE_AKTIVT_BIDRAG, dokument.brudd.bruddType)
                assertEquals(PARAGRAF_11_7, dokument.brudd.paragraf)
                assertEquals("", dokument.begrunnelse)
                assertEquals(periode, dokument.brudd.periode)
            }
        }
    }

    @Test
    fun `kan lagre brudd på sak`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val sak = nySak(connection)
            val repo = AktivitetspliktRepository(connection)
            val periode = Periode(LocalDate.now(), LocalDate.now().plusDays(5))

            nyeBrudd(
                connection, sak,
                bruddType = IKKE_AKTIVT_BIDRAG,
                paragraf = PARAGRAF_11_7,
                begrunnelse = "Orket ikke",
                perioder = listOf(periode),
            )

            val lagretHendelse = repo.hentBrudd(sak.id)
            assertEquals(1, lagretHendelse.size)
            lagretHendelse[0].also {
                it as AktivitetspliktRegistrering
                assertEquals(IKKE_AKTIVT_BIDRAG, it.brudd.bruddType)
                assertEquals(PARAGRAF_11_7, it.brudd.paragraf)
                assertEquals("Orket ikke", it.begrunnelse)
                assertEquals(periode, it.brudd.periode)
            }
        }
    }

    @Test
    fun `kan lagre flere hendelser på samme sak hver for seg`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val sak = nySak(connection)
            val repo = AktivitetspliktRepository(connection)
            nyeBrudd(
                connection, sak,
                bruddType = IKKE_MØTT_TIL_BEHANDLING_ELLER_UTREDNING,
                paragraf = PARAGRAF_11_8,
                begrunnelse = "Ville ikke",
                perioder = listOf(Periode(LocalDate.now(), LocalDate.now().plusDays(5))),
            )

            nyeBrudd(
                connection, sak,
                bruddType = IKKE_MØTT_TIL_BEHANDLING_ELLER_UTREDNING,
                paragraf = PARAGRAF_11_9,
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
            val repo = AktivitetspliktRepository(connection)
            nyeBrudd(
                connection, sak,
                bruddType = IKKE_MØTT_TIL_BEHANDLING_ELLER_UTREDNING,
                paragraf = PARAGRAF_11_8,
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
            val behandling = BehandlingRepositoryImpl(connection).opprettBehandling(
                sak.id,
                listOf(),
                TypeBehandling.Førstegangsbehandling,
                null
            )

            val førsteBrudd = nyeBrudd(connection, sak).toSet()
            nyttGrunnlag(connection, behandling, førsteBrudd)
            val førsteInnsendingId = førsteBrudd.first().metadata.id

            val andreBrudd = nyeBrudd(connection, sak).toSet()
            nyttGrunnlag(connection, behandling, førsteBrudd + andreBrudd)
            val andreInnsendingId = andreBrudd.first().metadata.id

            val alleGrunnlag = AktivitetspliktRepository(connection).hentAlleGrunnlagKunTestIkkeProd(behandling.id)

            assertEquals(
                setOf(
                    setOf(førsteInnsendingId),
                    setOf(førsteInnsendingId, andreInnsendingId)
                ),
                alleGrunnlag
            )
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
    bruddType: BruddType = IKKE_MØTT_TIL_BEHANDLING_ELLER_UTREDNING,
    paragraf: Brudd.Paragraf = PARAGRAF_11_8,
    grunn: Grunn = Grunn.INGEN_GYLDIG_GRUNN,
    begrunnelse: String = "En begrunnnelse",
    perioder: List<Periode> = listOf(Periode(LocalDate.now(), LocalDate.now().plusDays(5))),
): List<AktivitetspliktDokument> {
    val repo = AktivitetspliktRepository(connection)
    val innsendingId = repo.lagreBrudd(
        perioder.map { periode ->
            AktivitetspliktRepository.RegistreringInput(
                brudd = Brudd(
                    sakId = sak.id,
                    bruddType = bruddType,
                    paragraf = paragraf,
                    periode = periode,
                ),
                begrunnelse = begrunnelse,
                innsender = Bruker("Z000000"),
                grunn = grunn
            )
        }
    )
    return repo.hentBruddForInnsending(innsendingId)
}

fun nyeFeilregistrering(
    connection: DBConnection,
    sak: Sak,
    bruddType: BruddType = IKKE_MØTT_TIL_BEHANDLING_ELLER_UTREDNING,
    paragraf: Brudd.Paragraf = PARAGRAF_11_8,
    begrunnelse: String = "En begrunnnelse",
    periode: Periode = Periode(LocalDate.now(), LocalDate.now().plusDays(5)),
): List<AktivitetspliktDokument> {
    val repo = AktivitetspliktRepository(connection)
    val innsendingId = repo.lagreBrudd(
        listOf(
            AktivitetspliktRepository.FeilregistreringInput(
                brudd = Brudd(
                    sakId = sak.id,
                    bruddType = bruddType,
                    paragraf = paragraf,
                    periode = periode,
                ),
                begrunnelse = begrunnelse,
                innsender = Bruker("Z000000"),
            )
        )
    )
    return repo.hentBruddForInnsending(innsendingId)
}

fun nyttGrunnlag(
    connection: DBConnection,
    behandling: Behandling,
    brudd: Set<AktivitetspliktDokument>
): AktivitetspliktGrunnlag {
    val repo = AktivitetspliktRepository(connection)
    repo.nyttGrunnlag(behandling.id, brudd)
    return repo.hentGrunnlagHvisEksisterer(behandling.id)!!
}
