package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarSamordningAndreStatligeYtelserLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarSamordningUføreLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.uførevurdering.SamordningUføreRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.uførevurdering.SamordningUføreVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.uførevurdering.SamordningUføreVurderingPeriode
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.lookup.repository.RepositoryProvider

class AvklarSamordningAndreStatligeYtelserLøser(connection: DBConnection) : AvklaringsbehovsLøser<AvklarSamordningAndreStatligeYtelserLøsning> {

//    private val repositoryProvider = RepositoryProvider(connection)
//    private val samordningUføreRepository = repositoryProvider.provide<SamordningUføreRepository>()

    override fun løs(
        kontekst: AvklaringsbehovKontekst,
        løsning: AvklarSamordningAndreStatligeYtelserLøsning
    ): LøsningsResultat {
//        samordningUføreRepository.lagre(
//            kontekst.behandlingId(),
//            SamordningUføreVurdering(
//                begrunnelse = løsning.samordningUføreVurdering.begrunnelse,
//                vurderingPerioder = løsning.samordningUføreVurdering.vurderingPerioder.map {
//                    SamordningUføreVurderingPeriode(
//                        virkningstidspunkt = it.virkningstidspunkt,
//                        uføregradTilSamordning = it.uføregradTilSamordning.let(::Prosent)
//                    )
//                }
//            )
//        )
        return LøsningsResultat("Vurdert samordning andre statlige ytelser")
    }

    override fun forBehov(): Definisjon {
        return Definisjon.SAMORDNING_ANDRE_STATLIGE_YTELSER
    }
}