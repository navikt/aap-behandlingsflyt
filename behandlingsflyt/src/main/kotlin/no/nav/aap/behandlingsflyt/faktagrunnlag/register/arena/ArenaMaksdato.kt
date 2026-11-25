package no.nav.aap.behandlingsflyt.faktagrunnlag.register.arena

import java.time.LocalDate

/**
 * [vedtakStatus] og [harForlengelseEtter11_12] er null for grunnlag hentet før byttet til samordningsendepunktet.
 * Gjøres ikke-nullable når slike grunnlag er ryddet bort.
 */
data class ArenaMaksdato(
    val arenaSaksnummer: String,
    val vedtakStatus: ArenaVedtakStatus?,
    val maksdato: LocalDate?,
    val vedtakTil: LocalDate?,
    val vedtakId: Int,
    val harForlengelseEtter11_12: Boolean?,
)

enum class ArenaVedtakStatus {
    LØPENDE,
    STANSET,
    AVSLUTTET,
    ANNET,
}
