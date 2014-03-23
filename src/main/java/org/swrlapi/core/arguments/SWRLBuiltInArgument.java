package org.swrlapi.core.arguments;

import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.SWRLDArgument;
import org.swrlapi.ext.SWRLAPIOWLDataFactory;
import org.swrlapi.ext.SWRLAPIOWLOntology;

/**
 * Interface representing an argument to a SWRL built-in. It extends the {@link SWRLDArgument} interface to define an
 * interface representing arguments to swrl built-ins. This interfacce is the SWRLAPI's primary OWLAPI extension point.
 * The {@link SWRLBuiltInArgument} interface represents SWRL built-in atoms in the SWRLAPI, which has a wider range of
 * built-in argument types than the OWLAPI. The OWLAPI envisions built-in arguments as simple literals or variables
 * only. In addition to literals and variables, the SWRLAPI allows OWL named objects (classes, individuals, properties,
 * and datatypes) as well as SQWRL-specific arguments. *
 * <p>
 * Since an OWLAPI ontology (represented by the OWLAPI class {@link OWLOntology}) or an OWL data factory (represented by
 * the OWLAPI class {@link OWLDataFactory), will not be aware of these types a {@link SWRLAPIOWLOntology} (in
 * conjunction with an {@link SWRLAPIOWLDataFactory}) must be used to extract SWRLAPI SWRL rules
 * <p>
 * Similarly, a SWRLAPI-aware parser is required to generate SWRLAPI rules from rule text.
 * 
 * @see SWRLLiteralBuiltInArgument, SWRLVariableBuiltInArgument, SWRLClassBuiltInArgument,
 *      SWRLNamedIndividualBuiltInArgument, SWRLObjectPropertyBuiltInArgument, SWRLDataPropertyBuiltInArgument,
 *      SWRLDataPropertyBuiltInArgument, SWRLAnnotationPropertyBuiltInArgument, SWRLDatatypeBuiltInArgument,
 *      SWRLMultiValueBuiltInArgument, SQWRLCollectionBuiltInArgument
 */
public interface SWRLBuiltInArgument extends SWRLDArgument
{ // TODO These methods should really be in SWRLVariableBuiltInArgument
	void setBuiltInResult(SWRLBuiltInArgument builtInResult);

	SWRLBuiltInArgument getBuiltInResult();

	boolean hasBuiltInResult();

	String getVariableName();

	void setVariableName(String variableName);

	boolean isVariable();

	boolean isUnbound();

	boolean isBound();

	void setUnbound();

	void setBound();

	String toDisplayText();
}
