package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov

import no.nav.aap.behandlingsflyt.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.flyt.BehandlingFlyt
import no.nav.aap.behandlingsflyt.flyt.steg.StegType
import java.time.LocalDateTime

class Avklaringsbehovene(
    private val repository: AvklaringsbehovOperasjonerRepository,
    private val behandlingId: BehandlingId
) {
    private var avklaringsbehovene: MutableList<Avklaringsbehov> = repository.hent(behandlingId).toMutableList()

    fun løsAvklaringsbehov(definisjon: Definisjon, begrunnelse: String, endretAv: String, kreverToTrinn: Boolean) {
        val avklaringsbehov = alle().single { it.definisjon == definisjon }
        avklaringsbehov.løs(begrunnelse = begrunnelse, endretAv = endretAv, kreverToTrinn = kreverToTrinn)
        repository.kreverToTrinn(avklaringsbehov.id, kreverToTrinn)
        repository.endre(avklaringsbehov)
    }

    fun løsAvklaringsbehov(definisjon: Definisjon, begrunnelse: String, endretAv: String) {
        if (definisjon.erFrivillig()) {
            if (hentBehovForDefinisjon(definisjon) == null) {
                // Legger til frivillig behov
                leggTil(
                    Avklaringsbehov(
                        id = Long.MAX_VALUE,
                        definisjon = definisjon,
                        funnetISteg = definisjon.løsesISteg,
                        kreverToTrinn = null
                    )
                )
            }
        }
        val avklaringsbehov = alle().single { it.definisjon == definisjon }
        avklaringsbehov.løs(begrunnelse, endretAv = endretAv)
        repository.endre(avklaringsbehov)
    }

    fun leggTil(definisjoner: List<Definisjon>, stegType: StegType) {
        definisjoner.forEach { definisjon ->
            repository.opprett(
                behandlingId = behandlingId,
                definisjon = definisjon,
                funnetISteg = stegType
            )
            //TODO: Må legge til avklaringsbehov til listen
            //avklaringsbehovene.add(avklaringsbehov)
        }
    }

    fun leggTil(avklaringsbehov: Avklaringsbehov) {
        val relevantBehov = alle().firstOrNull { it.definisjon == avklaringsbehov.definisjon }

        //TODO: Flytte denne sjekken et hakk opp?
        if (relevantBehov != null) {
            repository.endreAvklaringsbehov(
                avklaringsbehovId = relevantBehov.id,
                status = avklaringsbehov.status(),
                begrunnelse = "",
                opprettetAv = "system"
            )
            relevantBehov.reåpne()
        } else {
            repository.opprett(
                behandlingId = behandlingId,
                definisjon = avklaringsbehov.definisjon,
                funnetISteg = avklaringsbehov.funnetISteg
            )
            //TODO: Her legger vi til en avklaring som ikke er gyldig
            avklaringsbehovene.add(avklaringsbehov)
        }
    }

    fun alle(): List<Avklaringsbehov> {
        avklaringsbehovene = repository.hent(behandlingId).toMutableList()
        return avklaringsbehovene.toList()
    }

    fun alleInkludertFrivillige(flyt: BehandlingFlyt): List<Avklaringsbehov> {
        val eksisterendeBehov = alle()
        val list = flyt.frivilligeAvklaringsbehovRelevantForFlyten()
            .filter { definisjon -> eksisterendeBehov.none { behov -> behov.definisjon == definisjon } }
            .map { definisjon ->
                Avklaringsbehov(
                    id = Long.MAX_VALUE,
                    definisjon = definisjon,
                    historikk = mutableListOf(
                        Endring(
                            status = Status.OPPRETTET,
                            tidsstempel = LocalDateTime.now(),
                            begrunnelse = "",
                            endretAv = "system"
                        )
                    ),
                    funnetISteg = definisjon.løsesISteg,
                    kreverToTrinn = null
                )
            }.toMutableList()
        list.addAll(eksisterendeBehov)

        return list.toList()
    }

    fun åpne(): List<Avklaringsbehov> {
        return alle().filter { it.erÅpent() }.toList()
    }

    fun tilbakeførtFraBeslutter(): List<Avklaringsbehov> {
        return alle().filter { it.status() == Status.SENDT_TILBAKE_FRA_BESLUTTER }.toList()
    }

    fun hentBehovForDefinisjon(definisjon: Definisjon): Avklaringsbehov? {
        return alle().filter { it.definisjon == definisjon }.singleOrNull()
    }

    fun hentBehovForDefinisjon(definisjoner: List<Definisjon>): List<Avklaringsbehov> {
        return alle().filter { it.definisjon in definisjoner }.toList()
    }

    fun vurderTotrinn(definisjon: Definisjon, godkjent: Boolean, begrunnelse: String, vurdertAv: String) {
        val avklaringsbehov = alle().single { it.definisjon == definisjon }
        avklaringsbehov.vurderTotrinn(begrunnelse, godkjent)
        repository.endre(avklaringsbehov)
    }

    fun avbryt(definisjon: Definisjon) {
        val avklaringsbehov = alle().single { it.definisjon == definisjon }
        avklaringsbehov.avbryt()
        repository.endre(avklaringsbehov)
    }

    fun harHattAvklaringsbehov(): Boolean {
        return alle().any { avklaringsbehov -> avklaringsbehov.erIkkeAvbrutt() }
    }

    fun harHattAvklaringsbehovSomHarKrevdToTrinn(): Boolean {
        return alle()
            .filter { avklaringsbehov -> avklaringsbehov.erIkkeAvbrutt() }
            .any { avklaringsbehov -> avklaringsbehov.erTotrinn() && !avklaringsbehov.erTotrinnsVurdert() }
    }

    fun harIkkeForeslåttVedtak(): Boolean {
        return alle()
            .filter { avklaringsbehov -> avklaringsbehov.erForeslåttVedtak() }
            .none { it.status() == Status.AVSLUTTET }
    }

    fun skalTilbakeføresEtterTotrinnsVurdering(): Boolean {
        return tilbakeførtFraBeslutter().isNotEmpty()
    }

    fun reåpne(definisjon: Definisjon) {
        val avklaringsbehov = avklaringsbehovene.single { it.definisjon == definisjon }
        avklaringsbehov.reåpne()
        repository.endre(avklaringsbehov)
    }

    fun harVærtSendtTilbakeFraBeslutterTidligere(): Boolean {
        return alle().any { avklaringsbehov -> avklaringsbehov.harVærtSendtTilbakeFraBeslutterTidligere() }
    }

    fun ingenEndring(avklaringsbehov: Avklaringsbehov) {
        løsAvklaringsbehov(
            avklaringsbehov.definisjon,
            "Ingen endring fra forrige vurdering",
            "saksbehandler"
        ) // TODO: Hente fra sikkerhetcontext
    }
}