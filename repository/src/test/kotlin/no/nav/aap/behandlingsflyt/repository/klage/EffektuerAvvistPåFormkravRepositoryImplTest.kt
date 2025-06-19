package no.nav.aap.behandlingsflyt.repository.klage

import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingReferanse
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.effektueravvistpåformkrav.EffektuerAvvistPåFormkravGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.effektueravvistpåformkrav.EffektuerAvvistPåFormkravVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.effektueravvistpåformkrav.ForhåndsvarselKlageFormkrav
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.repository.avklaringsbehov.FakePdlGateway
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.klage.EffektuerAvvistPåFormkravRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
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
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.util.UUID

class EffektuerAvvistPåFormkravRepositoryImplTest {
    private val dataSource = InitTestDatabase.freshDatabase()

    @Test
    fun `Lagrer og henter 'effektuer avvist på formkrav'-grunnlag`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            finnEllerOpprettBehandling(connection, sak)
            val klageBehandling = finnEllerOpprettBehandling(connection, sak, ÅrsakTilBehandling.MOTATT_KLAGE)

            val repository = EffektuerAvvistPåFormkravRepositoryImpl(connection)

            // Lagre grunnlag
            val bestillingsreferanse = BrevbestillingReferanse(UUID.randomUUID())
            val referanse = bestillingsreferanse
            

            repository.lagreVarsel(klageBehandling.id, referanse)

            // Hent grunnlag
            val grunnlag = repository.hentHvisEksisterer(klageBehandling.id)

            val forventetVarsel = ForhåndsvarselKlageFormkrav(
                referanse = referanse,
                datoVarslet = null,
                frist = null
            )
            assertNotNull(grunnlag)
            assertThat(grunnlag).isEqualTo(
                EffektuerAvvistPåFormkravGrunnlag(
                    forventetVarsel,
                    null
                )
            )
        }

    }

    @Test
    fun `Kun én bestilling per behandling`() {
        // TODO: Avgjør om det gir mening med flere varsler
        dataSource.transaction { connection ->
            val sak = sak(connection)
            finnEllerOpprettBehandling(connection, sak)
            val klageBehandling = finnEllerOpprettBehandling(connection, sak, ÅrsakTilBehandling.MOTATT_KLAGE)

            val repository = EffektuerAvvistPåFormkravRepositoryImpl(connection)

            val bestillingsreferanse = BrevbestillingReferanse(UUID.randomUUID())
            val varsel = ForhåndsvarselKlageFormkrav(
                referanse = bestillingsreferanse
            )
            repository.lagreVarsel(
                klageBehandling.id,
                varsel = bestillingsreferanse
            )

            // Hent grunnlag
            val grunnlag = repository.hentHvisEksisterer(klageBehandling.id)

            assertNotNull(grunnlag)
            assertThat(grunnlag.varsel.referanse).isEqualTo(bestillingsreferanse)

            val nyReferanse = BrevbestillingReferanse(UUID.randomUUID())
            
            // Prøv å lagre et nytt varsel
            assertThrows<IllegalStateException> {
                repository.lagreVarsel(klageBehandling.id, nyReferanse)
            }
        }
    }


    @Test
    fun `Skal kopiere varsel fra forrige aktive grunnlag ved lagring av effektueringsvurdering`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            finnEllerOpprettBehandling(connection, sak)
            val klageBehandling = finnEllerOpprettBehandling(connection, sak, ÅrsakTilBehandling.MOTATT_KLAGE)

            val repository = EffektuerAvvistPåFormkravRepositoryImpl(connection)

            val bestillingsreferanse = BrevbestillingReferanse(UUID.randomUUID())
      
            repository.lagreVarsel(klageBehandling.id, bestillingsreferanse)

            // Oppdater vurdering
            repository.lagreVurdering(klageBehandling.id, EffektuerAvvistPåFormkravVurdering(skalEndeligAvvises = true))

            // Hent grunnlag
            val grunnlag = repository.hentHvisEksisterer(klageBehandling.id)

            assertNotNull(grunnlag)
            assertThat(grunnlag.varsel).isEqualTo(
                ForhåndsvarselKlageFormkrav(
                    referanse = bestillingsreferanse,
                    datoVarslet = null,
                    frist = null
                )
            )
            assertThat(grunnlag.vurdering!!.skalEndeligAvvises).isTrue()
        }
    }

    private fun sak(connection: DBConnection): Sak {
        return PersonOgSakService(
            FakePdlGateway,
            PersonRepositoryImpl(connection),
            SakRepositoryImpl(connection)
        ).finnEllerOpprett(ident(), periode)
    }

    private companion object {
        private val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))
    }
}