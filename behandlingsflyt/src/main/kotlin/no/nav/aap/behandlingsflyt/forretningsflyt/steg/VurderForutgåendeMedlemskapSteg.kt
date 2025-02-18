package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.YrkesskadeRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.RepositoryProvider

class VurderForutgåendeMedlemskapSteg private constructor(
    private val vilkårsresultatRepository: VilkårsresultatRepository,
    private val yrkesskadeRepository: YrkesskadeRepository
) : BehandlingSteg {
    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val yrkesskadeGrunnlag = yrkesskadeRepository.hentHvisEksisterer(behandlingId = kontekst.behandlingId)
        if (yrkesskadeGrunnlag?.yrkesskader?.harYrkesskade() == true) {
            return Fullført
        }

        val behandlingId = kontekst.behandlingId
        val vilkårsresultat = vilkårsresultatRepository.hent(behandlingId)
        val vilkår = vilkårsresultat.finnVilkår(Vilkårtype.MEDLEMSKAP)

        // TODO: IF YRKESSKADE, SKIP
        // TODO: Revurdering må inn her

        for (periode in kontekst.perioder()) {
            vilkår.leggTilVurdering(
                Vilkårsperiode(
                    periode = periode,
                    utfall = Utfall.OPPFYLT,
                    begrunnelse = null,
                    faktagrunnlag = null
                )
            )
        }
        vilkårsresultatRepository.lagre(behandlingId, vilkårsresultat)

        return Fullført
    }

    companion object : FlytSteg {
        override fun konstruer(connection: DBConnection): BehandlingSteg {
            val repositoryProvider = RepositoryProvider(connection)
            val vilkårsresultatRepository = repositoryProvider.provide<VilkårsresultatRepository>()
            val yrkesskadeRepository = repositoryProvider.provide<YrkesskadeRepository>()
            return VurderForutgåendeMedlemskapSteg(
                vilkårsresultatRepository,
                yrkesskadeRepository
            )
        }

        override fun type(): StegType {
            return StegType.VURDER_MEDLEMSKAP
        }
    }
}
