package no.nav.aap.behandlingsflyt.avklaringsbehov

import no.nav.aap.behandlingsflyt.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.flyt.steg.StegType
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(Avklaringsbehovene::class.java)

class Avklaringsbehovene(
    private val repository: AvklaringsbehovOperasjonerRepository,
    private val behandlingId: BehandlingId
) : AvklaringsbehoveneDecorator {
    private var avklaringsbehovene: MutableList<Avklaringsbehov> = repository.hent(behandlingId).toMutableList()

    fun ingenEndring(avklaringsbehov: Avklaringsbehov) {
        løsAvklaringsbehov(
            avklaringsbehov.definisjon,
            "Ingen endring fra forrige vurdering",
            "saksbehandler"
        ) // TODO: Hente fra sikkerhetcontext
    }

    fun løsAvklaringsbehov(definisjon: Definisjon, begrunnelse: String, endretAv: String, kreverToTrinn: Boolean? = null) {
        val avklaringsbehov = alle().single { it.definisjon == definisjon }
        if (kreverToTrinn == null) {
            avklaringsbehov.løs(begrunnelse, endretAv = endretAv)
        } else {
            avklaringsbehov.løs(begrunnelse = begrunnelse, endretAv = endretAv, kreverToTrinn = kreverToTrinn)
            repository.kreverToTrinn(avklaringsbehov.id, kreverToTrinn)
        }
        repository.endre(avklaringsbehov)
    }

    fun leggTilFrivilligHvisMangler(definisjon: Definisjon) {
        if (definisjon.erFrivillig()) {
            if (hentBehovForDefinisjon(definisjon) == null) {
                // Legger til frivillig behov
                leggTil(listOf(definisjon), definisjon.løsesISteg)
            }
        }
    }

    /**
     * Legger til nye avklaringsbehov.
     *
     * NB! Dersom avklaringsbehovet finnes fra før og er åpent så ignorerer vi det nye behovet, mens dersom det er avsluttet så reåpner vi det.
     */
    fun leggTil(definisjoner: List<Definisjon>, stegType: StegType) {
        definisjoner.forEach { definisjon ->
            val avklaringsbehov = hentBehovForDefinisjon(definisjon)
            if (avklaringsbehov != null) {
                if (avklaringsbehov.erAvsluttet()) {
                    avklaringsbehov.reåpne()
                    repository.endre(avklaringsbehov)
                } else {
                    log.warn("Forsøkte å legge til et avklaringsbehov som allerede eksisterte")
                }
            } else {
                repository.opprett(
                    behandlingId = behandlingId,
                    definisjon = definisjon,
                    funnetISteg = stegType
                )
                //TODO: Må legge til avklaringsbehov til listen
                //avklaringsbehovene.add(avklaringsbehov)
            }
        }
    }

    fun vurderTotrinn(definisjon: Definisjon, godkjent: Boolean, begrunnelse: String, vurdertAv: String) {
        val avklaringsbehov = alle().single { it.definisjon == definisjon }
        avklaringsbehov.vurderTotrinn(begrunnelse, godkjent, vurdertAv)
        repository.endre(avklaringsbehov)
    }

    fun avbryt(definisjon: Definisjon) {
        val avklaringsbehov = alle().single { it.definisjon == definisjon }
        avklaringsbehov.avbryt()
        repository.endre(avklaringsbehov)
    }

    fun reåpne(definisjon: Definisjon) {
        val avklaringsbehov = avklaringsbehovene.single { it.definisjon == definisjon }
        avklaringsbehov.reåpne()
        repository.endre(avklaringsbehov)
    }

    override fun alle(): List<Avklaringsbehov> {
        avklaringsbehovene = repository.hent(behandlingId).toMutableList()
        return avklaringsbehovene.toList()
    }

    fun åpne(): List<Avklaringsbehov> {
        return alle().filter { it.erÅpent() }.toList()
    }

    fun skalTilbakeføresEtterTotrinnsVurdering(): Boolean {
        return tilbakeførtFraBeslutter().isNotEmpty()
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

    fun harVærtSendtTilbakeFraBeslutterTidligere(): Boolean {
        return alle().any { avklaringsbehov -> avklaringsbehov.harVærtSendtTilbakeFraBeslutterTidligere() }
    }
}