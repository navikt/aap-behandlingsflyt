package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.flyt.TestPersoner.PERSON_62
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.prosessering.HendelseMottattHåndteringJobbUtfører
import no.nav.aap.behandlingsflyt.test.AlleAvskruddUnleash
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.verdityper.dokument.Kanal
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class LegeerklæringFlytTest : AbstraktFlytOrkestratorTest(AlleAvskruddUnleash::class) {
    @Test
    fun `Fjerner legeerklæring ventebehov ved mottak av avvist legeerklæring`() {
        // Oppretter vanlig søknad
        val (_, behandling) = sendInnFørsteSøknad()
        behandling.medKontekst {
            // Validér avklaring
            assertThat(åpneAvklaringsbehov).anySatisfy { assertThat(it.definisjon).isEqualTo(Definisjon.AVKLAR_SYKDOM) }
        }

        // Oppretter bestilling av legeerklæring
        behandling
            .bestillLegeerklæring()
            .medKontekst {
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
            assertThat(legeerklæringBestillingVenteBehov).isEmpty()
        }
    }

    @Test
    fun `Fjerner legeerklæring ventebehov ved mottak av legeerklæring`() {
        // Oppretter vanlig søknad
        val (sak, behandling) = sendInnFørsteSøknad()

        // Validér avklaring
        behandling.medKontekst {
            assertThat(åpneAvklaringsbehov).anySatisfy { assertThat(it.definisjon).isEqualTo(Definisjon.AVKLAR_SYKDOM) }
        } // Oppretter bestilling av legeerklæring
            .bestillLegeerklæring()
            .medKontekst {
                // Både ventebehovet og avklaringsbehovet er åpent
                assertThat(åpneAvklaringsbehov.map { it.definisjon }).contains(
                    Definisjon.BESTILL_LEGEERKLÆRING,
                    Definisjon.AVKLAR_SYKDOM
                )
            }

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
        behandling.medKontekst {
            val legeerklæringBestillingVenteBehov =
                åpneAvklaringsbehov.filter { it.definisjon == Definisjon.BESTILL_LEGEERKLÆRING }
            assertThat(legeerklæringBestillingVenteBehov).isEmpty()
        }
    }


    @Test
    fun `Ventebehov gjør at behandling ikke kommer videre før fristen er gått ut`() {
        val fom = LocalDate.now()
        val periode = Periode(fom, fom.plusYears(3))
        val person = PERSON_62().medInntekter(emptyList())

        val (sak, behandling) = sendInnFørsteSøknad(
            person = person,
            mottattTidspunkt = fom.atStartOfDay(),
            periode = periode,
        )

        // Validér avklaring
        behandling
            .løsLovvalg(sak.rettighetsperiode.fom)
            .medKontekst {
                assertThat(åpneAvklaringsbehov).anySatisfy { assertThat(it.definisjon).isEqualTo(Definisjon.AVKLAR_SYKDOM) }
            } // Oppretter bestilling av legeerklæring
            .bestillLegeerklæring()
            .medKontekst {
                // Både ventebehovet og avklaringsbehovet er åpent
                assertThat(åpneAvklaringsbehov.map { it.definisjon }).contains(
                    Definisjon.BESTILL_LEGEERKLÆRING,
                    Definisjon.AVKLAR_SYKDOM
                )
            }
            .løsLovvalg(sak.rettighetsperiode.fom, false)
            .medKontekst {
                // Ønsker å dra rett til foreslå vedtak!
                assertThat(åpneAvklaringsbehov.map { it.definisjon }).contains(
                    Definisjon.BESTILL_LEGEERKLÆRING,
                )
            }
    }

    @Test
    fun `Fjerner legeerklæring ventebehov ved mottak av dialogmelding`() {
        // Oppretter vanlig søknad
        val (sak, behandling) = sendInnFørsteSøknad()

        // Validér avklaring
        behandling.medKontekst {
            assertThat(åpneAvklaringsbehov).anySatisfy { assertThat(it.definisjon).isEqualTo(Definisjon.AVKLAR_SYKDOM) }
        }
            // Oppretter bestilling av legeerklæring
            .bestillLegeerklæring()
            .medKontekst {
                assertThat(åpneAvklaringsbehov).anySatisfy { assertThat(it.definisjon).isEqualTo(Definisjon.BESTILL_LEGEERKLÆRING) }

            }

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
        behandling.medKontekst {
            val legeerklæringBestillingVenteBehov =
                åpneAvklaringsbehov.filter { it.definisjon == Definisjon.BESTILL_LEGEERKLÆRING }
            assertThat(legeerklæringBestillingVenteBehov).isEmpty()
        }
    }

}