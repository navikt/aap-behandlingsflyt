package no.nav.aap.behandlingsflyt.kontrakt.sak

enum class Status {
    OPPRETTET,
    UTREDES,
    AVSLUTTET;

    fun erAvsluttet(): Boolean {
        return AVSLUTTET == this
    }
}
