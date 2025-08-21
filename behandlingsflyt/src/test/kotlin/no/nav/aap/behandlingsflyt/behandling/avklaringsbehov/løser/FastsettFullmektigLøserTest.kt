package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import io.mockk.Called
import io.mockk.mockk
import io.mockk.verify
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.FastsettFullmektigLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.FullmektigLøsningDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.fullmektig.FullmektigRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.fullmektig.IdentMedType
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.fullmektig.IdentType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class FastsettFullmektigLøserTest {

    private val repository = mockk<FullmektigRepository>(relaxed = true)
    private val løser = FastsettFullmektigLøser(repository)

    @Test
    fun `Fullmektig er organisasjon, men har ugyldig orgnr`() {
        val løsning = FastsettFullmektigLøsning(
            FullmektigLøsningDto(
                harFullmektig = true,
                fullmektigIdentMedType = IdentMedType("000000000", IdentType.ORGNR),
                fullmektigNavnOgAdresse = null,
            )
        )

        assertThrows<IllegalStateException> {
            løser.løs(mockk(relaxed = true), løsning)
        }

        verify { repository wasNot Called }
    }

    @Test
    fun `Fullmektig er organisasjon og har gyldig orgnr`() {
        val løsning = FastsettFullmektigLøsning(
            FullmektigLøsningDto(
                harFullmektig = true,
                fullmektigIdentMedType = IdentMedType("958935420", IdentType.ORGNR),
                fullmektigNavnOgAdresse = null,
            )
        )

        løser.løs(mockk(relaxed = true), løsning)

        verify(exactly = 1) { repository.lagre(any(), any()) }
    }


    @Test
    fun `Fullmektig er person, men har ugyldig ident`() {
        val løsning = FastsettFullmektigLøsning(
            FullmektigLøsningDto(
                harFullmektig = true,
                fullmektigIdentMedType = IdentMedType("000000000", IdentType.FNR_DNR),
                fullmektigNavnOgAdresse = null,
            )
        )

        assertThrows<IllegalStateException> {
            løser.løs(mockk(relaxed = true), løsning)
        }

        verify { repository wasNot Called }
    }

    @Test
    fun `Fullmektig er person og har gyldig ident`() {
        val løsning = FastsettFullmektigLøsning(
            FullmektigLøsningDto(
                harFullmektig = true,
                fullmektigIdentMedType = IdentMedType("04438346142", IdentType.FNR_DNR),
                fullmektigNavnOgAdresse = null,
            )
        )

        løser.løs(mockk(relaxed = true), løsning)

        verify(exactly = 1) { repository.lagre(any(), any()) }
    }
}
