package no.nav.aap.behandlingsflyt.faktagrunnlag.bistand

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.flate.BistandVurderingLøsningDto
import no.nav.aap.behandlingsflyt.test.FakeUnleash
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource

class BistandVurderingLøsningLøsningDtoTest {

    //TODO alle testene her blir overflødige etter innføring av 11-17 og 11-18
    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `erBehovForAnnenOppfølging kan være en Boolean hvis erBehovForAnnenOppfølging AND erBehovForArbeidsrettetTiltak er false`(
        erBehovForAnnenOppfølging: Boolean
    ) {
        BistandVurderingLøsningDto("hei",
            erBehovForAktivBehandling = false,
            erBehovForArbeidsrettetTiltak = false,
            erBehovForAnnenOppfølging = erBehovForAnnenOppfølging,
            skalVurdereAapIOvergangTilUføre = null,
            skalVurdereAapIOvergangTilArbeid = null,
            overgangBegrunnelse = null,
        )
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `erBehovForAnnenOppfølging kan være null hvis erBehovForAnnenOppfølging OR erBehovForArbeidsrettetTiltak er true`(
        behov: Boolean
    ) {
        BistandVurderingLøsningDto("hei",
            erBehovForAktivBehandling = behov,
            erBehovForArbeidsrettetTiltak = !behov,
            erBehovForAnnenOppfølging = null,
            skalVurdereAapIOvergangTilUføre = null,
            skalVurdereAapIOvergangTilArbeid = null,
            overgangBegrunnelse = null,
        )
    }

    @Test
    fun `erBehovForAnnenOppfølging kan være null hvis erBehovForAnnenOppfølging AND erBehovForArbeidsrettetTiltak er true`() {
        BistandVurderingLøsningDto("hei",
            erBehovForAktivBehandling = true,
            erBehovForArbeidsrettetTiltak = true,
            erBehovForAnnenOppfølging = null,
            skalVurdereAapIOvergangTilUføre = null,
            skalVurdereAapIOvergangTilArbeid = null,
            overgangBegrunnelse = null,
        )
    }

    @ParameterizedTest
    @CsvSource(value = ["true, true", "true, false", "false, true", "false, false"])
    fun `erBehovForAnnenOppfølging kan ikke være en Boolean hvis erBehovForAnnenOppfølging OR erBehovForArbeidsrettetTiltak er true`(
        behov: Boolean,
        erBehovForAnnenOppfølging: Boolean
    ) {
        assertThrows<UgyldigForespørselException> {
            BistandVurderingLøsningDto(
                "hei",
                erBehovForAktivBehandling = behov,
                erBehovForArbeidsrettetTiltak = !behov,
                erBehovForAnnenOppfølging = erBehovForAnnenOppfølging,
                skalVurdereAapIOvergangTilUføre = null,
                skalVurdereAapIOvergangTilArbeid = null,
                overgangBegrunnelse = null,
            ).valider(FakeUnleash)
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `erBehovForAnnenOppfølging kan ikke være en Boolean hvis erBehovForAnnenOppfølging AND erBehovForArbeidsrettetTiltak er true`(
        erBehovForAnnenOppfølging: Boolean
    ) {
        assertThrows<UgyldigForespørselException> {
            BistandVurderingLøsningDto(
                "hei",
                erBehovForAktivBehandling = true,
                erBehovForArbeidsrettetTiltak = true,
                erBehovForAnnenOppfølging = erBehovForAnnenOppfølging,
                skalVurdereAapIOvergangTilUføre = null,
                skalVurdereAapIOvergangTilArbeid = null,
                overgangBegrunnelse = null,
            ).valider(FakeUnleash)
        }
    }

    @Test
    fun `erBehovForAnnenOppfølging kan ikke være null hvis erBehovForAnnenOppfølging AND erBehovForArbeidsrettetTiltak er false`() {
        assertThrows<UgyldigForespørselException> {
            BistandVurderingLøsningDto(
                "hei",
                erBehovForAktivBehandling = false,
                erBehovForArbeidsrettetTiltak = false,
                erBehovForAnnenOppfølging = null,
                skalVurdereAapIOvergangTilUføre = null,
                skalVurdereAapIOvergangTilArbeid = null,
                overgangBegrunnelse = null,
            ).valider(FakeUnleash)
        }
    }
}