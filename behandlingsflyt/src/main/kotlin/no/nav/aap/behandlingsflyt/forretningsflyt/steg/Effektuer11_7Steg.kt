package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.ÅrsakTilSettPåVent
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevGateway
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingRepository
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingService
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.TypeBrev
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisÅrsak
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FantAvklaringsbehov
import no.nav.aap.behandlingsflyt.flyt.steg.FantVentebehov
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.flyt.steg.Ventebehov
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon.BESTILL_BREV
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon.EFFEKTUER_11_7
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.LocalDate

class Effektuer11_7Steg private constructor(
    private val underveisRepository: UnderveisRepository,
    private val brevbestillingService: BrevbestillingService,
): BehandlingSteg {
    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        //hent brudd som har blitt forhåndsvarslet for
        val underveisGrunnlag = underveisRepository.hent(kontekst.behandlingId)

        val relevanteBrudd =
            underveisGrunnlag.perioder.filter { it.avslagsårsak == UnderveisÅrsak.BRUDD_PÅ_AKTIVITETSPLIKT }

        //TODO - filtrer ut de som er bestilt forhåndsvarsel for
        val uvarsledeRelevanteBrudd =
            underveisGrunnlag.perioder.filter { it.avslagsårsak == UnderveisÅrsak.BRUDD_PÅ_AKTIVITETSPLIKT }

        if (false && uvarsledeRelevanteBrudd.isNotEmpty()) {
            //TODO - trenger ny brevtype
            val bestillingsReferanse = brevbestillingService.bestill(kontekst.behandlingId, TypeBrev.VEDTAK_AVSLAG)
            //lagre ned bruddene i uvasledeRelevanteBrudd sammen med bestillingsReferansen
            return FantVentebehov(Ventebehov(BESTILL_BREV, ÅrsakTilSettPåVent.VENTER_PÅ_MASKINELL_AVKLARING))
        }

        //TODO - frist for å svare har ikke utløp og har ikke fått svar
        if (false) {
            return FantVentebehov(
                Ventebehov(
                    definisjon = EFFEKTUER_11_7,
                    grunn = ÅrsakTilSettPåVent.VENTER_PÅ_SVAR_FRA_BRUKER,
                    //TODO - sett frist
                    frist = LocalDate.now()
                )
            )
        }

        //TODO - veileder har ikke avklart disse bruddene
        if (false) {
            return FantAvklaringsbehov(EFFEKTUER_11_7)
        }

        return Fullført
    }

    companion object: FlytSteg {
        override fun konstruer(connection: DBConnection): BehandlingSteg {
            val repositoryProvider = RepositoryProvider(connection)
            val behandlingRepository = repositoryProvider.provide(BehandlingRepository::class)
            val sakRepository = repositoryProvider.provide(SakRepository::class)
            val brevbestillingRepository = repositoryProvider.provide(BrevbestillingRepository::class)

            val brevbestillingService =
                BrevbestillingService(
                    brevbestillingGateway = BrevGateway(),
                    brevbestillingRepository = brevbestillingRepository,
                    behandlingRepository = behandlingRepository,
                    sakRepository = sakRepository
                )

            return Effektuer11_7Steg(
                //TODO - bruk repository provider?
                UnderveisRepository(connection),
                brevbestillingService
            )
        }

        override fun type(): StegType {
            return StegType.EFFEKTUER_11_7
        }
    }
}