package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.medlemskaplovvalg

import no.nav.aap.behandlingsflyt.behandling.vilkår.medlemskap.KanBehandlesAutomatiskVurdering
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class LovvalgDelvurderingJsonTest {
    @Test
    fun `leser versjon 1 uten valgfrie detaljer`() {
        val json = """{"kanBehandlesAutomatisk":false,"tilhørighetVurdering":[]}"""
        assertThat(LovvalgDelvurderingJson.deserialiser(1, json))
            .isEqualTo(KanBehandlesAutomatiskVurdering(false, emptyList()))
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
