package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.samordning.tjenestepensjon

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.tjenestepensjon.TjenestePensjonForhold
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.tjenestepensjon.TjenestePensjonOrdning
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.tjenestepensjon.TjenestePensjonYtelse
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.tjenestepensjon.YtelseTypeCode
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.repository.avklaringsbehov.FakePdlGateway
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
import org.junit.jupiter.api.assertDoesNotThrow
import java.time.LocalDate


class TjenestePensjonRepositoryImplTest {
    private val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(1))

    @Test
    fun `kan lagre og hente tp ytelser`() {
        val dataSource = InitTestDatabase.freshDatabase()
        dataSource.transaction { dbConnect ->
            val sak = sak(dbConnect)
            val behandling = finnEllerOpprettBehandling(dbConnect, sak)

            val tjenestePensjon = listOf(TjenestePensjonForhold(
                ordning = TjenestePensjonOrdning(
                    navn = "Statens PensjonsKasse",
                    tpNr = "3010",
                    orgNr = "123445675645"
                ),
                ytelser = listOf(
                    TjenestePensjonYtelse(
                        innmeldtYtelseFom = null,
                        ytelseType = YtelseTypeCode.ALDER,
                        ytelseIverksattFom = LocalDate.of(2020, 1, 1),
                        ytelseIverksattTom = LocalDate.of(2025,4,22),
                        ytelseId = 1234L
                    ),
                    TjenestePensjonYtelse(
                        innmeldtYtelseFom = null,
                        ytelseType = YtelseTypeCode.BETINGET_TP,
                        ytelseIverksattFom = LocalDate.of(2020, 1, 1),
                        ytelseIverksattTom = null,
                        ytelseId = 1235L
                    )
                )
            ))

            TjenestePensjonRepositoryImpl(dbConnect).lagre(behandling.id, tjenestePensjon)

            val hentetTjenestePensjon = TjenestePensjonRepositoryImpl(dbConnect).hent(behandling.id)

            assertThat(hentetTjenestePensjon).isEqualTo(tjenestePensjon)
        }
    }

    @Test
    fun `test sletting`() {
        InitTestDatabase.freshDatabase().transaction { connection ->
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)
            val tjenestePensjonRepository = TjenestePensjonRepositoryImpl(connection)
            tjenestePensjonRepository.lagre(
                behandling.id,
                listOf(TjenestePensjonForhold(
                    ordning = TjenestePensjonOrdning(
                        navn = "navn",
                        tpNr = "tpNr",
                        orgNr = "orgNr",
                    ),
                    ytelser = listOf( TjenestePensjonYtelse(
                        innmeldtYtelseFom = null,
                        ytelseType = YtelseTypeCode.BETINGET_TP,
                        ytelseIverksattFom = LocalDate.of(2021, 1, 1),
                        ytelseIverksattTom = LocalDate.of(2021, 12, 31),
                        ytelseId = 1235L
                    )),
                )
            ))
            tjenestePensjonRepository.lagre(
                behandling.id,
                listOf(TjenestePensjonForhold(
                    ordning = TjenestePensjonOrdning(
                        navn = "navn",
                        tpNr = "tpNr",
                        orgNr = "orgNr",
                    ),
                    ytelser = listOf( TjenestePensjonYtelse(
                        innmeldtYtelseFom = null,
                        ytelseType = YtelseTypeCode.BETINGET_TP,
                        ytelseIverksattFom = LocalDate.of(2020, 1, 1),
                        ytelseIverksattTom = LocalDate.of(2020, 12, 31),
                        ytelseId = 1235L
                    )),
                )
                ))
            assertDoesNotThrow {
                tjenestePensjonRepository.slett(behandling.id)
            }
        }}

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
}
