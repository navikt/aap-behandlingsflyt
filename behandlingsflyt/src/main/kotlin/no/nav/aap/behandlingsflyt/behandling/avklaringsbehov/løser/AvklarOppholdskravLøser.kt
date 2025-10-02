package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarOppholdskravLøsning
import no.nav.aap.behandlingsflyt.behandling.oppholdskrav.OppholdskravGrunnlagRepository
import no.nav.aap.behandlingsflyt.behandling.oppholdskrav.OppholdskravVurdering
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.lookup.repository.RepositoryProvider

class AvklarOppholdskravLøser(
    private val behandlingRepository: BehandlingRepository,
    private val oppholdskravGrunnlagRepository: OppholdskravGrunnlagRepository
) : AvklaringsbehovsLøser<AvklarOppholdskravLøsning> {
    constructor(repositoryProvider: RepositoryProvider) : this(
        behandlingRepository = repositoryProvider.provide(),
        oppholdskravGrunnlagRepository = repositoryProvider.provide()

    )

    override fun løs(
        kontekst: AvklaringsbehovKontekst,
        løsning: AvklarOppholdskravLøsning
    ): LøsningsResultat {
        val behandling = behandlingRepository.hent(kontekst.kontekst.behandlingId)

        oppholdskravGrunnlagRepository.lagre(
            behandlingId = behandling.id,
            oppholdskravVurdering = OppholdskravVurdering(
                vurdertAv = kontekst.bruker.ident,
                perioder = løsning.vurdering.perioder.map { it.tilOppholdskravPeriode() },
                vurdertIBehandling = behandling.id
            )
        )
        return LøsningsResultat("Vurdert oppholdskrav")
    }

    override fun forBehov(): Definisjon {
        return Definisjon.AVKLAR_OPPHOLDSKRAV
    }

}