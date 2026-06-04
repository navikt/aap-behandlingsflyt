package no.nav.aap.behandlingsflyt.sakogbehandling.sak

import com.fasterxml.jackson.databind.node.TextNode
import com.fasterxml.jackson.databind.node.BooleanNode
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.komponenter.json.DefaultJsonMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.UUID

class PersonTest {
    @Test
    fun `Person kan leses tilbake hvis lagret ned i faktagrunnlag`() {
        val person = Person(
            id = PersonId(1),
            referanse = UUID.randomUUID(),
            identer = listOf(
                Ident("1".repeat(11), true),
                Ident("2".repeat(11), false),
            )
        )
        val deserialized = DefaultJsonMapper.fromJson<Person>(DefaultJsonMapper.toJson(person))
        /* Person overrider equals, så sjekker manuelt hvert enklet felt. */
        assertThat(deserialized.id).isEqualTo(person.id)
        assertThat(deserialized.referanse).isEqualTo(person.referanse)
        assertThat(deserialized.identer()).isEqualTo(person.identer())
    }

    @Test
    fun `Person har stabilt format uten ekstra wrappere`() {
        val person = Person(
            id = PersonId(1),
            referanse = UUID.randomUUID(),
            identer = listOf(
                Ident("1".repeat(11), true),
                Ident("2".repeat(11), false),
            )
        )
        val json = DefaultJsonMapper.objectMapper().readTree(DefaultJsonMapper.toJson(person))

        assertThat(json["id"].isIntegralNumber).isTrue()
        assertThat(json["referanse"]).isInstanceOf(TextNode::class.java)
        assertThat(json["identer"].elements().asSequence().toList())
            .allSatisfy {
                assertThat(it["identifikator"]).isInstanceOf(TextNode::class.java)
                assertThat(it["aktivIdent"]).isInstanceOf(BooleanNode::class.java)
            }
    }
}