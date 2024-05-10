package no.nav.aap.behandlingsflyt.avklaringsbehov

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.verdityper.flyt.StegType
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import org.slf4j.LoggerFactory
import java.time.LocalDate

private val log = LoggerFactory.getLogger(Avklaringsbehovene::class.java)

class Avklaringsbehovene(
    private val repository: AvklaringsbehovOperasjonerRepository,
    private val behandlingId: BehandlingId
) : AvklaringsbehoveneDecorator {
    private var avklaringsbehovene: MutableList<Avklaringsbehov> = repository.hent(behandlingId).toMutableList()

    fun ingenEndring(avklaringsbehov: Avklaringsbehov, bruker: String) {
        løsAvklaringsbehov(
            avklaringsbehov.definisjon,
            "Ingen endring fra forrige vurdering",
            bruker
        )
    }

    fun løsAvklaringsbehov(
        definisjon: Definisjon,
        begrunnelse: String,
        endretAv: String,
        kreverToTrinn: Boolean? = null
    ) {
        val avklaringsbehov = alle().single { it.definisjon == definisjon }
        if (kreverToTrinn == null) {
            avklaringsbehov.løs(begrunnelse, endretAv = endretAv)
        } else {
            avklaringsbehov.løs(begrunnelse = begrunnelse, endretAv = endretAv, kreverToTrinn = kreverToTrinn)
            repository.kreverToTrinn(avklaringsbehov.id, kreverToTrinn)
        }
        repository.endre(avklaringsbehov.id, avklaringsbehov.historikk.last())
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
     * NB! Dersom avklaringsbehovet finnes fra før og er åpent så ignorerer vi det nye behovet, mens dersom det er avsluttet eller avbrutt så reåpner vi det.
     */
    fun leggTil(definisjoner: List<Definisjon>, stegType: StegType, fristUtgangspunkt: LocalDate? = null) {
        definisjoner.forEach { definisjon ->
            val avklaringsbehov = hentBehovForDefinisjon(definisjon)
            if (avklaringsbehov != null) {
                if (avklaringsbehov.erAvsluttet() || avklaringsbehov.status() == Status.AVBRUTT) {
                    avklaringsbehov.reåpne()
                    repository.endre(avklaringsbehov.id, avklaringsbehov.historikk.last())
                } else {
                    log.warn("Forsøkte å legge til et avklaringsbehov som allerede eksisterte")
                }
            } else {
                repository.opprett(
                    behandlingId = behandlingId,
                    definisjon = definisjon,
                    funnetISteg = stegType,
                    frist = utledFrist(definisjon, fristUtgangspunkt)
                )
            }
        }
    }

    private fun utledFrist(definisjon: Definisjon, fristUtgangspunkt: LocalDate?): LocalDate? {
        if (definisjon.erVentepunkt()) {
            return definisjon.utledFrist(fristUtgangspunkt)
        }
        return null
    }

    fun vurderTotrinn(
        definisjon: Definisjon,
        godkjent: Boolean,
        begrunnelse: String,
        vurdertAv: String,
        årsakTilRetur: List<ÅrsakTilRetur> = emptyList(),
    ) {
        val avklaringsbehov = alle().single { it.definisjon == definisjon }
        avklaringsbehov.vurderTotrinn(begrunnelse, godkjent, vurdertAv, årsakTilRetur)
        repository.endre(avklaringsbehov.id, avklaringsbehov.historikk.last())
    }

    fun avbryt(definisjon: Definisjon) {
        val avklaringsbehov = alle().single { it.definisjon == definisjon }
        avklaringsbehov.avbryt()
        repository.endre(avklaringsbehov.id, avklaringsbehov.historikk.last())
    }

    fun reåpne(definisjon: Definisjon) {
        val avklaringsbehov = alle().single { it.definisjon == definisjon }
        avklaringsbehov.reåpne()
        repository.endre(avklaringsbehov.id, avklaringsbehov.historikk.last())
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

    fun validateTilstand(behandling: Behandling, avklaringsbehov: Definisjon? = null, versjon: Long? = null) {
        ValiderBehandlingTilstand.validerTilstandBehandling(
            behandling = behandling,
            avklaringsbehov = avklaringsbehov,
            eksisterenedeAvklaringsbehov = avklaringsbehovene,
            versjon = versjon
        )
    }

    fun erSattPåVent(): Boolean {
        return alle().any { avklaringsbehov -> avklaringsbehov.erVentepunkt() && avklaringsbehov.erÅpent() }
    }

    fun hentVentepunkterMedUtløptFrist(): List<Avklaringsbehov> {
        return alle().filter { it.erVentepunkt() && it.erÅpent() && it.fristUtløpt() }
    }
}