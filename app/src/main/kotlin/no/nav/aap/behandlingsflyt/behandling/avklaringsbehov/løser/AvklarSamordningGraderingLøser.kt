package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarSamordningGraderingLøsning
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.komponenter.dbconnect.DBConnection


class AvklarSamordningGraderingLøser(connection: DBConnection): AvklaringsbehovsLøser<AvklarSamordningGraderingLøsning> {
    private val behandlingRepository = BehandlingRepositoryImpl(connection)
    // private val samordningRepository = SamordningRepositoryImpl(connection)
    
    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: AvklarSamordningGraderingLøsning): LøsningsResultat {
        val behandling = behandlingRepository.hent(kontekst.kontekst.behandlingId)
        
        // TODO: Opprett repository og avklar datamodell
        
        // samordningRepository.lagre(
        //     behandlingId = behandling.id,
        //     samordningGraderingVurdering = løsning.samordningGraderingVurdering.toSamordningGraderingVurdering()
        // )
        return LøsningsResultat(
            begrunnelse = løsning.samordningGraderingVurdering.begrunnelse
        )
    }

    override fun forBehov(): Definisjon {
        return Definisjon.AVKLAR_SAMORDNING_GRADERING
    }
}