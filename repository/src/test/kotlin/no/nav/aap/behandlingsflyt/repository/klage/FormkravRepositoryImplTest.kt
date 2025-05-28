package no.nav.aap.behandlingsflyt.repository.klage

import no.nav.aap.behandlingsflyt.behandling.søknad.TrukketSøknadService
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.formkrav.FormkravVurdering
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.repository.avklaringsbehov.AvklaringsbehovRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.avklaringsbehov.FakePdlGateway
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.klage.FormkravRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.søknad.TrukketSøknadRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.ident
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class FormkravRepositoryImplTest {
    private val dataSource = InitTestDatabase.freshDatabase()

    @Test
    fun `Lagrer og henter formkrav`() {
        dataSource.transaction { connection ->

            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)

            val formkravRepository = FormkravRepositoryImpl(connection)
            val formkrav = FormkravVurdering(
                begrunnelse = "Begrunnelse",
                erBrukerPart = true,
                erFristOverholdt = true,
                erKonkret = true,
                erSignert = true,
                vurdertAv = "ident",
                likevelBehandles = null
            )

            formkravRepository.lagre(behandling.id, formkrav)

            val grunnlag = formkravRepository.hentHvisEksisterer(behandling.id)

            assertThat(grunnlag?.vurdering).isEqualTo(formkrav)
        }
    }

    private fun sak(connection: DBConnection): Sak {
        return PersonOgSakService(
            FakePdlGateway,
            PersonRepositoryImpl(connection),
            SakRepositoryImpl(connection),
            BehandlingRepositoryImpl(connection),
            TrukketSøknadService(
                AvklaringsbehovRepositoryImpl(connection),
                TrukketSøknadRepositoryImpl(connection)
            ),
        ).finnEllerOpprett(ident(), periode)
    }

    private companion object {
        private val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))
    }
}