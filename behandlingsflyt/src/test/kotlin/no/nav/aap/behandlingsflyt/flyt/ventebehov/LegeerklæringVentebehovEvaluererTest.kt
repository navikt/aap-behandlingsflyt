package no.nav.aap.behandlingsflyt.flyt.ventebehov

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehov
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokument
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepositoryImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Status
import no.nav.aap.behandlingsflyt.help.FakePdlGateway
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.test.Fakes
import no.nav.aap.behandlingsflyt.test.ident
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.verdityper.dokument.Kanal
import org.junit.jupiter.api.Assertions.assertEquals
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.Test

@Fakes
internal class LegeerklæringVentebehovEvaluererTest {
    private val repositoryRegistry = RepositoryRegistry()
        .register<MottattDokumentRepositoryImpl>()

    companion object {
        private val dataSource = InitTestDatabase.freshDatabase()
    }

    @Test
    fun `Løser behov når det finnes avvist dokument` () {
        val behandling = dataSource.transaction { connection ->
            val sak = opprettSak(connection)
            finnEllerOpprettBehandling(connection, sak, Vurderingsbehov.MOTTATT_AVVIST_LEGEERKLÆRING)
        }

        dataSource.transaction { connection ->
            val repositoryProvider = repositoryRegistry.provider(connection)
            val evaluerer = LegeerklæringVentebehovEvaluerer(repositoryProvider)
            val avklaringsbehov = Avklaringsbehov(1L, Definisjon.BESTILL_LEGEERKLÆRING, mutableListOf(), StegType.AVKLAR_SYKDOM, false)

            genererDokument(
                behandling.sakId,
                behandling.id,
                connection,
                InnsendingType.LEGEERKLÆRING_AVVIST,
                InnsendingReferanse(InnsendingReferanse.Type.AVVIST_LEGEERKLÆRING_ID, "referanse"),
                LocalDateTime.now().plusDays(1)
            )

            val erLøst = evaluerer.ansesSomLøst(behandling.id, avklaringsbehov, behandling.sakId)
            assertEquals(true, erLøst)
        }
    }

    @Test
    fun `løser behov når det finnes mottatt legeerklæring` () {
        val behandling = dataSource.transaction { connection ->
            val sak = opprettSak(connection)
            finnEllerOpprettBehandling(connection, sak, Vurderingsbehov.MOTTATT_LEGEERKLÆRING)
        }

        dataSource.transaction { connection ->
            val repositoryProvider = repositoryRegistry.provider(connection)
            val evaluerer = LegeerklæringVentebehovEvaluerer(repositoryProvider)
            val avklaringsbehov = Avklaringsbehov(1L, Definisjon.BESTILL_LEGEERKLÆRING, mutableListOf(), StegType.AVKLAR_SYKDOM, false)

            genererDokument(
                behandling.sakId,
                behandling.id,
                connection,
                InnsendingType.LEGEERKLÆRING,
                InnsendingReferanse(InnsendingReferanse.Type.JOURNALPOST, "referanse"),
                LocalDateTime.now().plusDays(1)
            )

            val erLøst = evaluerer.ansesSomLøst(behandling.id, avklaringsbehov, behandling.sakId)
            assertEquals(true, erLøst)
        }
    }

    @Test
    fun `løser behov når det finnes mottatt dialogmelding`() {
        val behandling = dataSource.transaction { connection ->
            val sak = opprettSak(connection)
            finnEllerOpprettBehandling(connection, sak, Vurderingsbehov.MOTTATT_DIALOGMELDING)
        }

        dataSource.transaction { connection ->
            val repositoryProvider = repositoryRegistry.provider(connection)
            val evaluerer = LegeerklæringVentebehovEvaluerer(repositoryProvider)
            val avklaringsbehov = Avklaringsbehov(1L, Definisjon.BESTILL_LEGEERKLÆRING, mutableListOf(), StegType.AVKLAR_SYKDOM, false)

            genererDokument(
                behandling.sakId,
                behandling.id,
                connection,
                InnsendingType.DIALOGMELDING,
                InnsendingReferanse(InnsendingReferanse.Type.JOURNALPOST, "referanse"),
                LocalDateTime.now().plusDays(1)
            )

            val erLøst = evaluerer.ansesSomLøst(behandling.id, avklaringsbehov, behandling.sakId)
            assertEquals(true, erLøst)
        }
    }

    @Test
    fun `løser ikke behov når legeerklæring er eldre enn bestilling`() {
        val behandling = dataSource.transaction { connection ->
            val sak = opprettSak(connection)
            finnEllerOpprettBehandling(connection, sak, Vurderingsbehov.MOTTATT_SØKNAD)
        }

        dataSource.transaction { connection ->
            val repositoryProvider = repositoryRegistry.provider(connection)
            val evaluerer = LegeerklæringVentebehovEvaluerer(repositoryProvider)
            genererDokument(
                behandling.sakId,
                behandling.id,
                connection,
                InnsendingType.LEGEERKLÆRING,
                InnsendingReferanse(InnsendingReferanse.Type.JOURNALPOST, "referanse"),
                LocalDateTime.now().minusDays(1)
            )

            val avklaringsbehov = Avklaringsbehov(1L, Definisjon.BESTILL_LEGEERKLÆRING, mutableListOf(), StegType.AVKLAR_SYKDOM, false)
            val erLøst = evaluerer.ansesSomLøst(behandling.id, avklaringsbehov, behandling.sakId)
            assertEquals(false, erLøst)
        }
    }

    @Test
    fun `løser ikke behov når ikke det finnes avvist dokument` () {
        val behandling = dataSource.transaction { connection ->
            val sak = opprettSak(connection)
            finnEllerOpprettBehandling(connection, sak, Vurderingsbehov.MOTTATT_SØKNAD)
        }

        dataSource.transaction { connection ->
            val repositoryProvider = repositoryRegistry.provider(connection)
            val evaluerer = LegeerklæringVentebehovEvaluerer(repositoryProvider)
            val avklaringsbehov = Avklaringsbehov(1L, Definisjon.BESTILL_LEGEERKLÆRING, mutableListOf(), StegType.AVKLAR_SYKDOM, false)

            genererDokument(
                behandling.sakId,
                behandling.id,
                connection,
                InnsendingType.SØKNAD,
                InnsendingReferanse(InnsendingReferanse.Type.JOURNALPOST, "referanse"),
                LocalDateTime.now().plusDays(1)
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
        referanse: InnsendingReferanse,
        mottatt: LocalDateTime
    ) {
        val mottattDokument = MottattDokument(
            referanse = referanse,
            sakId = sakId,
            behandlingId = behandlingId,
            mottattTidspunkt = mottatt,
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
}