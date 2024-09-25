package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarSamordningGraderingLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelseVurderingRepository
import no.nav.aap.komponenter.dbconnect.DBConnection

class AvklarSamordningGraderingLøser(connection: DBConnection): AvklaringsbehovsLøser<AvklarSamordningGraderingLøsning> {
    private val samordningYtelseVurderingRepository  = SamordningYtelseVurderingRepository(connection)
    
    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: AvklarSamordningGraderingLøsning): LøsningsResultat {

        samordningYtelseVurderingRepository.lagreVurderinger(kontekst.kontekst.behandlingId, løsning.vurderingerForSamordning.vurderteSamordninger)

        return LøsningsResultat("Vurdert samordning")
    }

    override fun forBehov(): Definisjon {
        return Definisjon.AVKLAR_SAMORDNING_GRADERING
    }
}