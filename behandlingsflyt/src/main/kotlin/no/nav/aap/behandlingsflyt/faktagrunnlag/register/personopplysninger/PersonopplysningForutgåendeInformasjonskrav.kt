package no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger

import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav.Endret.ENDRET
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav.Endret.IKKE_ENDRET
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravInput
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravNavn
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravOppdatert
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskravkonstruktør
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravRegisterdata
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Innvilgelsesårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.ikkeKjørtSisteKalenderdag
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

class PersonopplysningForutgåendeInformasjonskrav private constructor(
    private val sakService: SakService,
    private val personopplysningForutgåendeRepository: PersonopplysningForutgåendeRepository,
    private val personopplysningGateway: PersonopplysningGateway,
    private val vilkårsresultatRepository: VilkårsresultatRepository,
    private val tidligereVurderinger: TidligereVurderinger,
) : Informasjonskrav<PersonopplysningForutgåendeInformasjonskrav.Input, PersonopplysningForutgåendeInformasjonskrav.Registerdata> {
    override val navn = Companion.navn

    override fun erRelevant(
        kontekst: FlytKontekstMedPerioder,
        steg: StegType,
        oppdatert: InformasjonskravOppdatert?
    ): Boolean {
        return kontekst.erFørstegangsbehandlingEllerRevurdering() &&
                oppdatert.ikkeKjørtSisteKalenderdag() &&
                !tidligereVurderinger.girAvslagEllerIngenBehandlingsgrunnlag(kontekst, steg)
    }

    data class Input(val sak: Sak) : InformasjonskravInput

    data class Registerdata(val personopplysninger: PersonopplysningMedHistorikk) : InformasjonskravRegisterdata

    override fun klargjør(kontekst: FlytKontekstMedPerioder): Input {
        return Input(sakService.hent(kontekst.sakId))
    }

    override fun hentData(input: Input): Registerdata {
        val personopplysninger =
            personopplysningGateway.innhentMedHistorikk(input.sak.person)

        return Registerdata(personopplysninger)
    }

    override fun oppdater(
        input: Input,
        registerdata: Registerdata,
        kontekst: FlytKontekstMedPerioder
    ): Informasjonskrav.Endret {
        val vilkårsresultat = vilkårsresultatRepository.hent(kontekst.behandlingId)
        // Vilkåret § 11-2 om forutgående medlemskap gjelder ikke ved yrkesskade
        if (vilkårsresultat.finnVilkår(Vilkårtype.SYKDOMSVILKÅRET).vilkårsperioder()
                .any { it.innvilgelsesårsak == Innvilgelsesårsak.YRKESSKADE_ÅRSAKSSAMMENHENG }
        ) {
            return IKKE_ENDRET
        }

        val eksisterendeData = personopplysningForutgåendeRepository.hentHvisEksisterer(kontekst.behandlingId)
        val personopplysninger = registerdata.personopplysninger

        if (personopplysninger != eksisterendeData?.brukerPersonopplysning) {
            personopplysningForutgåendeRepository.lagre(kontekst.behandlingId, personopplysninger)
            return ENDRET
        }
        return IKKE_ENDRET
    }

    companion object : Informasjonskravkonstruktør {
        override val navn = InformasjonskravNavn.PERSONOPPLYSNING_FORUTGÅENDE

        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): PersonopplysningForutgåendeInformasjonskrav {
            val sakRepository = repositoryProvider.provide<SakRepository>()
            val personopplysningRepository = repositoryProvider.provide<PersonopplysningForutgåendeRepository>()
            val vilkårsresultatRepository = repositoryProvider.provide<VilkårsresultatRepository>()
            return PersonopplysningForutgåendeInformasjonskrav(
                SakService(sakRepository),
                personopplysningRepository,
                gatewayProvider.provide(),
                vilkårsresultatRepository,
                TidligereVurderingerImpl(repositoryProvider),
            )
        }
    }
}
