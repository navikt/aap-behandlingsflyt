package no.nav.aap.domene.behandling

import no.nav.aap.domene.behandling.avklaringsbehov.Avklaringsbehov
import no.nav.aap.domene.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.domene.behandling.avklaringsbehov.Definisjon
import no.nav.aap.flyt.BehandlingFlyt
import no.nav.aap.flyt.StegStatus
import no.nav.aap.flyt.StegType
import no.nav.aap.flyt.Tilstand
import java.time.LocalDateTime
import java.util.*

class Behandling(
    val id: Long,
    val referanse: UUID = UUID.randomUUID(),
    val sakId: Long,
    val type: BehandlingType,
    private var status: Status = Status.OPPRETTET,
    private var årsaker: List<Årsak> = mutableListOf(),
    private var avklaringsbehov: List<Avklaringsbehov> = mutableListOf(),
    private val flereavklaringsbehov: Avklaringsbehovene = Avklaringsbehovene(),
    private var stegHistorikk: List<StegTilstand> = mutableListOf(),
    private val vilkårsresultat: Vilkårsresultat = Vilkårsresultat(),
    val opprettetTidspunkt: LocalDateTime = LocalDateTime.now()
) : Comparable<Behandling> {

    fun visit(stegTilstand: StegTilstand) {
        if (!stegTilstand.aktiv) {
            throw IllegalStateException("Utvikler feil, prøver legge til steg med aktivtflagg false.")
        }
        if (stegHistorikk.isEmpty() || aktivtSteg() != stegTilstand) {
            stegHistorikk.stream().filter { tilstand -> tilstand.aktiv }.forEach { tilstand -> tilstand.deaktiver() }
            stegHistorikk += stegTilstand
            stegHistorikk = stegHistorikk.sorted()
        }
        validerStegTilstand()

        oppdaterStatus(stegTilstand)
    }

    private fun validerStegTilstand() {
        if (stegHistorikk.isNotEmpty() && stegHistorikk.stream().noneMatch { tilstand -> tilstand.aktiv }) {
            throw IllegalStateException("Utvikler feil, mangler aktivt steg når steghistorikk ikke er tom.")
        }
    }

    private fun oppdaterStatus(stegTilstand: StegTilstand) {
        val stegStatus = stegTilstand.tilstand.steg().status
        if (status != stegStatus) {
            status = stegStatus
        }
    }

    fun leggTil(funnetAvklaringsbehov: List<Definisjon>) {
        funnetAvklaringsbehov.stream()
            .map { definisjon ->
                Avklaringsbehov(
                    definisjon,
                    funnetISteg = aktivtSteg().tilstand.steg()
                )
            }
            .forEach { leggTil(behov = it) }
    }

    fun aktivtSteg(): StegTilstand {
        return stegHistorikk.stream()
            .filter { tilstand -> tilstand.aktiv }
            .findAny()
            .orElse(
                StegTilstand(
                    tilstand = Tilstand(StegType.START_BEHANDLING, StegStatus.START)
                )
            )
    }

    fun settPåVent() {
        status = Status.PÅ_VENT
        leggTil(behov = Avklaringsbehov(Definisjon.MANUELT_SATT_PÅ_VENT, funnetISteg = aktivtSteg().tilstand.steg()))
    }

    private fun leggTil(behov: Avklaringsbehov) {
        val relevantBehov = avklaringsbehov.stream().filter { it.definisjon == behov.definisjon }.findFirst()
        relevantBehov.ifPresentOrElse({it.reåpne()}, {avklaringsbehov += behov})
    }

    fun løsAvklaringsbehov(definisjon: Definisjon, begrunnelse: String, endretAv: String) {
        avklaringsbehov.single { it.definisjon == definisjon }.løs(begrunnelse, endretAv = endretAv)
    }

    fun vilkårsresultat(): Vilkårsresultat = vilkårsresultat
    fun flyt(): BehandlingFlyt = type.flyt()
    fun avklaringsbehov(): List<Avklaringsbehov> = avklaringsbehov.toList()
    fun åpneAvklaringsbehov(): List<Avklaringsbehov> = avklaringsbehov.filter { it.erÅpent() }.toList()
    fun årsaker(): List<Årsak> = årsaker.toList()
    fun stegHistorikk(): List<StegTilstand> = stegHistorikk.toList()
    fun status(): Status = status

    override fun compareTo(other: Behandling): Int {
        return this.opprettetTidspunkt.compareTo(other.opprettetTidspunkt)
    }
}