package no.nav.aap.behandlingsflyt.domene.behandling

import no.nav.aap.behandlingsflyt.domene.behandling.avklaringsbehov.Avklaringsbehov
import no.nav.aap.behandlingsflyt.domene.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.domene.behandling.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.domene.behandling.avklaringsbehov.Status.AVSLUTTET
import no.nav.aap.behandlingsflyt.flyt.BehandlingFlyt
import no.nav.aap.behandlingsflyt.flyt.steg.StegStatus
import no.nav.aap.behandlingsflyt.flyt.steg.StegType
import no.nav.aap.behandlingsflyt.flyt.steg.Tilstand
import no.nav.aap.behandlingsflyt.flyt.vilkår.Vilkårsresultat
import java.time.LocalDateTime
import java.util.*

class Behandling(
    val id: Long,
    val referanse: UUID = UUID.randomUUID(),
    val sakId: Long,
    val type: BehandlingType,
    private var status: Status = Status.OPPRETTET,
    private var årsaker: List<Årsak> = mutableListOf(),
    private val avklaringsbehovene: Avklaringsbehovene = Avklaringsbehovene(),
    private var stegHistorikk: List<StegTilstand> = mutableListOf(),
    private val vilkårsresultat: Vilkårsresultat = Vilkårsresultat(),
    val opprettetTidspunkt: LocalDateTime = LocalDateTime.now()
) : Comparable<Behandling> {

    private val flyt: BehandlingFlyt = type.flyt()

    fun flyt(): BehandlingFlyt = flyt

    fun visit(stegTilstand: StegTilstand) {
        if (!stegTilstand.aktiv) {
            throw IllegalStateException("Utvikler feil, prøver legge til steg med aktivtflagg false.")
        }
        if (stegHistorikk.isEmpty() || aktivtStegTilstand() != stegTilstand) {
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

    fun status(): Status = status

    fun stegHistorikk(): List<StegTilstand> = stegHistorikk.toList()

    private fun aktivtStegTilstand(): StegTilstand {
        return stegHistorikk.stream()
            .filter { tilstand -> tilstand.aktiv }
            .findAny()
            .orElse(
                StegTilstand(
                    tilstand = Tilstand(StegType.START_BEHANDLING, StegStatus.START)
                )
            )
    }
    fun aktivtSteg(): StegType {
        return stegHistorikk.stream()
            .filter { tilstand -> tilstand.aktiv }
            .findAny()
            .orElse(
                StegTilstand(
                    tilstand = Tilstand(StegType.START_BEHANDLING, StegStatus.START)
                )
            ).tilstand.steg()
    }
    fun settPåVent() {
        status = Status.PÅ_VENT
        avklaringsbehovene.leggTil(
            Avklaringsbehov(
                Definisjon.MANUELT_SATT_PÅ_VENT,
                funnetISteg = aktivtSteg()
            )
        )
    }
    fun vilkårsresultat(): Vilkårsresultat = vilkårsresultat

    fun årsaker(): List<Årsak> = årsaker.toList()

    fun leggTil(funnetAvklaringsbehov: List<Definisjon>) {
        avklaringsbehovene.leggTil(funnetAvklaringsbehov, aktivtSteg())
    }

    fun avklaringsbehov(): List<Avklaringsbehov> = avklaringsbehovene.alle()
    fun avklaringsbehovene(): Avklaringsbehovene = avklaringsbehovene
    fun åpneAvklaringsbehov(): List<Avklaringsbehov> = avklaringsbehovene.åpne()

    fun harHattAvklaringsbehov(): Boolean {
        return avklaringsbehov().any { avklaringsbehov -> avklaringsbehov.erIkkeAvbrutt() }
    }

    fun harIkkeForeslåttVedtak(): Boolean {
        return avklaringsbehov()
            .filter { avklaringsbehov -> avklaringsbehov.erForeslåttVedtak() }
            .none { it.status() == AVSLUTTET }
    }

    override fun compareTo(other: Behandling): Int {
        return this.opprettetTidspunkt.compareTo(other.opprettetTidspunkt)
    }
}