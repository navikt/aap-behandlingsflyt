package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.lovvalg.MedlemskapLovvalgGrunnlag
import no.nav.aap.behandlingsflyt.behandling.vilkår.medlemskap.Medlemskapvilkåret
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.MedlemskapArbeidInntektRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonopplysningRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FantAvklaringsbehov
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.RepositoryProvider

class VurderLovvalgSteg private constructor(
    private val vilkårsresultatRepository: VilkårsresultatRepository,
    private val personopplysningRepository: PersonopplysningRepository,
    private val medlemskapArbeidInntektRepository: MedlemskapArbeidInntektRepository
) : BehandlingSteg {
    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        if (kontekst.perioderTilVurdering.isNotEmpty()) {
            val vilkårsresultat = vilkårsresultatRepository.hent(kontekst.behandlingId)
            val personopplysningGrunnlag = personopplysningRepository.hentHvisEksisterer(kontekst.behandlingId)
                ?: throw IllegalStateException("Forventet å finne personopplysninger")
            val medlemskapArbeidInntektGrunnlag = medlemskapArbeidInntektRepository.hentHvisEksisterer(kontekst.behandlingId)
            val oppgittUtenlandsOppholdGrunnlag = medlemskapArbeidInntektRepository.hentOppgittUtenlandsOppholdHvisEksisterer(kontekst.behandlingId)
                ?: throw IllegalStateException("Forventet å finne utenlandsopplysninger")

            for (periode in kontekst.perioder()) {
                Medlemskapvilkåret(vilkårsresultat, periode).vurder(
                    MedlemskapLovvalgGrunnlag(medlemskapArbeidInntektGrunnlag, personopplysningGrunnlag, oppgittUtenlandsOppholdGrunnlag)
                )
            }
            vilkårsresultatRepository.lagre(kontekst.behandlingId, vilkårsresultat)
        }

        /*
        val alleVilkårOppfylt = vilkårsresultatRepository.hent(kontekst.behandlingId).finnVilkår(Vilkårtype.MEDLEMSKAP).vilkårsperioder().all{it.erOppfylt()}
        if (!alleVilkårOppfylt) {
            return FantAvklaringsbehov(Definisjon.AVKLAR_LOVVALG_MEDLEMSKAP)
        }

        if (!x.harGjortVurdering(kontekst.behandlingId)) {
            return FantAvklaringsbehov(Definisjon.AVKLAR_LOVVALG_MEDLEMSKAP)
        }*/

        return Fullført
    }

    companion object : FlytSteg {
        override fun konstruer(connection: DBConnection): BehandlingSteg {
            val repositoryProvider = RepositoryProvider(connection)
            val vilkårsresultatRepository = repositoryProvider.provide<VilkårsresultatRepository>()
            val personopplysningRepository = repositoryProvider.provide<PersonopplysningRepository>()
            val medlemskapArbeidInntektRepository = repositoryProvider.provide<MedlemskapArbeidInntektRepository>()
            return VurderLovvalgSteg(vilkårsresultatRepository, personopplysningRepository, medlemskapArbeidInntektRepository)
        }

        override fun type(): StegType {
            return StegType.VURDER_LOVVALG
        }
    }
}
