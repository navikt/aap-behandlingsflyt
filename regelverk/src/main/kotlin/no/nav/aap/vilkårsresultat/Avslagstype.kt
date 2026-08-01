package no.nav.aap.vilkårsresultat

/** [Avslagstype] sier hvordan en [Avslagsårsak] skal tolkes når
 * avslagsårsaken er den utslagsgivende årsaken til at medlemmet *mister* retten
 * til AAP. Merk at avslagstypen ikke sier noe om hvordan avslagsårsaken
 * skal forstås hvis den er medvirkende til at medlemmet ikke får innvilget AAP.
 */
enum class Avslagstype {
    STANS,
    OPPHØR,
    KUN_INNGANGSVILKÅR,
    UKJENT,
}