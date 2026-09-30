package no.nav.aap.behandlingsflyt.repository.klage

import no.nav.aap.behandlingsflyt.behandling.tilbakekrevingsbehandling.TilbakekrevingBehandlingsstatus
import no.nav.aap.behandlingsflyt.behandling.tilbakekrevingsbehandling.Tilbakekrevingshendelse
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.påklagetbehandling.PåklagetBehandlingVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.påklagetbehandling.PåklagetVedtakType
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.help.sak
import no.nav.aap.behandlingsflyt.repository.behandling.tilbakekrevingsbehandling.TilbakekrevingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.klage.PåklagetBehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.Bruker
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import java.net.URI
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Instant
import java.util.UUID

internal class PåklagetBehandlingRepositoryImplTest {
    companion object {
        private val søknadsdato = LocalDate.now()

        private lateinit var dataSource: TestDataSource

        @BeforeAll
        @JvmStatic
        fun setup() {
            dataSource = TestDataSource()
        }

        @AfterAll
        @JvmStatic
        fun tearDown() = dataSource.close()
    }

    @Test
    fun `Lagrer og henter påklagetbehandling med id`() {
        dataSource.transaction { connection ->
            val sak = sak(connection, søknadsdato)
            val behandling = finnEllerOpprettBehandling(connection, sak)
            val klageBehandling = finnEllerOpprettBehandling(connection, sak, Vurderingsbehov.MOTATT_KLAGE)
            
            val påklagetBehandlingRepository = PåklagetBehandlingRepositoryImpl(connection)
            val vurdering = PåklagetBehandlingVurdering(
                påklagetVedtakType = PåklagetVedtakType.KELVIN_BEHANDLING,
                påklagetBehandling = behandling.id,
                påklagetTilbakekrevingsbehandling = null,
                vurdertAv = Bruker("ident"),
            opprettet = Instant.now()
            )
            
            påklagetBehandlingRepository.lagre(klageBehandling.id, vurdering)
            val grunnlag = påklagetBehandlingRepository.hentHvisEksisterer(klageBehandling.id)!!
            assertThat(grunnlag.vurdering.påklagetVedtakType).isEqualTo(PåklagetVedtakType.KELVIN_BEHANDLING)
            assertThat(grunnlag.vurdering.påklagetBehandling).isEqualTo(behandling.id)
            assertThat(grunnlag.vurdering.påklagetTilbakekrevingsbehandling).isNull()
            assertThat(grunnlag.vurdering.vurdertAv).isEqualTo(Bruker("ident"))
            assertNotNull(grunnlag.vurdering.opprettet)

            val påklagetVedtaksType = påklagetBehandlingRepository.hentPåklagetVedtakstype(klageBehandling.id)
            assertThat(påklagetVedtaksType).isEqualTo(PåklagetVedtakType.KELVIN_BEHANDLING)
        }
    }

    @Test
    fun `Lagrer og henter påklaget tilbakekrevingsbehandling med referanse-uuid`() {
        dataSource.transaction { connection ->
            val sak = sak(connection, søknadsdato)
            finnEllerOpprettBehandling(connection, sak)
            val klageBehandling = finnEllerOpprettBehandling(connection, sak, Vurderingsbehov.MOTATT_KLAGE)
            val tilbakekrevingsReferanse = lagreTilbakekrevingsbehandling(connection, sak)

            val påklagetBehandlingRepository = PåklagetBehandlingRepositoryImpl(connection)
            val vurdering = PåklagetBehandlingVurdering(
                påklagetVedtakType = PåklagetVedtakType.TILBAKEKREVING,
                påklagetBehandling = null,
                påklagetTilbakekrevingsbehandling = tilbakekrevingsReferanse,
                vurdertAv = Bruker("ident"),
                opprettet = Instant.now()
            )

            påklagetBehandlingRepository.lagre(klageBehandling.id, vurdering)
            val grunnlag = påklagetBehandlingRepository.hentHvisEksisterer(klageBehandling.id)!!
            assertThat(grunnlag.vurdering.påklagetVedtakType).isEqualTo(PåklagetVedtakType.TILBAKEKREVING)
            assertThat(grunnlag.vurdering.påklagetBehandling).isNull()
            assertThat(grunnlag.vurdering.påklagetTilbakekrevingsbehandling).isEqualTo(tilbakekrevingsReferanse)
            assertThat(grunnlag.vurdering.vurdertAv).isEqualTo(Bruker("ident"))
            assertNotNull(grunnlag.vurdering.opprettet)

            val påklagetVedtaksType = påklagetBehandlingRepository.hentPåklagetVedtakstype(klageBehandling.id)
            assertThat(påklagetVedtaksType).isEqualTo(PåklagetVedtakType.TILBAKEKREVING)

            val vurderingMedReferanse =
                påklagetBehandlingRepository.hentGjeldendeVurderingMedReferanse(klageBehandling.referanse)!!
            assertThat(vurderingMedReferanse.påklagetVedtakType).isEqualTo(PåklagetVedtakType.TILBAKEKREVING)
            assertThat(vurderingMedReferanse.påklagetBehandling).isNull()
            assertThat(vurderingMedReferanse.påklagetTilbakekrevingsbehandling).isEqualTo(tilbakekrevingsReferanse)
        }
    }

    @Test
    fun `Henter vedtakstype fra aktiv vurdering`() {
        dataSource.transaction { connection ->
            val sak = sak(connection, søknadsdato)
            val behandling = finnEllerOpprettBehandling(connection, sak)
            val klageBehandling = finnEllerOpprettBehandling(connection, sak, Vurderingsbehov.MOTATT_KLAGE)
            val repository = PåklagetBehandlingRepositoryImpl(connection)

            repository.lagre(
                klageBehandling.id,
                PåklagetBehandlingVurdering(
                    påklagetVedtakType = PåklagetVedtakType.KELVIN_BEHANDLING,
                    påklagetBehandling = behandling.id,
                    påklagetTilbakekrevingsbehandling = null,
                    vurdertAv = Bruker("ident"),
                    opprettet = Instant.now(),
                ),
            )
            repository.lagre(
                klageBehandling.id,
                PåklagetBehandlingVurdering(
                    påklagetVedtakType = PåklagetVedtakType.TILBAKEKREVING,
                    påklagetBehandling = null,
                    påklagetTilbakekrevingsbehandling = lagreTilbakekrevingsbehandling(connection, sak),
                    vurdertAv = Bruker("ident"),
                    opprettet = Instant.now(),
                ),
            )

            assertThat(repository.hentPåklagetVedtakstype(klageBehandling.id))
                .isEqualTo(PåklagetVedtakType.TILBAKEKREVING)
        }
    }

    @Test
    fun `Lagrer og henter påklagetbehandling med referanse`() {
        dataSource.transaction { connection ->
            val sak = sak(connection, søknadsdato)
            val behandling = finnEllerOpprettBehandling(connection, sak)
            val klageBehandling = finnEllerOpprettBehandling(connection, sak, Vurderingsbehov.MOTATT_KLAGE)

            val påklagetBehandlingRepository = PåklagetBehandlingRepositoryImpl(connection)
            val vurdering = PåklagetBehandlingVurdering(
                påklagetVedtakType = PåklagetVedtakType.KELVIN_BEHANDLING,
                påklagetBehandling = behandling.id,
                påklagetTilbakekrevingsbehandling = null,
                vurdertAv = Bruker("ident"),
            opprettet = Instant.now()
            )

            påklagetBehandlingRepository.lagre(klageBehandling.id, vurdering)
            val vurderingMedReferanse = påklagetBehandlingRepository.hentGjeldendeVurderingMedReferanse(klageBehandling.referanse)!!
            assertThat(vurderingMedReferanse.påklagetVedtakType).isEqualTo(PåklagetVedtakType.KELVIN_BEHANDLING)
            assertThat(vurderingMedReferanse.påklagetBehandling).isEqualTo(behandling.id)
            assertThat(vurderingMedReferanse.påklagetTilbakekrevingsbehandling).isNull()
            assertThat(vurderingMedReferanse.referanse?.referanse).isEqualTo(behandling.referanse.referanse)
            assertThat(vurderingMedReferanse.vurdertAv).isEqualTo(Bruker("ident"))
            assertNotNull(vurderingMedReferanse.opprettet)
        }
    }

    private fun lagreTilbakekrevingsbehandling(connection: DBConnection, sak: Sak): UUID {
        val tilbakekrevingsReferanse = UUID.randomUUID()
        val nå = LocalDateTime.now()
        TilbakekrevingRepositoryImpl(connection).lagre(
            sak.id,
            Tilbakekrevingshendelse(
                tilbakekrevingBehandlingId = tilbakekrevingsReferanse,
                eksternFagsakId = "123",
                hendelseOpprettet = nå,
                eksternBehandlingId = UUID.randomUUID().toString(),
                sakOpprettet = nå,
                varselSendt = null,
                venteGrunn = null,
                gjenopptas = null,
                behandlingsstatus = TilbakekrevingBehandlingsstatus.AVSLUTTET,
                totaltFeilutbetaltBeløp = Beløp(1000),
                tilbakekrevingSaksbehandlingUrl = URI.create("https://nav.no"),
                fullstendigPeriode = Periode(LocalDate.now().minusYears(1), LocalDate.now()),
                versjon = 1,
                vedtaksdato = LocalDate.now(),
            )
        )
        return tilbakekrevingsReferanse
    }
}