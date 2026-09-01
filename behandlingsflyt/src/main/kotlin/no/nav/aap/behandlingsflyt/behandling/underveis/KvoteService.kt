package no.nav.aap.behandlingsflyt.behandling.underveis

import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.StegStatus
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.LocalDateTime

class KvoteService(
    private val behandlingRepository: BehandlingRepository,
) {
    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        behandlingRepository = repositoryProvider.provide(),
    )

    /* Kvoter som skal brukes i alle nye og åpne behandlinger. */
    fun gjeldendeKvoter(): Kvoter {
        return standardKvoter
    }

    /** Kvoter som ble brukt i en potensielt historisk behandling.
     * Hvis du skal gjøre beregninger på kvoter i en behandling som kan være historisk (avsluttet),
     * så må denne metoden brukes for å få størrelsen på kvoten som ble brukt i behandlingen.
     * */
    fun historiskBruktKvoter(behandling: Behandling): Kvoter {
        val underveisKjørte = behandlingRepository.hentStegHistorikk(behandling.id)
            .lastOrNull { it.steg() == StegType.FASTSETT_UTTAK && it.status() == StegStatus.UTFØRER }
            ?.tidspunkt()

        return if (underveisKjørte != null && underveisKjørte <= kvoteEndretFra130Til131) {
            Kvoter.create(
                ordinærkvote = 784,
                sykepengeerstatningkvote = 130,
            )
        } else {
            gjeldendeKvoter()
        }
    }

    companion object {
        /* Endret SPE-kvote fra 130 til 131 dager med deploy 28. januar 2026. **/
        private val kvoteEndretFra130Til131 = LocalDateTime.parse("2026-01-28T14:15:00")

        val standardKvoter = Kvoter.create(
            /* Så lenge Arena har 784 må vi ha samme som dem, i stede for ANTALL_ARBEIDSDAGER_I_ÅRET * 3. */
            ordinærkvote = 784,

            /* Fra regelspesifiseringen:
             *
             *  Perioden på inntil 6 måneder er en kvote som består av 131 dager.
             *  Begrunnelsen for at 6 mnd = 131 dager er at tre år etter § 11-12
             *  er (261 dager ganger 3) + 1 = 784 dager. Dersom man deler 784 på 6
             *  for å få 6 måneder tilsvarer dette 130,67 dager, som rundes opp til
             *  131.
             */
            sykepengeerstatningkvote = 131,
        )
    }
}