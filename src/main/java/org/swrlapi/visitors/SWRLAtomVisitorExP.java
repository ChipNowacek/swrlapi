package org.swrlapi.visitors;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.semanticweb.owlapi.model.SWRLClassAtom;
import org.semanticweb.owlapi.model.SWRLDataPropertyAtom;
import org.semanticweb.owlapi.model.SWRLDataRangeAtom;
import org.semanticweb.owlapi.model.SWRLDifferentIndividualsAtom;
import org.semanticweb.owlapi.model.SWRLObjectPropertyAtom;
import org.semanticweb.owlapi.model.SWRLSameIndividualAtom;
import org.swrlapi.core.SWRLAPIBuiltInAtom;

/**
 * @see org.semanticweb.owlapi.model.SWRLAtom
 */
public interface SWRLAtomVisitorExP<T, P>
{
  @NonNull T visit(@NonNull SWRLClassAtom atom, @NonNull P p);

  @NonNull T visit(@NonNull SWRLObjectPropertyAtom atom, @NonNull P p);

  @NonNull T visit(@NonNull SWRLDataPropertyAtom atom, @NonNull P p);

  @NonNull T visit(@NonNull SWRLSameIndividualAtom atom, @NonNull P p);

  @NonNull T visit(@NonNull SWRLDifferentIndividualsAtom atom, @NonNull P p);

  @NonNull T visit(@NonNull SWRLDataRangeAtom atom, @NonNull P p);

  @NonNull T visit(@NonNull SWRLAPIBuiltInAtom atom, @NonNull P p);
}