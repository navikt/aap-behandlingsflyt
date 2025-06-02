package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.TrekkKlageLøsning
import no.nav.aap.behandlingsflyt.behandling.trekkklage.TrekkKlageRepository
import no.nav.aap.behandlingsflyt.behandling.trekkklage.TrekkKlageVurdering
import no.nav.aap.behandlingsflyt.behandling.trekkklage.TrekkKlageÅrsak
import no.nav.aap.behandlingsflyt.behandling.trekkklage.flate.TrekkKlageÅrsakDto
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.Instant

class TrekkKlageLøser(
    private val behandlingRepository: BehandlingRepository,
    private val trekkKlageRepository: TrekkKlageRepository
) : AvklaringsbehovsLøser<TrekkKlageLøsning> {

    constructor(repositoryProvider: RepositoryProvider) : this(
        behandlingRepository = repositoryProvider.provide(),
        trekkKlageRepository = repositoryProvider.provide()
    )

    override fun løs(
        kontekst: AvklaringsbehovKontekst,
        løsning: TrekkKlageLøsning
    ): LøsningsResultat {
        val behandling = behandlingRepository.hent(kontekst.behandlingId())

        require(behandling.status().erÅpen()) {
            "kan kun trekke klager som utredes"
        }

        if(løsning.vurdering.skalTrekkes && løsning.vurdering.hvorforTrekkes == null)(
            throw UgyldigForespørselException("Må ha valgt hvorfor klage trekkes om den skal trekkes")
        )

        trekkKlageRepository.lagreTrekkKlageVurdering(
            behandlingId = kontekst.behandlingId(),
            vurdering = TrekkKlageVurdering(
                begrunnelse = løsning.vurdering.begrunnelse,
                skalTrekkes = løsning.vurdering.skalTrekkes,
                hvorforTrekkes = when(løsning.vurdering.hvorforTrekkes) {
                    TrekkKlageÅrsakDto.TRUKKET_AV_BRUKER -> TrekkKlageÅrsak.TRUKKET_AV_BRUKER
                    TrekkKlageÅrsakDto.FEILREGISTRERING -> TrekkKlageÅrsak.FEILREGISTRERING
                    null -> null
                },
                vurdertAv = kontekst.bruker,
                vurdert = Instant.now(),
            )
        )

        return LøsningsResultat(løsning.vurdering.begrunnelse)
    }

    override fun forBehov(): Definisjon {
        return Definisjon.VURDER_TREKK_AV_KLAGE
    }

}
