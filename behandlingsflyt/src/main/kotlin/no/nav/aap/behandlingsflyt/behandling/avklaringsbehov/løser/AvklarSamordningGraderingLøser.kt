package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarSamordningGraderingLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningVurderingRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.RepositoryProvider

class AvklarSamordningGraderingLøser(connection: DBConnection) :
    AvklaringsbehovsLøser<AvklarSamordningGraderingLøsning> {
    private val samordningYtelseVurderingRepository =
        RepositoryProvider(connection).provide<SamordningVurderingRepository>()

    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: AvklarSamordningGraderingLøsning): LøsningsResultat {

        samordningYtelseVurderingRepository.lagreVurderinger(
            kontekst.kontekst.behandlingId, løsning.vurderingerForSamordning.vurderteSamordninger
        )

        return LøsningsResultat("Vurdert samordning")
    }

    override fun forBehov(): Definisjon {
        return Definisjon.AVKLAR_SAMORDNING_GRADERING
    }
}