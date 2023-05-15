package no.nav.aap.domene.behandling

enum class Status {
    OPPRETTET,
    UTREDES,
    IVERKSETTES,
    AVSLUTTET,
    HENLAGT;

    fun erAvsluttet(): Boolean {
        return setOf(IVERKSETTES, AVSLUTTET).contains(this)
    }
}
