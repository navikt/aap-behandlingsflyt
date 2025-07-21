package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.markering

import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.markering.Markering
import no.nav.aap.behandlingsflyt.kontrakt.behandling.MarkeringType
import no.nav.aap.behandlingsflyt.repository.avklaringsbehov.FakePdlGateway
import no.nav.aap.behandlingsflyt.repository.postgresRepositoryRegistry
import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Årsak
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.ÅrsakTilBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.FakeUnleash
import no.nav.aap.behandlingsflyt.test.ident
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class MarkeringRepositoryImplTest {
    private val dataSource = InitTestDatabase.freshDatabase()

    @Test
    fun `lagre og hente markering for gitt behandling`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = behandling(connection, sak)

            val behandlingMarkeringRepository = MarkeringRepositoryImpl(connection)
            val nyMarkering = Markering(
                forBehandling = behandling.id,
                markeringType = MarkeringType.HASTER,
                begrunnelse = "behandlingen haster",
                erAktiv = true,
                opprettetAv = "saksbehandler"
            )

            behandlingMarkeringRepository.lagre(nyMarkering)
            val uthentetMarkering = behandlingMarkeringRepository.hentAktiveMarkeringerForBehandling(behandling.id)
            assertThat(uthentetMarkering).hasSize(1)
            assertThat(uthentetMarkering.first()).isEqualTo(nyMarkering)
        }
    }

    @Test
    fun `deaktiver aktiv markering for gitt behandling`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = behandling(connection, sak)

            val behandlingMarkeringRepository = MarkeringRepositoryImpl(connection)
            val gammelMarkering = Markering(
                forBehandling = behandling.id,
                markeringType = MarkeringType.HASTER,
                begrunnelse = "behandlingen haster",
                erAktiv = true,
                opprettetAv = "saksbehandler"
            )

            behandlingMarkeringRepository.lagre(gammelMarkering)
            val uthentetMarkering = behandlingMarkeringRepository.hentAktiveMarkeringerForBehandling(behandling.id)
            assertThat(uthentetMarkering.first().erAktiv).isTrue()

            behandlingMarkeringRepository.deaktiverMarkering(behandling.id, MarkeringType.HASTER)
            val aktiveMarkeringer = behandlingMarkeringRepository.hentAktiveMarkeringerForBehandling(behandling.id)
            assertThat(aktiveMarkeringer).isEmpty()
        }
    }

    @Test
    fun `ny markering av gitt type på samme behandling deaktiverer den gamle markeringen`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = behandling(connection, sak)

            val behandlingMarkeringRepository = MarkeringRepositoryImpl(connection)
            val nyMarkering = Markering(
                forBehandling = behandling.id,
                markeringType = MarkeringType.HASTER,
                begrunnelse = "behandlingen haster",
                erAktiv = true,
                opprettetAv = "saksbehandler"
            )

            behandlingMarkeringRepository.lagre(nyMarkering)

            val duplisertMarkering = Markering(
                forBehandling = behandling.id,
                markeringType = MarkeringType.HASTER,
                begrunnelse = "behandlingen haster fortsatt",
                erAktiv = true,
                opprettetAv = "saksbehandler"
            )

            behandlingMarkeringRepository.lagre(duplisertMarkering)

            val markeringerPåBehandling = behandlingMarkeringRepository.hentAktiveMarkeringerForBehandling(behandling.id)
            assertThat(markeringerPåBehandling).hasSize(1)
            assertThat(markeringerPåBehandling.first()).isEqualTo(duplisertMarkering)
        }
    }

    private companion object {
        private val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))
    }

    private fun sak(connection: DBConnection): Sak {
        return PersonOgSakService(
            FakePdlGateway,
            PersonRepositoryImpl(connection),
            SakRepositoryImpl(connection)
        ).finnEllerOpprett(ident(), periode)
    }

    private fun behandling(connection: DBConnection, sak: Sak): Behandling {
        return SakOgBehandlingService(
            postgresRepositoryRegistry.provider(connection),
            unleashGateway = FakeUnleash,
        )
            .finnEllerOpprettBehandling(
                sak.saksnummer,
                listOf(Årsak(ÅrsakTilBehandling.MOTTATT_SØKNAD))
            )
    }
}