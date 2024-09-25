package no.nav.aap.behandlingsflyt.kontrakt.behandling

enum class Status {
    OPPRETTET,
    UTREDES,
    AVSLUTTET;

    fun erAvsluttet(): Boolean {
        return AVSLUTTET == this
    }
}
