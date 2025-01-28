package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.lovvalg.MedlemskapLovvalgGrunnlag
import no.nav.aap.behandlingsflyt.behandling.vilkår.medlemskap.Medlemskapvilkåret
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.MedlemskapArbeidInntektRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonopplysningRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
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

            for (periode in kontekst.perioder()) {
                Medlemskapvilkåret(vilkårsresultat).vurder(
                    MedlemskapLovvalgGrunnlag(medlemskapArbeidInntektGrunnlag, personopplysningGrunnlag, oppgittUtenlandsOppholdGrunnlag)
                )
            }
            vilkårsresultatRepository.lagre(kontekst.behandlingId, vilkårsresultat)
        }
        /*
        Medlemskapvilkåret(vilkårsresultat = "").vurder(grunnnlag)
        if (fantAtViIkkeKanStoppe) {
            return FantAvklaringsbehov(Definisjon.AVKLAR_LOVVALG_MEDLEMSKAP)
        }
        */
        // val vilkårsresultat = vilkårsresultatRepository.hent(kontekst.behandlingId).finnVilkår(Vilkårtype.MEDLEMSKAP). //Denne kan gi noe ikke_oppfylt/oppfylt
        // Finne ut hva vi ønsker å returnere her

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
