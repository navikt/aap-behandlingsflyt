package no.nav.aap.behandlingsflyt.sakogbehandling.sak.flate

import no.nav.aap.behandlingsflyt.behandling.søknad.TrukketSøknadVurdering
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.help.opprettSak
import no.nav.aap.behandlingsflyt.integrasjon.createGatewayProvider
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.ResultatKode
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.behandling.vedtak.VedtakRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.søknad.TrukketSøknadRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.postgresRepositoryRegistry
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.test.AlleAvskruddUnleash
import no.nav.aap.behandlingsflyt.test.testGatewayProvider
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.verdityper.dokument.JournalpostId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime

class FinnSaksinfoTest {

    @Test
    fun `person finnes ikke - returnerer tom liste`() {
        val result = dataSource.transaction(readOnly = true) { connection ->
            SakOgBehandlingService(postgresRepositoryRegistry.provider(connection), createGatewayProvider { register<AlleAvskruddUnleash>()})
                .finnSaksinfo(Ident("00000000000"))
        }

        assertThat(result).isEmpty()
    }

    @Test
    fun `person har sak uten behandlinger - returnerer sak med null resultat`() {
        val sak = dataSource.transaction { connection ->
            opprettSak(connection, LocalDate.now())
        }

        val result = dataSource.transaction(readOnly = true) { connection ->
            SakOgBehandlingService(postgresRepositoryRegistry.provider(connection), createGatewayProvider { register<AlleAvskruddUnleash>()})
                .finnSaksinfo(Ident(sak.person.aktivIdent().identifikator))
        }

        assertThat(result).hasSize(1)
        assertThat(result.single().saksnummer).isEqualTo(sak.saksnummer.toString())
        assertThat(result.single().resultat).isNull()
    }

    @Test
    fun `person har sak med behandling som ikke er trukket - ingen gjeldende vedtatt behandling gir null resultat`() {
        val sak = dataSource.transaction { connection ->
            opprettSak(connection, LocalDate.now())
        }
        dataSource.transaction { connection ->
            finnEllerOpprettBehandling(connection, sak)
        }

        val result = dataSource.transaction(readOnly = true) { connection ->
            SakOgBehandlingService(postgresRepositoryRegistry.provider(connection), createGatewayProvider { register<AlleAvskruddUnleash>()})
                .finnSaksinfo(Ident(sak.person.aktivIdent().identifikator))
        }

        assertThat(result).hasSize(1)
        assertThat(result.single().resultat).isNull()
    }

    @Test
    fun `person har sak med behandling der søknad er trukket - returnerer TRUKKET`() {
        val sak = dataSource.transaction { connection ->
            opprettSak(connection, LocalDate.now())
        }
        val behandling = dataSource.transaction { connection ->
            finnEllerOpprettBehandling(connection, sak)
        }
        dataSource.transaction { connection ->
            TrukketSøknadRepositoryImpl(connection).lagreTrukketSøknadVurdering(
                behandlingId = behandling.id,
                vurdering = TrukketSøknadVurdering(
                    journalpostId = JournalpostId("123"),
                    begrunnelse = "Søker trekker søknaden",
                    skalTrekkes = true,
                    vurdertAv = Bruker("Z999999"),
                    vurdert = Instant.now(),
                )
            )
        }

        val result = dataSource.transaction(readOnly = true) { connection ->
            SakOgBehandlingService(postgresRepositoryRegistry.provider(connection), createGatewayProvider { register<AlleAvskruddUnleash>()})
                .finnSaksinfo(Ident(sak.person.aktivIdent().identifikator))
        }

        assertThat(result).hasSize(1)
        assertThat(result.single().resultat).isEqualTo(ResultatKode.TRUKKET)
    }

    @Test
    fun `person har sak med behandling der søknad IKKE skal trekkes - returnerer null resultat`() {
        val sak = dataSource.transaction { connection ->
            opprettSak(connection, LocalDate.now())
        }
        val behandling = dataSource.transaction { connection ->
            finnEllerOpprettBehandling(connection, sak)
        }
        dataSource.transaction { connection ->
            TrukketSøknadRepositoryImpl(connection).lagreTrukketSøknadVurdering(
                behandlingId = behandling.id,
                vurdering = TrukketSøknadVurdering(
                    journalpostId = JournalpostId("123"),
                    begrunnelse = "Søker ombestemte seg",
                    skalTrekkes = false,
                    vurdertAv = Bruker("Z999999"),
                    vurdert = Instant.now(),
                )
            )
        }

        val result = dataSource.transaction(readOnly = true) { connection ->
            SakOgBehandlingService(postgresRepositoryRegistry.provider(connection), createGatewayProvider { register<AlleAvskruddUnleash>()})
                .finnSaksinfo(Ident(sak.person.aktivIdent().identifikator))
        }

        assertThat(result).hasSize(1)
        assertThat(result.single().resultat).isNull()
    }

    @Test
    fun `sak med ferdigbehandlet FGB og åpen revurdering - bruker FGB som gjeldende vedtatt behandling`() {
        val sak = dataSource.transaction { connection -> opprettSak(connection, LocalDate.now()) }

        // Opprett og avslutt FGB med vedtak - dette gjør den til gjeldende vedtatt behandling
        val fgb = dataSource.transaction { connection ->
            val behandling = finnEllerOpprettBehandling(connection, sak)
            BehandlingRepositoryImpl(connection).oppdaterBehandlingStatus(behandling.id, Status.AVSLUTTET)
            VedtakRepositoryImpl(connection).lagre(behandling.id, LocalDateTime.now(), LocalDate.now())
            behandling
        }

        // Opprett revurdering (FGB er avsluttet, så ny behandling blir en revurdering)
        dataSource.transaction { connection ->
            finnEllerOpprettBehandling(
                connection, sak,
                vurderingsbehov = listOf(VurderingsbehovMedPeriode(Vurderingsbehov.REVURDER_LOVVALG)),
                årsakTilOpprettelse = ÅrsakTilOpprettelse.MANUELL_OPPRETTELSE
            )
        }

        val result = dataSource.transaction(readOnly = true) { connection ->
            SakOgBehandlingService(postgresRepositoryRegistry.provider(connection), createGatewayProvider { register<AlleAvskruddUnleash>()})
                .finnSaksinfo(Ident(sak.person.aktivIdent().identifikator))
        }

        // Gjeldende vedtatt behandling er FGB - resultatet utledes fra FGB (avslag uten underveisdata)
        assertThat(result).hasSize(1)
        assertThat(result.single().resultat).isEqualTo(ResultatKode.AVSLAG)
    }

    @Test
    fun `sak med ferdigbehandlet FGB og ferdigbehandlet revurdering - bruker revurdering som gjeldende vedtatt behandling`() {
        val sak = dataSource.transaction { connection -> opprettSak(connection, LocalDate.now()) }

        // Opprett og avslutt FGB med vedtak
        dataSource.transaction { connection ->
            val behandling = finnEllerOpprettBehandling(connection, sak)
            BehandlingRepositoryImpl(connection).oppdaterBehandlingStatus(behandling.id, Status.AVSLUTTET)
            VedtakRepositoryImpl(connection).lagre(behandling.id, LocalDateTime.now().minusWeeks(1), LocalDate.now().minusWeeks(1))
        }

        // Opprett og avslutt revurdering med vedtak - denne tar over som gjeldende vedtatt behandling
        dataSource.transaction { connection ->
            val revurdering = finnEllerOpprettBehandling(
                connection, sak,
                vurderingsbehov = listOf(VurderingsbehovMedPeriode(Vurderingsbehov.REVURDER_LOVVALG)),
                årsakTilOpprettelse = ÅrsakTilOpprettelse.MANUELL_OPPRETTELSE
            )
            BehandlingRepositoryImpl(connection).oppdaterBehandlingStatus(revurdering.id, Status.AVSLUTTET)
            VedtakRepositoryImpl(connection).lagre(revurdering.id, LocalDateTime.now(), LocalDate.now())
        }

        val result = dataSource.transaction(readOnly = true) { connection ->
            SakOgBehandlingService(postgresRepositoryRegistry.provider(connection), createGatewayProvider { register<AlleAvskruddUnleash>()})
                .finnSaksinfo(Ident(sak.person.aktivIdent().identifikator))
        }

        // Gjeldende vedtatt behandling er revurderingen - uten søknadsvurderingsbehov gir den null resultat
        assertThat(result).hasSize(1)
        assertThat(result.single().resultat).isNull()
    }

    companion object {
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
}
