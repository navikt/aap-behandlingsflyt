package no.nav.aap.behandlingsflyt.behandling.klage.resultat

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import no.nav.aap.behandlingsflyt.AbstractApiTest
import no.nav.aap.behandlingsflyt.behandling.svarfraandreinstans.svarfraandreinstans.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokument
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.StrukturertDokument
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.UnparsedStrukturertDokument
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.AnkeUtfall
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.AnkebehandlingAvsluttetDetaljer
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.AnkebehandlingOpprettetDetaljer
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.BehandlingDetaljer
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.BehandlingEventType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.KabalHendelse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.KabalHendelseV0
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.KlageUtfall
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.KlagebehandlingAvsluttetDetaljer
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Melding
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.test.Fakes
import no.nav.aap.behandlingsflyt.test.MockDataSource
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryBehandlingRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryMottattDokumentRepository
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.verdityper.dokument.Kanal
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID
import kotlin.random.Random

@Fakes
class KlageresultatApiTest : AbstractApiTest() {
    private val repositoryRegistry = RepositoryRegistry()
        .register<InMemoryMottattDokumentRepository>()
        .register<InMemoryBehandlingRepository>()

    @Test
    fun `skal returnere resultat med tom liste dersom klageresultater fra kabal ikke finnes`() {
        val ds = MockDataSource()
        val behandling = opprettBehandling(nySak(), TypeBehandling.Revurdering)

        testApplication {
            installApplication {
                klageresultatApi(ds, repositoryRegistry)
            }

            val jwt = issueToken("nav:aap:afpoffentlig.read")
            val response = createClient().get("/api/klage/${behandling.referanse.referanse}/kabal-resultat") {
                header("Authorization", "Bearer ${jwt.serialize()}")
            }
            assertThat(response.status).isEqualTo(HttpStatusCode.OK)
            val resultat = response.body<KabalKlageResultat>()
            assertThat(resultat.svarFraAndreinstans).isEmpty()
        }
    }

    @Test
    fun `skal returnere resultat med et element når det foreligger en hendelse fra kabal`() {
        val ds = MockDataSource()
        val sak = nySak()
        val behandling = opprettBehandling(sak, TypeBehandling.Revurdering)
        val kabalHendelse = KabalHendelseV0(
            eventId = UUID.randomUUID(),
            kildeReferanse = behandling.referanse.referanse.toString(),
            kilde = "kilde",
            kabalReferanse = UUID.randomUUID().toString(),
            type = BehandlingEventType.KLAGEBEHANDLING_AVSLUTTET,
            detaljer = BehandlingDetaljer(
                klagebehandlingAvsluttet = KlagebehandlingAvsluttetDetaljer(
                    avsluttet = LocalDateTime.now(),
                    utfall = KlageUtfall.MEDHOLD,
                    journalpostReferanser = emptyList(),
                ),
            )
        )
        InMemoryMottattDokumentRepository.lagre(opprettKabalHendelse(sak.id, kabalHendelse))

        testApplication {
            installApplication {
                klageresultatApi(ds, repositoryRegistry)
            }

            val jwt = issueToken("nav:aap:afpoffentlig.read")
            val response = createClient().get("/api/klage/${behandling.referanse.referanse}/kabal-resultat") {
                header("Authorization", "Bearer ${jwt.serialize()}")
            }
            assertThat(response.status).isEqualTo(HttpStatusCode.OK)
            val resultat = response.body<KabalKlageResultat>()
            assertThat(resultat.svarFraAndreinstans).hasSize(1)
            assertThat(resultat.svarFraAndreinstans.first().type).isEqualTo(BehandlingEventType.KLAGEBEHANDLING_AVSLUTTET)
            assertThat(resultat.svarFraAndreinstans.first().utfall).isEqualTo(Utfall.MEDHOLD)
            assertThat(resultat.svarFraAndreinstans.first().feilregistrertBegrunnelse).isNull()
            assertThat(resultat.svarFraAndreinstans.first().opprettetTidspunkt).isNull()
            assertThat(resultat.svarFraAndreinstans.first().avsluttetTidspunkt).isEqualTo(kabalHendelse.detaljer.klagebehandlingAvsluttet?.avsluttet)
        }
    }

    @Test
    fun `skal returnere resultater med flere element når det foreligger flere hendelse fra kabal`() {
        val ds = MockDataSource()
        val sak = nySak()
        val behandling = opprettBehandling(sak, TypeBehandling.Revurdering)
        val klageAvsluttet = KabalHendelseV0(
            eventId = UUID.randomUUID(),
            kildeReferanse = behandling.referanse.referanse.toString(),
            kilde = "kilde",
            kabalReferanse = UUID.randomUUID().toString(),
            type = BehandlingEventType.KLAGEBEHANDLING_AVSLUTTET,
            detaljer = BehandlingDetaljer(
                klagebehandlingAvsluttet = KlagebehandlingAvsluttetDetaljer(
                    avsluttet = LocalDateTime.now().minusDays(100),
                    utfall = KlageUtfall.DELVIS_MEDHOLD,
                    journalpostReferanser = emptyList(),
                ),
            )
        )

        val ankeOpprettet = KabalHendelseV0(
            eventId = UUID.randomUUID(),
            kildeReferanse = behandling.referanse.referanse.toString(),
            kilde = "kilde",
            kabalReferanse = UUID.randomUUID().toString(),
            type = BehandlingEventType.ANKEBEHANDLING_OPPRETTET,
            detaljer = BehandlingDetaljer(
                ankebehandlingOpprettet = AnkebehandlingOpprettetDetaljer(
                    mottattKlageinstans = LocalDateTime.now().minusDays(50),
                ),
            )
        )

        val ankeAvsluttet = KabalHendelseV0(
            eventId = UUID.randomUUID(),
            kildeReferanse = behandling.referanse.referanse.toString(),
            kilde = "kilde",
            kabalReferanse = UUID.randomUUID().toString(),
            type = BehandlingEventType.ANKEBEHANDLING_AVSLUTTET,
            detaljer = BehandlingDetaljer(
                ankebehandlingAvsluttet = AnkebehandlingAvsluttetDetaljer(
                    avsluttet = LocalDateTime.now(),
                    utfall = AnkeUtfall.STADFESTELSE,
                    journalpostReferanser = emptyList(),
                ),
            )
        )
        InMemoryMottattDokumentRepository.lagre(opprettKabalHendelse(sak.id, klageAvsluttet))
        InMemoryMottattDokumentRepository.lagre(opprettKabalHendelse(sak.id, ankeOpprettet))
        InMemoryMottattDokumentRepository.lagre(opprettKabalHendelse(sak.id, ankeAvsluttet))

        testApplication {
            installApplication {
                klageresultatApi(ds, repositoryRegistry)
            }

            val jwt = issueToken("nav:aap:afpoffentlig.read")
            val response = createClient().get("/api/klage/${behandling.referanse.referanse}/kabal-resultat") {
                header("Authorization", "Bearer ${jwt.serialize()}")
            }
            assertThat(response.status).isEqualTo(HttpStatusCode.OK)
            val resultat = response.body<KabalKlageResultat>()
            assertThat(resultat.svarFraAndreinstans).hasSize(3)
            val kabalBehandlingAvsluttet = resultat.svarFraAndreinstans.find { it.type == BehandlingEventType.KLAGEBEHANDLING_AVSLUTTET } ?: error("Fant ikke klagebehandling avsluttet")
            val kabalAnkeOpprettet = resultat.svarFraAndreinstans.find { it.type == BehandlingEventType.ANKEBEHANDLING_OPPRETTET } ?: error("Fant ikke anke opprettet")
            val kabalAnkeAvsluttet = resultat.svarFraAndreinstans.find { it.type == BehandlingEventType.ANKEBEHANDLING_AVSLUTTET } ?: error("Fant ikke anke avsluttet")

            assertThat(kabalBehandlingAvsluttet.type).isEqualTo(BehandlingEventType.KLAGEBEHANDLING_AVSLUTTET)
            assertThat(kabalBehandlingAvsluttet.utfall).isEqualTo(Utfall.DELVIS_MEDHOLD)
            assertThat(kabalBehandlingAvsluttet.feilregistrertBegrunnelse).isNull()
            assertThat(kabalBehandlingAvsluttet.opprettetTidspunkt).isNull()
            assertThat(kabalBehandlingAvsluttet.avsluttetTidspunkt).isEqualTo(klageAvsluttet.detaljer.klagebehandlingAvsluttet?.avsluttet)

            assertThat(kabalAnkeOpprettet.type).isEqualTo(BehandlingEventType.ANKEBEHANDLING_OPPRETTET)
            assertThat(kabalAnkeOpprettet.utfall).isNull()
            assertThat(kabalAnkeOpprettet.feilregistrertBegrunnelse).isNull()
            assertThat(kabalAnkeOpprettet.opprettetTidspunkt).isEqualTo(ankeOpprettet.detaljer.ankebehandlingOpprettet?.mottattKlageinstans)

            assertThat(kabalAnkeAvsluttet.type).isEqualTo(BehandlingEventType.ANKEBEHANDLING_AVSLUTTET)
            assertThat(kabalAnkeAvsluttet.utfall).isEqualTo(Utfall.STADFESTELSE)
            assertThat(kabalAnkeAvsluttet.feilregistrertBegrunnelse).isNull()
            assertThat(kabalAnkeAvsluttet.opprettetTidspunkt).isNull()
            assertThat(kabalAnkeAvsluttet.avsluttetTidspunkt).isEqualTo(ankeAvsluttet.detaljer.ankebehandlingAvsluttet?.avsluttet)
        }
    }

    @Test
    fun `skal feile dersom behandlingen ikke finnes`() {
        val ds = MockDataSource()

        testApplication {
            installApplication {
                klageresultatApi(ds, repositoryRegistry)
            }

            val jwt = issueToken("nav:aap:afpoffentlig.read")
            val response = createClient().get("/api/klage/${UUID.randomUUID()}/kabal-resultat") {
                header("Authorization", "Bearer ${jwt.serialize()}")
            }
            assertThat(response.status).isEqualTo(HttpStatusCode.InternalServerError)
        }
    }

    private fun opprettKabalHendelse(sakId: SakId, kabalHendelse: Melding): MottattDokument {

        return MottattDokument(
            referanse = InnsendingReferanse(type = InnsendingReferanse.Type.KABAL_HENDELSE_ID, verdi = UUID.randomUUID().toString()),
            sakId = sakId,
            behandlingId = BehandlingId(Random.nextLong()),
            mottattTidspunkt = LocalDateTime.now(),
            type = InnsendingType.KABAL_HENDELSE,
            kanal = Kanal.DIGITAL,
            status = Status.MOTTATT,
            strukturertDokument = StrukturertDokument(kabalHendelse)
        )
    }

}