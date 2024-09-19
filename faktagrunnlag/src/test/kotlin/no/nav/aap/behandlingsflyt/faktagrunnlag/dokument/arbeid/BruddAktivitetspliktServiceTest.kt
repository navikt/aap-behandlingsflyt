package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid

import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottaDokumentService
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.StrukturertDokument
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.dokumenter.Brevkode
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.komponenter.httpklient.json.DefaultJsonMapper
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.verdityper.dokument.JournalpostId
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.time.LocalDate
import java.time.LocalDateTime

class BruddAktivitetspliktServiceTest {
    @Test
    fun `detekterer nye dokumenter og legger dem til i grunnlaget`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val sak = nySak(connection)
            val repo = BruddAktivitetspliktRepository(connection)

            val brudd = nyeBrudd(connection, sak,
                brudd = AktivitetType.IKKE_AKTIVT_BIDRAG,
                paragraf = Paragraf.PARAGRAF_11_7,
                begrunnelse = "Orket ikke",
                perioder = listOf(Periode(LocalDate.now(), LocalDate.now().plusDays(5))),
            ).first()

            val dokumentRepo = MottaDokumentService(MottattDokumentRepository(connection))
            val journalpostId = JournalpostId(brudd.innsendingId.toString())
            val dokument = StrukturertDokument(DefaultJsonMapper.toJson(LocalDateTime.now()), Brevkode.AKTIVITETSKORT)
            dokumentRepo.mottattDokument(journalpostId, sak.id, brudd.opprettetTid, dokument.brevkode, dokument)

            val lagretHendelse = repo.hentBrudd(sak.id)
            assertEquals(1, lagretHendelse.size)
        }

    }
}