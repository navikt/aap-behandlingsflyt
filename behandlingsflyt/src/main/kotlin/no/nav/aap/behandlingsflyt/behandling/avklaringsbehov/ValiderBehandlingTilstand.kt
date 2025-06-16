package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov

import no.nav.aap.behandlingsflyt.flyt.BehandlingFlyt
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
            if (!eksisterendeAvklaringsbehov.map { a -> a.definisjon }
                    .contains(avklaringsbehov) && !avklaringsbehov.erFrivillig() && !avklaringsbehov.erOverstyring()) {
                log.warn("Forsøker å løse avklaringsbehov $avklaringsbehov ikke knyttet til behandlingen, har $eksisterendeAvklaringsbehov")
                throw UgyldigForespørselException("Forsøker å løse avklaringsbehov $avklaringsbehov ikke knyttet til behandlingen, har $eksisterendeAvklaringsbehov")
            }
            val flyt = behandling.flyt()
            if (!flyt.erStegFørEllerLik(avklaringsbehov.løsesISteg, behandling.aktivtSteg()) && !avklaringsbehov.erVentebehov()) {
                val errorMsg = "Forsøker å løse avklaringsbehov $avklaringsbehov som er definert i et steg etter " +
                        "nåværende steg[${behandling.aktivtSteg()}] ${
                            behandling.typeBehandling().toLogString()
                        }. Skal løses i steg[${avklaringsbehov.løsesISteg}]"

                log.warn(errorMsg)
                throw UgyldigForespørselException(errorMsg)
            }
            if (løserAvklaringsbehovForTidligereStegEtterAtBehandlingenErLåst(flyt, avklaringsbehov, behandling)) {
                val errorMsg = "Forsøker å løse avklaringsbehov $avklaringsbehov som er definert i et steg før " +
                        "nåværende steg[${behandling.aktivtSteg()}], men dette er ikke tillatt for behandlingens " +
                        "gjeldende steg ${behandling.typeBehandling().toLogString()}"

                log.warn(errorMsg)
                throw UgyldigForespørselException(errorMsg)
            }
        }
    }

    private fun løserAvklaringsbehovForTidligereStegEtterAtBehandlingenErLåst(
        flyt: BehandlingFlyt,
        avklaringsbehov: Definisjon,
        behandling: Behandling
    ): Boolean {
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
            throw OutdatedBehandlingException("Behandlingen har blitt oppdatert. Versjonsnummer[$versjon] ulikt fra siste[${behandling.versjon}]")
        }
    }

    private fun validerStatus(behandlingStatus: Status) {
        if (Status.AVSLUTTET == behandlingStatus) {
            log.warn("Forsøker manipulere på behandling som er avsluttet")
            throw IllegalArgumentException("Forsøker manipulere på behandling som er avsluttet")
        }
    }
}