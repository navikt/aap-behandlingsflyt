package no.nav.aap.behandlingsflyt.faktagrunnlag.bistand

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.flate.BistandVurderingDto
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource

class BistandVurderingDtoTest {

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `erBehovForAnnenOppfølging kan være en Boolean hvis erBehovForAnnenOppfølging AND erBehovForArbeidsrettetTiltak er false`(
        erBehovForAnnenOppfølging: Boolean
    ) {
        BistandVurderingDto("hei",
            erBehovForAktivBehandling = false,
            erBehovForArbeidsrettetTiltak = false,
            erBehovForAnnenOppfølging = erBehovForAnnenOppfølging
        )
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `erBehovForAnnenOppfølging kan være null hvis erBehovForAnnenOppfølging OR erBehovForArbeidsrettetTiltak er true`(
        behov: Boolean
    ) {
        BistandVurderingDto("hei",
            erBehovForAktivBehandling = behov,
            erBehovForArbeidsrettetTiltak = !behov,
            erBehovForAnnenOppfølging = null
        )
    }

    @Test
    fun `erBehovForAnnenOppfølging kan være null hvis erBehovForAnnenOppfølging AND erBehovForArbeidsrettetTiltak er true`() {
        BistandVurderingDto("hei",
            erBehovForAktivBehandling = true,
            erBehovForArbeidsrettetTiltak = true,
            erBehovForAnnenOppfølging = null
        )
    }

    @ParameterizedTest
    @CsvSource(value = ["true, true", "true, false", "false, true", "false, false"])
    fun `erBehovForAnnenOppfølging kan ikke være en Boolean hvis erBehovForAnnenOppfølging OR erBehovForArbeidsrettetTiltak er true`(
        behov: Boolean,
        erBehovForAnnenOppfølging: Boolean
    ) {
        assertThrows<IllegalArgumentException> {
            BistandVurderingDto(
                "hei",
                erBehovForAktivBehandling = behov,
                erBehovForArbeidsrettetTiltak = !behov,
                erBehovForAnnenOppfølging = erBehovForAnnenOppfølging
            )
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `erBehovForAnnenOppfølging kan ikke være en Boolean hvis erBehovForAnnenOppfølging AND erBehovForArbeidsrettetTiltak er true`(
        erBehovForAnnenOppfølging: Boolean
    ) {
        assertThrows<IllegalArgumentException> {
            BistandVurderingDto(
                "hei",
                erBehovForAktivBehandling = true,
                erBehovForArbeidsrettetTiltak = true,
                erBehovForAnnenOppfølging = erBehovForAnnenOppfølging
            )
        }
    }

    @Test
    fun `erBehovForAnnenOppfølging kan ikke være null hvis erBehovForAnnenOppfølging AND erBehovForArbeidsrettetTiltak er false`() {
        assertThrows<IllegalArgumentException> {
            BistandVurderingDto(
                "hei",
                erBehovForAktivBehandling = false,
                erBehovForArbeidsrettetTiltak = false,
                erBehovForAnnenOppfølging = null
            )
        }
    }
}