package no.nav.aap.behandlingsflyt.kontrakt.behandling

enum class Status {
    OPPRETTET,
    UTREDES,
    IVERKSETTES,
    AVSLUTTET;

    fun erAvsluttet(): Boolean {
        return AVSLUTTET == this || this == IVERKSETTES
    }
}
