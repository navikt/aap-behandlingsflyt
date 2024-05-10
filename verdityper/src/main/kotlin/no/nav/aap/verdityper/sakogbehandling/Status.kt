package no.nav.aap.verdityper.sakogbehandling

enum class Status {
    OPPRETTET,
    UTREDES,
    AVSLUTTET;

    fun erAvsluttet(): Boolean {
        return AVSLUTTET == this
    }
}
