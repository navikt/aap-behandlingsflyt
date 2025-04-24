package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.samordning.tjenestepensjon

import no.nav.aap.behandlingsflyt.faktagrunnlag.GrunnlagKopierer
import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.tjenestepensjon.SamhandlerForholdDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.tjenestepensjon.SamhandlerYtelseDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.tjenestepensjon.TjenestePensjonForhold
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.tjenestepensjon.TjenestePensjonOrdning
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.tjenestepensjon.TjenestePensjonYtelse
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.tjenestepensjon.TpOrdning
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.tjenestepensjon.YtelseTypeCode
import no.nav.aap.behandlingsflyt.repository.avklaringsbehov.FakePdlGateway
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Årsak
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.ÅrsakTilBehandling
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


class TjenestePensjonRepositoryImplTest() {

    private val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(1))

    @Test
    fun `kan lagre og hente tp ytelser`() {
        //lag testen for meg
        val dataSource = InitTestDatabase.dataSource
        dataSource.transaction { dbConnect ->
            val sak = sak(dbConnect)
            val behandling = behandling(dbConnect, sak)

            val tjenestePensjon = listOf(TjenestePensjonForhold(
                ordning = TjenestePensjonOrdning(
                    navn = "Statens Pensjon Kasse",
                    tpNr = "3010",
                    orgNr = "123445675645"
                ),
                ytelser = listOf(
                    TjenestePensjonYtelse(
                        datoInnmeldtYtelseFom = null,
                        ytelseType = YtelseTypeCode.ALDER,
                        datoYtelseIverksattFom = LocalDate.of(2020, 1, 1),
                        datoYtelseIverksattTom = LocalDate.of(2025,4,22),
                        ytelseId = 1234L
                    ),
                    TjenestePensjonYtelse(
                        datoInnmeldtYtelseFom = null,
                        ytelseType = YtelseTypeCode.BETINGET_TP,
                        datoYtelseIverksattFom = LocalDate.of(2020, 1, 1),
                        datoYtelseIverksattTom = null,
                        ytelseId = 1235L
                    )
                )
            ))

            TjenestePensjonRepositoryImpl(dbConnect).lagre(behandling.id, tjenestePensjon)

            val hentetTjenestePensjon = TjenestePensjonRepositoryImpl(dbConnect).hent(behandling.id)

            assertThat(hentetTjenestePensjon).isEqualTo(tjenestePensjon)
        }
    }

    private fun sak(connection: DBConnection): Sak {
        return PersonOgSakService(
            FakePdlGateway,
            PersonRepositoryImpl(connection),
            SakRepositoryImpl(connection)
        ).finnEllerOpprett(
            ident(),
            periode
        )
    }

    private fun behandling(connection: DBConnection, sak: Sak): Behandling {
        return SakOgBehandlingService(
            GrunnlagKopierer(connection), SakRepositoryImpl(connection),
            BehandlingRepositoryImpl(connection)
        ).finnEllerOpprettBehandling(
            sak.saksnummer,
            listOf(Årsak(ÅrsakTilBehandling.MOTTATT_SØKNAD))
        ).behandling
    }
}
