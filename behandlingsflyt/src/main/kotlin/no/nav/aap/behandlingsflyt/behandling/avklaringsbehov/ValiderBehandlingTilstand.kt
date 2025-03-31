package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov

import no.nav.aap.behandlingsflyt.flyt.BehandlingFlyt
import no.nav.aap.behandlingsflyt.flyt.utledType
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
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
                throw IllegalArgumentException("Forsøker å løse avklaringsbehov $avklaringsbehov ikke knyttet til behandlingen, har $eksisterendeAvklaringsbehov")
            }
            val flyt = utledType(behandling.typeBehandling()).flyt()
            if (!flyt.erStegFørEllerLik(avklaringsbehov.løsesISteg, behandling.aktivtSteg())) {
                log.warn(
                    "Forsøker å løse avklaringsbehov $avklaringsbehov som er definert i et steg etter nåværende steg[${behandling.aktivtSteg()}] ${
                        behandling.typeBehandling().identifikator()
                    }"
                )
                throw IllegalArgumentException(
                    "Forsøker å løse avklaringsbehov $avklaringsbehov som er definert i et steg etter nåværende steg[${behandling.aktivtSteg()}] ${
                        behandling.typeBehandling().identifikator()
                    }"
                )
            }
            if (løserAvklaringsbehovForTidligereStegEtterAtBehandlingenErLåst(flyt, avklaringsbehov, behandling)) {
                log.warn(
                    "Forsøker å løse avklaringsbehov $avklaringsbehov som er definert i et steg før nåværende steg[${behandling.aktivtSteg()}], men dette er ikke tillatt for behandlingens gjeldende steg ${
                        behandling.typeBehandling().identifikator()
                    }"
                )
                throw IllegalArgumentException(
                    "Forsøker å løse avklaringsbehov $avklaringsbehov som er definert i et steg før nåværende steg[${behandling.aktivtSteg()}], men dette er ikke tillatt for behandlingens gjeldende steg ${
                        behandling.typeBehandling().identifikator()
                    }"
                )
            }
        }
    }

    private fun løserAvklaringsbehovForTidligereStegEtterAtBehandlingenErLåst(
        flyt: BehandlingFlyt,
        avklaringsbehov: Definisjon,
        behandling: Behandling
    ): Boolean {
        val forsøkerÅLøseAvklaringsbehovFørGjeldendeSteg = flyt.erStegFør(avklaringsbehov.løsesISteg, behandling.aktivtSteg())
        val erGjeldendeStegLåstForOppdateringAvOpplysninger = !flyt.skalOppdatereFaktagrunnlagForSteg(behandling.aktivtSteg())
        val erAvklaringsbehovUnntattForSjekk =  avklaringsbehov.kode in listOf(AvklaringsbehovKode.`9002`, AvklaringsbehovKode.`5050`)
        return forsøkerÅLøseAvklaringsbehovFørGjeldendeSteg && erGjeldendeStegLåstForOppdateringAvOpplysninger && !erAvklaringsbehovUnntattForSjekk
    }

    /**
     * Valider om behandlingen er i en tilstand hvor det er OK å skrive til den
     */
    fun validerTilstandBehandling(behandling: Behandling, versjon: Long) {
        validerStatus(behandling.status())
        if (behandling.versjon != versjon) {
            log.warn("Behandlingen har blitt oppdatert. Versjonsnummer[$versjon] ulikt fra siste[${behandling.versjon}]")
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