package no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter

import no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov
import no.nav.aap.komponenter.json.DefaultJsonMapper
import org.assertj.core.api.Assertions.assertThat
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

class AnnetRelevantDokumentV1Test {
    @Test
    fun `deserialisere v2`() {
        @Language("JSON")
        val json = """
            {
              "meldingType" : "AnnetRelevantDokumentV2",
              "årsakerTilBehandling" : [ "SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND" ],
              "underkategori" : "HELSEOPPLYSNINGER"
            }
        """.trimIndent()

        val deserialized = DefaultJsonMapper.fromJson<Melding>(json)

        assertThat(deserialized).isEqualTo(
            AnnetRelevantDokumentV2(
                årsakerTilBehandling = listOf(Vurderingsbehov.SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND),
                underkategori = AnnetRelevantDokumentUnderkategori.HELSEOPPLYSNINGER
            )
        )
    }
}