package no.nav.aap.behandlingsflyt.repository.klage

import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.fullmektig.Adresse
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.fullmektig.FullmektigVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.fullmektig.IdentMedType
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.fullmektig.IdentType
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.fullmektig.NavnOgAdresse
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.help.sak
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.klage.FullmektigRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class FullmektigRepositoryImplTest {
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

    @Test
    fun `Lagrer og henter ut med ident`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            finnEllerOpprettBehandling(connection, sak)
            val klageBehandling = finnEllerOpprettBehandling(connection, sak, Vurderingsbehov.MOTATT_KLAGE)

            val fullmektigRepository = FullmektigRepositoryImpl(connection)
            val vurdering = FullmektigVurdering(
                harFullmektig = true,
                fullmektigIdent = IdentMedType("12345678901", IdentType.FNR_DNR),
                vurdertAv = "Saksbehandler"
            )

            fullmektigRepository.lagre(klageBehandling.id, vurdering)
            val grunnlag = fullmektigRepository.hentHvisEksisterer(klageBehandling.id)!!
            assertThat(grunnlag.vurdering).extracting(
                FullmektigVurdering::harFullmektig,
                FullmektigVurdering::fullmektigIdent,
                FullmektigVurdering::fullmektigNavnOgAdresse,
                FullmektigVurdering::vurdertAv
            ).containsExactly(true, IdentMedType("12345678901", IdentType.FNR_DNR), null, "Saksbehandler")
        }
    }

    @Test
    fun `Lagrer, henter ut og sletter fullmektigvurdering med navn og adresse`() {
        val klagebehandling = dataSource.transaction { connection ->
            val sak = sak(connection)
            finnEllerOpprettBehandling(connection, sak)
            val klagebehandling = finnEllerOpprettBehandling(connection, sak, Vurderingsbehov.MOTATT_KLAGE)

            val navnOgAdresse = NavnOgAdresse(
                navn = "Ola Nordmann", adresse = Adresse(
                    adresselinje1 = "Gate 1", postnummer = "1188", poststed = "Oslo", landkode = "NO"
                )
            )

            val fullmektigRepository = FullmektigRepositoryImpl(connection)
            val vurdering = FullmektigVurdering(
                harFullmektig = true, fullmektigNavnOgAdresse = navnOgAdresse, vurdertAv = "Saksbehandler"
            )

            fullmektigRepository.lagre(klagebehandling.id, vurdering)
            val grunnlag = fullmektigRepository.hentHvisEksisterer(klagebehandling.id)!!
            assertThat(grunnlag.vurdering).extracting(
                FullmektigVurdering::harFullmektig,
                FullmektigVurdering::fullmektigIdent,
                FullmektigVurdering::fullmektigNavnOgAdresse,
                FullmektigVurdering::vurdertAv
            ).containsExactly(true, null, navnOgAdresse, "Saksbehandler")

            klagebehandling
        }

        dataSource.transaction {
            FullmektigRepositoryImpl(it).slett(klagebehandling.id)
        }
        val uthentetEtterSletting = dataSource.transaction {
            FullmektigRepositoryImpl(it).hentHvisEksisterer(klagebehandling.id)
        }
        assertThat(uthentetEtterSletting).isNull()
    }
}