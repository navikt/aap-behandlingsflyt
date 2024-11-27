package no.nav.aap.behandlingsflyt.flyt.ventebehov

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehov
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.FakePdlGateway
import no.nav.aap.behandlingsflyt.dbtestdata.ident
import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokument
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Status
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Årsak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.Fakes
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.verdityper.dokument.Kanal
import no.nav.aap.verdityper.flyt.ÅrsakTilBehandling
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import no.nav.aap.verdityper.sakogbehandling.SakId
import org.junit.jupiter.api.Assertions.assertEquals
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.Test

@Fakes
class LegeerklæringVentebehovEvaluererTest {

    @Test
    fun LøserBehovNårDetFinnesAvvistDokument () {
        InitTestDatabase.dataSource.transaction { connection ->
            val evaluerer = LegeerklæringVentebehovEvaluerer(connection)
            val avklaringsbehov = Avklaringsbehov(1L, Definisjon.BESTILL_LEGEERKLÆRING, mutableListOf(), StegType.AVKLAR_SYKDOM, false)

            val sak = opprettSak(connection)
            val behandling = opprettBehandling(connection, sak, ÅrsakTilBehandling.MOTTATT_AVVIST_LEGEERKLÆRING)

            genererDokument(
                sak.id,
                behandling.id,
                connection,
                InnsendingType.LEGEERKLÆRING_AVVIST,
                InnsendingReferanse(InnsendingReferanse.Type.AVVIST_LEGEERKLÆRING_ID, "referanse")
            )

            val erLøst = evaluerer.ansesSomLøst(behandling.id, avklaringsbehov, sak.id)
            assertEquals(true, erLøst)
        }
    }

    @Test
    fun LøserBehovNårDetFinnesMottattLegeerklæring () {
        InitTestDatabase.dataSource.transaction { connection ->
            val evaluerer = LegeerklæringVentebehovEvaluerer(connection)
            val avklaringsbehov = Avklaringsbehov(1L, Definisjon.BESTILL_LEGEERKLÆRING, mutableListOf(), StegType.AVKLAR_SYKDOM, false)

            val sak = opprettSak(connection)
            val behandling = opprettBehandling(connection, sak, ÅrsakTilBehandling.MOTTATT_LEGEERKLÆRING)

            genererDokument(
                sak.id,
                behandling.id,
                connection,
                InnsendingType.LEGEERKLÆRING,
                InnsendingReferanse(InnsendingReferanse.Type.JOURNALPOST, "referanse")
            )

            val erLøst = evaluerer.ansesSomLøst(behandling.id, avklaringsbehov, sak.id)
            assertEquals(true, erLøst)
        }
    }

    @Test
    fun LøserBehovNårDetFinnesMottattDialogmelding() {
        InitTestDatabase.dataSource.transaction { connection ->
            val evaluerer = LegeerklæringVentebehovEvaluerer(connection)
            val avklaringsbehov = Avklaringsbehov(1L, Definisjon.BESTILL_LEGEERKLÆRING, mutableListOf(), StegType.AVKLAR_SYKDOM, false)

            val sak = opprettSak(connection)
            val behandling = opprettBehandling(connection, sak, ÅrsakTilBehandling.MOTTATT_DIALOGMELDING)

            genererDokument(
                sak.id,
                behandling.id,
                connection,
                InnsendingType.DIALOGMELDING,
                InnsendingReferanse(InnsendingReferanse.Type.JOURNALPOST, "referanse")
            )

            val erLøst = evaluerer.ansesSomLøst(behandling.id, avklaringsbehov, sak.id)
            assertEquals(true, erLøst)
        }
    }

    @Test
    fun LøserIkkeBehovNårLegeerklæringErEldreEnnBestilling() {
        InitTestDatabase.dataSource.transaction { connection ->
            val evaluerer = LegeerklæringVentebehovEvaluerer(connection)

            val sak = opprettSak(connection)
            val behandling = opprettBehandling(connection, sak, ÅrsakTilBehandling.MOTTATT_SØKNAD)

            genererDokument(
                sak.id,
                behandling.id,
                connection,
                InnsendingType.LEGEERKLÆRING,
                InnsendingReferanse(InnsendingReferanse.Type.JOURNALPOST, "referanse")
            )

            val avklaringsbehov = Avklaringsbehov(1L, Definisjon.BESTILL_LEGEERKLÆRING, mutableListOf(), StegType.AVKLAR_SYKDOM, false)
            val erLøst = evaluerer.ansesSomLøst(behandling.id, avklaringsbehov, sak.id)
            assertEquals(false, erLøst)
        }
    }

    @Test
    fun LøserIkkeBehovNårIkkeDetFinnesAvvistDokument () {
        InitTestDatabase.dataSource.transaction { connection ->
            val evaluerer = LegeerklæringVentebehovEvaluerer(connection)
            val avklaringsbehov = Avklaringsbehov(1L, Definisjon.BESTILL_LEGEERKLÆRING, mutableListOf(), StegType.AVKLAR_SYKDOM, false)

            val sak = opprettSak(connection)
            val behandling = opprettBehandling(connection, sak, ÅrsakTilBehandling.MOTTATT_SØKNAD)

            genererDokument(
                sak.id,
                behandling.id,
                connection,
                InnsendingType.SØKNAD,
                InnsendingReferanse(InnsendingReferanse.Type.JOURNALPOST, "referanse")
            )

            val erIkkeLøst = evaluerer.ansesSomLøst(behandling.id, avklaringsbehov, sak.id)
            assertEquals(false, erIkkeLøst)
        }
    }

    private fun genererDokument(
        sakId: SakId,
        behandlingId: BehandlingId,
        connection: DBConnection,
        type: InnsendingType,
        referanse: InnsendingReferanse
    ) {
        val mottattDokument = MottattDokument(
            referanse = referanse,
            sakId = sakId,
            behandlingId = behandlingId,
            mottattTidspunkt = LocalDateTime.now(),
            type = type,
            status = Status.MOTTATT,
            kanal = Kanal.DIGITAL,
            strukturertDokument = null,
        )
        MottattDokumentRepository(connection).lagre(
            mottattDokument
        )
    }

    private fun opprettSak(connection: DBConnection): Sak {
        return PersonOgSakService(connection, FakePdlGateway).finnEllerOpprett(
            ident(), Periode(LocalDate.now(), LocalDate.now().plusYears(3))
        )
    }

    private fun opprettBehandling(connection: DBConnection, sak: Sak, årsak: ÅrsakTilBehandling): Behandling {
        return SakOgBehandlingService(connection).finnEllerOpprettBehandling(
            sak.saksnummer, listOf(Årsak(årsak))
        ).behandling
    }
}