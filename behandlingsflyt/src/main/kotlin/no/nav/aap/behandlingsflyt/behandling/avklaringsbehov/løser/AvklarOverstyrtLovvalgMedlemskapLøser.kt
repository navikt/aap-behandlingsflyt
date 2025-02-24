package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarOverstyrtLovvalgMedlemskapLøsning
import no.nav.aap.behandlingsflyt.behandling.lovvalg.MedlemskapLovvalgGrunnlag
import no.nav.aap.behandlingsflyt.behandling.vilkår.medlemskap.Medlemskapvilkåret
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.MedlemskapArbeidInntektRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonopplysningRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.RepositoryProvider

class AvklarOverstyrtLovvalgMedlemskapLøser(connection: DBConnection): AvklaringsbehovsLøser<AvklarOverstyrtLovvalgMedlemskapLøsning> {
    private val repositoryProvider = RepositoryProvider(connection)
    private val medlemskapArbeidInntektRepository = repositoryProvider.provide<MedlemskapArbeidInntektRepository>()
    private val vilkårsresultatRepository = repositoryProvider.provide<VilkårsresultatRepository>()
    private val sakRepository = repositoryProvider.provide<SakRepository>()
    private val personopplysningRepository = repositoryProvider.provide<PersonopplysningRepository>()

    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: AvklarOverstyrtLovvalgMedlemskapLøsning): LøsningsResultat {
        medlemskapArbeidInntektRepository.lagreManuellVurdering(kontekst.behandlingId(), løsning.manuellVurderingForLovvalgMedlemskap, true)

        val overstyrtManuellVurdering = medlemskapArbeidInntektRepository.hentHvisEksisterer(kontekst.behandlingId())?.manuellVurdering

        val sak = sakRepository.hent(kontekst.kontekst.sakId)
        val vilkårsresultat = vilkårsresultatRepository.hent(kontekst.behandlingId())
        val personopplysningGrunnlag = personopplysningRepository.hentHvisEksisterer(kontekst.behandlingId())
            ?: throw IllegalStateException("Forventet å finne personopplysninger")
        val medlemskapArbeidInntektGrunnlag = medlemskapArbeidInntektRepository.hentHvisEksisterer(kontekst.behandlingId())
        val oppgittUtenlandsOppholdGrunnlag = medlemskapArbeidInntektRepository.hentOppgittUtenlandsOppholdHvisEksisterer(kontekst.behandlingId())

        Medlemskapvilkåret(vilkårsresultat, sak.rettighetsperiode, overstyrtManuellVurdering).vurderOverstyrt(
            MedlemskapLovvalgGrunnlag(medlemskapArbeidInntektGrunnlag, personopplysningGrunnlag, oppgittUtenlandsOppholdGrunnlag)
        )
        vilkårsresultatRepository.lagre(kontekst.behandlingId(), vilkårsresultat)

        return LøsningsResultat("OVERSTYRT: Vurdert lovvalg & medlemskap manuelt.")
    }

    override fun forBehov(): Definisjon {
        return Definisjon.MANUELL_OVERSTYRING_LOVVALG
    }
}