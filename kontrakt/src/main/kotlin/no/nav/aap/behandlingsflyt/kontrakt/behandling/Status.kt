package no.nav.aap.behandlingsflyt.kontrakt.behandling

public enum class Status {
    OPPRETTET,
    UTREDES,
    IVERKSETTES,
    AVSLUTTET;

    public fun erAvsluttet(): Boolean {
        return AVSLUTTET == this || this == IVERKSETTES
    }

    public fun erÅpen(): Boolean {
        return this == OPPRETTET || this == UTREDES
    }
}
