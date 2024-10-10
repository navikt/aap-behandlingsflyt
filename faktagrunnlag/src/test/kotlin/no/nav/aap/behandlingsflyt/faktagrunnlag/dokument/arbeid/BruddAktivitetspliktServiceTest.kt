package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid

import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottaDokumentService
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentReferanse
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.StrukturertDokument
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.BruddAktivitetsplikt.Paragraf.PARAGRAF_11_7
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.BruddAktivitetsplikt.Type.IKKE_AKTIVT_BIDRAG
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.dokumenter.Brevkode
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.verdityper.flyt.FlytKontekstMedPerioder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class BruddAktivitetspliktServiceTest {
    @Test
    fun `detekterer nye dokumenter og legger dem til i grunnlaget`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val sak = nySak(connection)
            val behandling = BehandlingRepositoryImpl(connection).opprettBehandling(sak.id, listOf(), TypeBehandling.Førstegangsbehandling, null)
            val bruddAktivitetspliktService = BruddAktivitetspliktService.konstruer(connection)
            val flytKontekst = flytKontekstMedPerioder(behandling)

            nyeBrudd(
                connection, sak,
                brudd = IKKE_AKTIVT_BIDRAG,
                paragraf = PARAGRAF_11_7,
                begrunnelse = "Orket ikke",
                perioder = listOf(Periode(LocalDate.now(), LocalDate.now().plusDays(5))),
            ).first().also {
                mottattDokument(connection, it, sak)
            }


            // Før vi oppdaterer kravinformasjonen, så finnes det ingen grunnlag
            BruddAktivitetspliktRepository(connection).hentGrunnlagHvisEksisterer(behandling.id).also {
                assertNull(it)
            }

            // Etter første oppdatering av kravinformasjonen, skal bruddet vi la inn over dukke opp
            bruddAktivitetspliktService.oppdater(flytKontekst)
            BruddAktivitetspliktRepository(connection).hentGrunnlagHvisEksisterer(behandling.id).also {
                assertEquals(1, it?.bruddene?.size)
            }

            // Ved oppdatering av kravinformasjonen uten ny brudd, skal grunnlaget være uendret
            bruddAktivitetspliktService.oppdater(flytKontekst)
            BruddAktivitetspliktRepository(connection).hentGrunnlagHvisEksisterer(behandling.id).also {
                assertEquals(1, it?.bruddene?.size)
            }
        }
    }

    private fun mottattDokument(
        connection: DBConnection,
        brudd: BruddAktivitetsplikt,
        sak: Sak
    ) {
        val dokument = StrukturertDokument(brudd.innsendingId, Brevkode.AKTIVITETSKORT)
        MottaDokumentService(MottattDokumentRepository(connection)).mottattDokument(
            MottattDokumentReferanse(brudd.innsendingId),
            sak.id,
            LocalDateTime.ofInstant(brudd.opprettetTid, ZoneId.of("Europe/Oslo")),
            dokument.brevkode,
            dokument
        )
    }

    private fun flytKontekstMedPerioder(behandling: Behandling) =
        FlytKontekstMedPerioder(
            sakId = behandling.sakId,
            behandlingId = behandling.id,
            behandlingType = TypeBehandling.Førstegangsbehandling,
            perioderTilVurdering = setOf(),
        )

}