package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov

import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import org.slf4j.LoggerFactory

internal object ValiderBehandlingTilstand {

    private val log = LoggerFactory.getLogger(javaClass)

    fun validerTilstandBehandling(
        behandling: Behandling,
        avklaringsbehov: Definisjon?,
        eksisterendeAvklaringsbehov: List<Avklaringsbehov>
    ) {
        validerStatus(behandling.status())

        if (avklaringsbehov != null) {
            validerAvklaringsbehov(avklaringsbehov, eksisterendeAvklaringsbehov)

            validerRekkefølge(behandling, avklaringsbehov)

            validerAvklaringsbehovOgLås(behandling, avklaringsbehov)
        }
    }

    private fun validerAvklaringsbehov(
        avklaringsbehov: Definisjon,
        eksisterendeAvklaringsbehov: List<Avklaringsbehov>
    ) {
        val avklaringsbehovFinnesIBehandlingen = eksisterendeAvklaringsbehov.any { it.definisjon == avklaringsbehov }

        if (!avklaringsbehovFinnesIBehandlingen && !avklaringsbehov.erFrivillig() && !avklaringsbehov.erOverstyring()) {
            log.warn("Forsøker å løse avklaringsbehov $avklaringsbehov ikke knyttet til behandlingen, har $eksisterendeAvklaringsbehov")

            throw UgyldigForespørselException(
                "Forsøker å løse avklaringsbehov '${avklaringsbehov.name}' som ikke hører til behandlingen."
            )
        }
    }

    private fun validerRekkefølge(behandling: Behandling, avklaringsbehov: Definisjon) {
        val gyldigRekkefølgeSteg = behandling
            .flyt()
            .erStegFørEllerLik(avklaringsbehov.løsesISteg, behandling.aktivtSteg())

        if (!gyldigRekkefølgeSteg && !avklaringsbehov.erVentebehov()) {
            log.warn("Bruker forsøkte å løse '${avklaringsbehov.name}' før nåværende steg '${behandling.aktivtSteg().name}'")

            throw UgyldigForespørselException("Aktivt steg ${behandling.aktivtSteg().name} må løses før du kan løse ${avklaringsbehov.name}")
        }
    }

    private fun validerAvklaringsbehovOgLås(behandling: Behandling, avklaringsbehov: Definisjon) {
        if (løserAvklaringsbehovForTidligereStegEtterAtBehandlingenErLåst(avklaringsbehov, behandling)) {
            val errorMsg = "Forsøker å løse avklaringsbehov $avklaringsbehov som er definert i et steg før " +
                    "nåværende steg[${behandling.aktivtSteg()}], men dette er ikke tillatt for behandlingens " +
                    "gjeldende steg ${behandling.typeBehandling().toLogString()}"

            log.warn(errorMsg)
            throw UgyldigForespørselException(errorMsg)
        }
    }

    private fun løserAvklaringsbehovForTidligereStegEtterAtBehandlingenErLåst(
        avklaringsbehov: Definisjon,
        behandling: Behandling
    ): Boolean {
        val flyt = behandling.flyt()

        val forsøkerÅLøseAvklaringsbehovFørGjeldendeSteg =
            flyt.erStegFør(avklaringsbehov.løsesISteg, behandling.aktivtSteg())
        val erGjeldendeStegLåstForOppdateringAvOpplysninger =
            !flyt.skalOppdatereFaktagrunnlagForSteg(behandling.aktivtSteg())
        val erAvklaringsbehovUnntattForSjekk = avklaringsbehov.kode in listOf(
            AvklaringsbehovKode.`9001`,
            AvklaringsbehovKode.`9002`,
            AvklaringsbehovKode.`9003`,
            AvklaringsbehovKode.`5050`,
            AvklaringsbehovKode.`5051`,
        )
        return forsøkerÅLøseAvklaringsbehovFørGjeldendeSteg && erGjeldendeStegLåstForOppdateringAvOpplysninger && !erAvklaringsbehovUnntattForSjekk
    }

    /**
     * Valider om behandlingen er i en tilstand hvor det er OK å skrive til den
     */
    fun validerTilstandBehandling(behandling: Behandling, versjon: Long) {
        validerStatus(behandling.status())
        if (behandling.versjon != versjon) {
            log.warn("Behandlingen har blitt oppdatert. Versjonsnummer[$versjon] ulikt fra siste[${behandling.versjon}]. Behandlingreferanse: ${behandling.referanse}")
            throw UgyldigForespørselException("Behandlingen har blitt oppdatert. Versjonsnummer[$versjon] ulikt fra siste[${behandling.versjon}]")
        }
    }

    private fun validerStatus(behandlingStatus: Status) {
        if (Status.AVSLUTTET == behandlingStatus) {
            log.warn("Forsøker manipulere på behandling som er avsluttet")
            throw UgyldigForespørselException("Behandlingen er avsluttet og kan ikke lenger endres")
        }
    }
}