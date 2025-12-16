package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.prosessering.HendelseMottattHåndteringJobbUtfører
import no.nav.aap.behandlingsflyt.test.FakeUnleash
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.verdityper.dokument.Kanal
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

class LegeerklæringFlytTest: AbstraktFlytOrkestratorTest(FakeUnleash::class) {
    @Test
    fun `Fjerner legeerklæring ventebehov ved mottak av avvist legeerklæring`() {
        // Oppretter vanlig søknad
        val (_, behandling) = sendInnFørsteSøknad()
        behandling.medKontekst {
            // Validér avklaring
            assertThat(åpneAvklaringsbehov).anySatisfy { assertThat(it.definisjon).isEqualTo(Definisjon.AVKLAR_SYKDOM) }
        }

        // Oppretter bestilling av legeerklæring
        bestillLegeerklæring(behandling.id)
        motor.kjørJobber()

        behandling.medKontekst {
            assertThat(åpneAvklaringsbehov.all { it.definisjon == Definisjon.BESTILL_LEGEERKLÆRING })
        }

        // Send inn avvist legeerklæring
        val avvistLegeerklæringId = UUID.randomUUID().toString()
        dataSource.transaction { connection ->
            val flytJobbRepository = FlytJobbRepository(connection)
            flytJobbRepository.leggTil(
                HendelseMottattHåndteringJobbUtfører.nyJobb(
                    sakId = behandling.sakId,
                    dokumentReferanse = InnsendingReferanse(
                        InnsendingReferanse.Type.AVVIST_LEGEERKLÆRING_ID,
                        avvistLegeerklæringId
                    ),
                    brevkategori = InnsendingType.LEGEERKLÆRING_AVVIST,
                    kanal = Kanal.DIGITAL,
                    mottattTidspunkt = LocalDateTime.now()
                )
            )
        }
        motor.kjørJobber()

        // Validér avklaring
        behandling.medKontekst {
            val legeerklæringBestillingVenteBehov =
                åpneAvklaringsbehov.filter { it.definisjon == Definisjon.BESTILL_LEGEERKLÆRING }
            assertThat(legeerklæringBestillingVenteBehov.isEmpty()).isTrue()
        }
    }

    @Test
    fun `Fjerner legeerklæring ventebehov ved mottak av legeerklæring`() {
        // Oppretter vanlig søknad
        val (sak, behandling) = sendInnFørsteSøknad()

        // Validér avklaring
        var åpneAvklaringsbehov = hentÅpneAvklaringsbehov(behandling)
        assertThat(åpneAvklaringsbehov).anySatisfy { assertThat(it.definisjon).isEqualTo(Definisjon.AVKLAR_SYKDOM) }

        // Oppretter bestilling av legeerklæring
        bestillLegeerklæring(behandling.id)
        motor.kjørJobber()

        åpneAvklaringsbehov = hentÅpneAvklaringsbehov(behandling)
        assertThat(åpneAvklaringsbehov).anySatisfy { assertThat(it.definisjon).isEqualTo(Definisjon.BESTILL_LEGEERKLÆRING) }

        // Validér avklaring
        åpneAvklaringsbehov = hentÅpneAvklaringsbehov(behandling.id)
        assertThat(åpneAvklaringsbehov.all { it.definisjon == Definisjon.BESTILL_LEGEERKLÆRING })

        // Mottar legeerklæring
        val journalpostId = UUID.randomUUID().toString()
        dataSource.transaction { connection ->
            val flytJobbRepository = FlytJobbRepository(connection)
            flytJobbRepository.leggTil(
                HendelseMottattHåndteringJobbUtfører.nyJobb(
                    sakId = sak.id,
                    dokumentReferanse = InnsendingReferanse(
                        InnsendingReferanse.Type.JOURNALPOST,
                        journalpostId
                    ),
                    brevkategori = InnsendingType.LEGEERKLÆRING,
                    kanal = Kanal.DIGITAL,
                    mottattTidspunkt = LocalDateTime.now()
                )
            )
        }
        motor.kjørJobber()

        // Validér avklaring
        åpneAvklaringsbehov = hentÅpneAvklaringsbehov(behandling.id)

        val legeerklæringBestillingVenteBehov =
            åpneAvklaringsbehov.filter { it.definisjon == Definisjon.BESTILL_LEGEERKLÆRING }
        assertThat(legeerklæringBestillingVenteBehov.isEmpty()).isTrue()

    }

    @Test
    fun `Fjerner legeerklæring ventebehov ved mottak av dialogmelding`() {
        // Oppretter vanlig søknad
        val (sak, behandling) = sendInnFørsteSøknad()

        // Validér avklaring
        var åpneAvklaringsbehov = hentÅpneAvklaringsbehov(behandling)
        assertThat(åpneAvklaringsbehov).anySatisfy { assertThat(it.definisjon).isEqualTo(Definisjon.AVKLAR_SYKDOM) }

        // Oppretter bestilling av legeerklæring
        bestillLegeerklæring(behandling.id)

        assertThat(hentÅpneAvklaringsbehov(behandling)).anySatisfy { assertThat(it.definisjon).isEqualTo(Definisjon.BESTILL_LEGEERKLÆRING) }

        // Validér avklaring
        åpneAvklaringsbehov = hentÅpneAvklaringsbehov(behandling)
        assertThat(åpneAvklaringsbehov.all { it.definisjon == Definisjon.BESTILL_LEGEERKLÆRING })

        // Mottar dialogmelding
        val journalpostId = UUID.randomUUID().toString()
        dataSource.transaction { connection ->
            val flytJobbRepository = FlytJobbRepository(connection)
            flytJobbRepository.leggTil(
                HendelseMottattHåndteringJobbUtfører.nyJobb(
                    sakId = sak.id,
                    dokumentReferanse = InnsendingReferanse(
                        InnsendingReferanse.Type.JOURNALPOST,
                        journalpostId
                    ),
                    brevkategori = InnsendingType.DIALOGMELDING,
                    kanal = Kanal.DIGITAL,
                    mottattTidspunkt = LocalDateTime.now()
                )
            )
        }
        motor.kjørJobber()

        // Validér avklaring
        åpneAvklaringsbehov = hentÅpneAvklaringsbehov(behandling.id)
        val legeerklæringBestillingVenteBehov =
            åpneAvklaringsbehov.filter { it.definisjon == Definisjon.BESTILL_LEGEERKLÆRING }
        assertThat(legeerklæringBestillingVenteBehov.isEmpty()).isTrue()
    }

}