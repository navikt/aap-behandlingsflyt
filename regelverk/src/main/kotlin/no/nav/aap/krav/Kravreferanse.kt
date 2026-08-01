package no.nav.aap.krav

import java.util.UUID

@JvmInline
value class Kravreferanse(val verdi: UUID) {
    companion object {
        fun ny(): Kravreferanse = Kravreferanse(UUID.randomUUID())
    }
}