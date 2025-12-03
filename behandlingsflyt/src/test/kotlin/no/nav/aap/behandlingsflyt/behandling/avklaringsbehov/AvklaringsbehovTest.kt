package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov

import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Status
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.test.februar
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class AvklaringsbehovTest {
    @Test
    fun `kun bestille brev er et helautomatisk avklaringsbehov`() {
        StegType.entries.forEach { stegtype ->
            Definisjon.entries.forEach { definisjon ->
                if (definisjon != Definisjon.BESTILL_BREV) {
                    assertThat(
                        Avklaringsbehov(
                            id = 1,
                            definisjon = definisjon,
                            funnetISteg = stegtype,
                            kreverToTrinn = false
                        ).erAutomatisk()
                    ).isFalse()
                }
            }
        }

        val ab = Avklaringsbehov(
            id = 1,
            definisjon = Definisjon.BESTILL_BREV,
            funnetISteg = StegType.BREV,
            kreverToTrinn = false
        )

        assertThat(ab.erAutomatisk()).isTrue()
    }

    @Test
    fun `Skal ikke kunne gjenåpne et allerede åpnet behov`() {
        val ab = Avklaringsbehov(
            id = 1,
            definisjon = Definisjon.SKRIV_VEDTAKSBREV,
            funnetISteg = StegType.BREV,
            kreverToTrinn = false
        )

        org.junit.jupiter.api.assertThrows<IllegalArgumentException> {
            ab.reåpne(
                perioderVedtaketBehøverVurdering = null,
                perioderSomIkkeErTilstrekkeligVurdert = null
            )
        }
    }

    @Test
    fun `Periodene som returneres skal være for det siste åpne avklaringsbehovet i aktivhistorikk`() {
        val behov = Avklaringsbehov(
            id = 1,
            definisjon = Definisjon.SKRIV_VEDTAKSBREV,
            funnetISteg = StegType.AVKLAR_SYKDOM,
            kreverToTrinn = false,
            historikk = mutableListOf(
                Endring(
                    status = Status.OPPRETTET,
                    begrunnelse = "Første behov",
                    endretAv = "test",
                    tidsstempel = LocalDateTime.now(),
                    perioderVedtaketBehøverVurdering = setOf(Periode(2 januar 2020, 1 februar 2020)),
                    perioderSomIkkeErTilstrekkeligVurdert = setOf(),
                )
            )
        )
        assertThat(behov.perioderVedtaketBehøverVurdering()).containsExactly(Periode(2 januar 2020, 1 februar 2020))
        assertThat(behov.perioderSomIkkeErTilstrekkeligVurdert()).isEmpty()

        behov.oppdaterPerioder(
            perioderVedtaketBehøverVurdering = setOf(Periode(1 januar 2020, 1 februar 2020)),
            perioderSomIkkeErTilstrekkeligVurdert = setOf(Periode(1 januar 2020, 2 januar 2020))
        )
        assertThat(behov.perioderVedtaketBehøverVurdering()).containsExactly(Periode(1 januar 2020, 1 februar 2020))
        assertThat(behov.perioderSomIkkeErTilstrekkeligVurdert()).containsExactly(Periode(1 januar 2020, 2 januar 2020))

        behov.løs("Løst", "test")

        behov.reåpne(
            perioderVedtaketBehøverVurdering = setOf(Periode(1 januar 2020, 1 februar 2020)),
            perioderSomIkkeErTilstrekkeligVurdert = null
        )

        assertThat(behov.perioderVedtaketBehøverVurdering()).containsExactly(Periode(1 januar 2020, 1 februar 2020))
        assertThat(behov.perioderSomIkkeErTilstrekkeligVurdert() == null)

        behov.avbryt()

        assertThat(behov.perioderVedtaketBehøverVurdering()).isNull()
        assertThat(behov.perioderSomIkkeErTilstrekkeligVurdert()).isNull()

        behov.reåpne(
            perioderVedtaketBehøverVurdering = setOf(Periode(1 februar 2020, 2 februar 2020)),
            perioderSomIkkeErTilstrekkeligVurdert = setOf(Periode(1 februar 2020, 2 februar 2020))
        )
        assertThat(behov.perioderVedtaketBehøverVurdering()).containsExactly(Periode(1 februar 2020, 2 februar 2020))
        assertThat(behov.perioderSomIkkeErTilstrekkeligVurdert()).containsExactly(
            Periode(
                1 februar 2020,
                2 februar 2020
            )
        )
    }
}