package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.medlemskaplovvalg

import no.nav.aap.behandlingsflyt.behandling.vilkår.medlemskap.KanBehandlesAutomatiskVurdering
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class LovvalgDelvurderingJsonTest {
    @Test
    fun `leser versjon 1 uten valgfrie detaljer`() {
        val json = """{"kanBehandlesAutomatisk":false,"tilhørighetVurdering":[], "periode":{"fom":"2026-01-01","tom":"2026-01-31"}}"""
        assertThat(LovvalgDelvurderingJson.deserialiser(1, json))
            .isEqualTo(KanBehandlesAutomatiskVurdering(false, emptyList(), Periode(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31))))
    }

    @Test
    fun `ukjent versjon skal gi feil fremfor en tom vurdering`() {
        val feil = assertThrows<IllegalArgumentException> {
            LovvalgDelvurderingJson.deserialiser(2, "{}")
        }
        assertThat(feil).hasMessageContaining("Ukjent serialiseringsversjon")
    }

    @Test
    fun `ugyldig json skal gi feil fremfor en tom vurdering`() {
        assertThrows<Exception> {
            LovvalgDelvurderingJson.deserialiser(1, "ikke json")
        }
    }
}
