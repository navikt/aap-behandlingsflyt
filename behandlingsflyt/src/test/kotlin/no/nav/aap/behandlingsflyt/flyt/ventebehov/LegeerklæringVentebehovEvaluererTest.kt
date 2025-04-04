package no.nav.aap.behandlingsflyt.flyt.ventebehov

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehov
import no.nav.aap.behandlingsflyt.faktagrunnlag.FakePdlGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.GrunnlagKopierer
import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokument
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepositoryImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Status
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Årsak
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.ÅrsakTilBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.test.Fakes
import no.nav.aap.behandlingsflyt.test.ident
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.repository.RepositoryRegistry
import no.nav.aap.verdityper.dokument.Kanal
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.Test

@Fakes
class LegeerklæringVentebehovEvaluererTest {
    companion object {
        @JvmStatic
        @BeforeAll
        fun beforeAll() {
            RepositoryRegistry
                .register<MottattDokumentRepositoryImpl>()
                .status()
        }
    }

    @Test
    fun `Løser behov når det finnes avvist dokument` () {
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
    fun `løser behov når det finnes mottatt legeerklæring` () {
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
    fun `løser behov når det finnes mottatt dialogmelding`() {
        val behandling = InitTestDatabase.dataSource.transaction { connection ->

            val sak = opprettSak(connection)
            opprettBehandling(connection, sak, ÅrsakTilBehandling.MOTTATT_DIALOGMELDING)
        }

        InitTestDatabase.dataSource.transaction { connection ->
            val evaluerer = LegeerklæringVentebehovEvaluerer(connection)
            val avklaringsbehov = Avklaringsbehov(1L, Definisjon.BESTILL_LEGEERKLÆRING, mutableListOf(), StegType.AVKLAR_SYKDOM, false)

            genererDokument(
                behandling.sakId,
                behandling.id,
                connection,
                InnsendingType.DIALOGMELDING,
                InnsendingReferanse(InnsendingReferanse.Type.JOURNALPOST, "referanse")
            )

            val erLøst = evaluerer.ansesSomLøst(behandling.id, avklaringsbehov, behandling.sakId)
            assertEquals(true, erLøst)
        }
    }

    @Test
    fun `løser ikke behov når legeerklæring er eldre enn bestilling`() {
        val behandling = InitTestDatabase.dataSource.transaction { connection ->
            val sak = opprettSak(connection)
            opprettBehandling(connection, sak, ÅrsakTilBehandling.MOTTATT_SØKNAD)
        }

        InitTestDatabase.dataSource.transaction { connection ->
            val evaluerer = LegeerklæringVentebehovEvaluerer(connection)
            genererDokument(
                behandling.sakId,
                behandling.id,
                connection,
                InnsendingType.LEGEERKLÆRING,
                InnsendingReferanse(InnsendingReferanse.Type.JOURNALPOST, "referanse")
            )

            val avklaringsbehov = Avklaringsbehov(1L, Definisjon.BESTILL_LEGEERKLÆRING, mutableListOf(), StegType.AVKLAR_SYKDOM, false)
            val erLøst = evaluerer.ansesSomLøst(behandling.id, avklaringsbehov, behandling.sakId)
            assertEquals(false, erLøst)
        }
    }

    @Test
    fun `løser ikke behov når ikke det finnes avvist dokument` () {
        val behandling = InitTestDatabase.dataSource.transaction { connection ->
            val sak = opprettSak(connection)
            opprettBehandling(connection, sak, ÅrsakTilBehandling.MOTTATT_SØKNAD)
        }

        InitTestDatabase.dataSource.transaction { connection ->
            val evaluerer = LegeerklæringVentebehovEvaluerer(connection)
            val avklaringsbehov = Avklaringsbehov(1L, Definisjon.BESTILL_LEGEERKLÆRING, mutableListOf(), StegType.AVKLAR_SYKDOM, false)

            genererDokument(
                behandling.sakId,
                behandling.id,
                connection,
                InnsendingType.SØKNAD,
                InnsendingReferanse(InnsendingReferanse.Type.JOURNALPOST, "referanse")
            )

            val erIkkeLøst = evaluerer.ansesSomLøst(behandling.id, avklaringsbehov, behandling.sakId)
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
        MottattDokumentRepositoryImpl(connection).lagre(
            mottattDokument
        )
    }

    private fun opprettSak(connection: DBConnection): Sak {
        return PersonOgSakService(
            FakePdlGateway,
            PersonRepositoryImpl(connection),
            SakRepositoryImpl(connection)
        ).finnEllerOpprett(
            ident(), Periode(LocalDate.now(), LocalDate.now().plusYears(3))
        )
    }

    private fun opprettBehandling(connection: DBConnection, sak: Sak, årsak: ÅrsakTilBehandling): Behandling {
        return SakOgBehandlingService(
            GrunnlagKopierer(connection), SakRepositoryImpl(connection),
            BehandlingRepositoryImpl(connection)
        ).finnEllerOpprettBehandling(
            sak.saksnummer, listOf(Årsak(årsak))
        ).behandling
    }
}