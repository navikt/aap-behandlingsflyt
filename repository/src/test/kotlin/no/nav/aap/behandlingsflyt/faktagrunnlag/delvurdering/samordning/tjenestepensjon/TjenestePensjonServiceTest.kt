package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.tjenestepensjon

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate

class TjenestePensjonServiceTest {

    @Test
    fun `Ulik rekkefølge i lister skal ikke gi endring`() {
        val eksisterende = listOf(
            TjenestePensjonForhold(
                TjenestePensjonOrdning("Navn", "1111", "2222"),
                listOf(
                    TjenestePensjonYtelse(null, YtelseTypeCode.AFP, LocalDate.now(), null, 1L)
                )
            ),
            TjenestePensjonForhold(
                TjenestePensjonOrdning("Navn 2", "123456789", "987654321"),
                listOf(
                    TjenestePensjonYtelse(null, YtelseTypeCode.AFP, LocalDate.now(), null, 1L),
                    TjenestePensjonYtelse(LocalDate.now(), YtelseTypeCode.UFORE, LocalDate.now(), null, 2L),
                )
            )
        )

        val ny = listOf(
            TjenestePensjonForhold(
                TjenestePensjonOrdning("Navn 2", "123456789", "987654321"),
                listOf(
                    TjenestePensjonYtelse(LocalDate.now(), YtelseTypeCode.UFORE, LocalDate.now(), null, 2L),
                    TjenestePensjonYtelse(null, YtelseTypeCode.AFP, LocalDate.now(), null, 1L),
                )
            ),
            TjenestePensjonForhold(
                TjenestePensjonOrdning("Navn", "1111", "2222"),
                listOf(
                    TjenestePensjonYtelse(null, YtelseTypeCode.AFP, LocalDate.now(), null, 1L)
                )
            ),
        )


        assertFalse(TjenestePensjonService.harEndringerITjenestePensjon(eksisterende, ny))
    }

}