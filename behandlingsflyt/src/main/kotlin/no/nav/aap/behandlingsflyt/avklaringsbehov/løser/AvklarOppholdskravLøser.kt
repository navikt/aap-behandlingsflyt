package no.nav.aap.behandlingsflyt.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.avklaringsbehov.løsning.AvklarOppholdskravLøsning
import no.nav.aap.behandlingsflyt.steg.oppholdskrav.OppholdskravGrunnlagRepository
import no.nav.aap.oppholdskrav.OppholdskravVurdering
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

        val vurdering = OppholdskravVurdering(
            vurdertAv = kontekst.bruker,
            perioder = løsning.løsningerForPerioder.map { it.tilOppholdskravPeriode() },
            vurdertIBehandling = behandling.id
        )

        oppholdskravGrunnlagRepository.lagre(
            behandlingId = behandling.id,
            oppholdskravVurdering = vurdering
        )
        return LøsningsResultat("Vurdert oppholdskrav")
    }

    override fun forBehov(): Definisjon {
        return Definisjon.AVKLAR_OPPHOLDSKRAV
    }

}