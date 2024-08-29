package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov

import io.mockk.spyk
import io.mockk.verify
import no.nav.aap.auth.Bruker
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.ÅrsakTilSettPåVent
import no.nav.aap.behandlingsflyt.dbtestdata.ident
import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.hendelse.avløp.BehandlingHendelseService
import no.nav.aap.behandlingsflyt.hendelse.mottak.BehandlingSattPåVent
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.EndringType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Årsak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.behandlingsflyt.server.prosessering.StoppetHendelseJobbUtfører
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbType
import org.junit.jupiter.api.Test
import java.time.LocalDate

class AvklaringsbehovOrkestratorTest {

    @Test
    fun taAvVentHvisPåVentOgFortsettProsessering() {
        // TODO implementer
    }

    @Test
    fun løsAvklaringsbehovOgFortsettProsessering() {
        // TODO implementer
    }

    @Test
    fun `behandlingHendelseService dot stoppet blir kalt når en behandling er satt på vent`() {
        JobbType.leggTil(StoppetHendelseJobbUtfører)
        InitTestDatabase.dataSource.transaction { connection ->
            // Bruker mock her fordi formålet med testen er å verifisere at noe blir kalt
            val behandlingHendelseService = spyk<BehandlingHendelseService>(
                BehandlingHendelseService(
                    FlytJobbRepository(connection),
                    SakService((connection))
                )
            )

            val avklaringsbehovOrkestrator = AvklaringsbehovOrkestrator(
                connection,
                behandlingHendelseService,
            )
            val sak = sak(connection)
            val behandling = behandling(connection, sak)

            // Act
            avklaringsbehovOrkestrator.settBehandlingPåVent(
                behandling.id,
                BehandlingSattPåVent(
                    frist = LocalDate.now().plusDays(1),
                    begrunnelse = "en god begrunnelse",
                    behandlingVersjon = behandling.versjon,
                    bruker = Bruker("123"),
                    grunn = ÅrsakTilSettPåVent.VENTER_PÅ_MEDISINSKE_OPPLYSNINGER,
                )
            )

            verify(exactly = 1) { behandlingHendelseService.stoppet(any(), any()) }
        }
    }

    private fun sak(connection: DBConnection): Sak {
        return PersonOgSakService(connection, FakePdlGateway).finnEllerOpprett(
            ident(),
            Periode(LocalDate.now(), LocalDate.now().plusYears(3))
        )
    }

    private fun behandling(connection: DBConnection, sak: Sak): Behandling {
        return SakOgBehandlingService(connection).finnEllerOpprettBehandling(
            sak.saksnummer,
            listOf(Årsak(EndringType.MOTTATT_SØKNAD))
        ).behandling
    }
}