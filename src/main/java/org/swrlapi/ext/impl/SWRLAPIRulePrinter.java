package org.swrlapi.ext.impl;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataPropertyExpression;
import org.semanticweb.owlapi.model.OWLDataRange;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.SWRLAtom;
import org.semanticweb.owlapi.model.SWRLBuiltInAtom;
import org.semanticweb.owlapi.model.SWRLClassAtom;
import org.semanticweb.owlapi.model.SWRLDArgument;
import org.semanticweb.owlapi.model.SWRLDataPropertyAtom;
import org.semanticweb.owlapi.model.SWRLDataRangeAtom;
import org.semanticweb.owlapi.model.SWRLDifferentIndividualsAtom;
import org.semanticweb.owlapi.model.SWRLIArgument;
import org.semanticweb.owlapi.model.SWRLIndividualArgument;
import org.semanticweb.owlapi.model.SWRLLiteralArgument;
import org.semanticweb.owlapi.model.SWRLObjectPropertyAtom;
import org.semanticweb.owlapi.model.SWRLRule;
import org.semanticweb.owlapi.model.SWRLSameIndividualAtom;
import org.semanticweb.owlapi.model.SWRLVariable;
import org.semanticweb.owlapi.util.DefaultPrefixManager;
import org.swrlapi.core.arguments.SQWRLCollectionVariableBuiltInArgument;
import org.swrlapi.core.arguments.SWRLAnnotationPropertyBuiltInArgument;
import org.swrlapi.core.arguments.SWRLBuiltInArgument;
import org.swrlapi.core.arguments.SWRLBuiltInArgumentVisitorEx;
import org.swrlapi.core.arguments.SWRLClassBuiltInArgument;
import org.swrlapi.core.arguments.SWRLDataPropertyBuiltInArgument;
import org.swrlapi.core.arguments.SWRLDatatypeBuiltInArgument;
import org.swrlapi.core.arguments.SWRLLiteralBuiltInArgument;
import org.swrlapi.core.arguments.SWRLMultiValueVariableBuiltInArgument;
import org.swrlapi.core.arguments.SWRLNamedIndividualBuiltInArgument;
import org.swrlapi.core.arguments.SWRLObjectPropertyBuiltInArgument;
import org.swrlapi.core.arguments.SWRLVariableBuiltInArgument;
import org.swrlapi.ext.SWRLAPIBuiltInAtom;
import org.swrlapi.ext.SWRLAPIEntityVisitorEx;

public class SWRLAPIRulePrinter implements SWRLAPIEntityVisitorEx<String>
{
	private final DefaultPrefixManager prefixManager;

	public SWRLAPIRulePrinter(DefaultPrefixManager prefixManager)
	{
		this.prefixManager = prefixManager;
	}

	@Override
	public String visit(SWRLRule node)
	{
		StringBuilder sb = new StringBuilder();
		boolean isFirst = true;

		for (SWRLAtom atom : node.getBody()) {
			if (!isFirst)
				sb.append(" ^ ");
			sb.append(atom.accept(this));
			isFirst = false;
		}

		sb.append(" -> ");

		isFirst = true;
		for (SWRLAtom atom : node.getHead()) {
			if (!isFirst)
				sb.append(" ^ ");
			sb.append(atom.accept(this));
			isFirst = false;
		}
		return sb.toString();
	}

	@Override
	public String visit(SWRLClassAtom classAtom)
	{
		OWLClassExpression classExpression = classAtom.getPredicate();
		SWRLIArgument argument = classAtom.getArgument();
		StringBuilder sb = new StringBuilder();

		sb.append(visit(classExpression));

		sb.append("(" + visit(argument) + ")");

		return sb.toString();
	}

	@Override
	public String visit(SWRLDataRangeAtom dataRangeAtom)
	{
		OWLDataRange dataRange = dataRangeAtom.getPredicate();
		SWRLDArgument argument = dataRangeAtom.getArgument();
		StringBuilder sb = new StringBuilder();

		sb.append(visit(dataRange));

		sb.append("(" + visit(argument) + ")");

		return sb.toString();
	}

	@Override
	public String visit(SWRLObjectPropertyAtom objectPropertyAtom)
	{
		OWLObjectPropertyExpression objectPropertyExpression = objectPropertyAtom.getPredicate();
		SWRLIArgument argument1 = objectPropertyAtom.getFirstArgument();
		SWRLIArgument argument2 = objectPropertyAtom.getSecondArgument();
		StringBuilder sb = new StringBuilder();

		sb.append(visit(objectPropertyExpression));

		sb.append("(" + visit(argument1) + ", " + visit(argument2) + ")");

		return sb.toString();
	}

	@Override
	public String visit(SWRLDataPropertyAtom dataPropertyAtom)
	{
		OWLDataPropertyExpression dataPropertyExpression = dataPropertyAtom.getPredicate();
		SWRLIArgument argument1 = dataPropertyAtom.getFirstArgument();
		SWRLDArgument argument2 = dataPropertyAtom.getSecondArgument();
		StringBuilder sb = new StringBuilder();

		sb.append(visit(dataPropertyExpression));

		sb.append("(" + visit(argument1) + ", " + visit(argument2) + ")");

		return sb.toString();
	}

	@Override
	public String visit(SWRLBuiltInAtom builtInAtom)
	{
		IRI iri = builtInAtom.getPredicate();
		String builtInShortName = prefixManager.getShortForm(iri);
		StringBuilder sb = new StringBuilder();

		sb.append(builtInShortName + "(");

		boolean isFirst = true;
		for (SWRLDArgument argument : builtInAtom.getArguments()) {
			if (!isFirst)
				sb.append(", ");
			sb.append(argument.accept(this));
			isFirst = false;
		}
		sb.append(")");

		return sb.toString();
	}

	@Override
	public String visit(SWRLAPIBuiltInAtom swrlapiBuiltInAtom)
	{
		String builtInShortName = swrlapiBuiltInAtom.getBuiltInShortName();
		StringBuilder sb = new StringBuilder(builtInShortName + "(");
		boolean isFirst = true;

		for (SWRLBuiltInArgument argument : swrlapiBuiltInAtom.getBuiltInArguments()) {
			if (!isFirst)
				sb.append(", ");
			// TODO Look at. accept() in SWRLBuiltInArgument and SWRLObject could apply
			sb.append(argument.accept((SWRLBuiltInArgumentVisitorEx<String>)this));
			isFirst = false;
		}
		sb.append(")");

		return sb.toString();
	}

	@Override
	public String visit(SWRLVariable variable)
	{
		String variableShortName = prefixManager.getShortForm(variable.getIRI());

		return variableShortName2VariableName(variableShortName);
	}

	@Override
	public String visit(SWRLIndividualArgument individualArgument)
	{
		return visit(individualArgument.getIndividual());
	}

	@Override
	public String visit(SWRLLiteralArgument literalArgument)
	{
		OWLLiteral literal = literalArgument.getLiteral();

		return visit(literal);
	}

	@Override
	public String visit(SWRLSameIndividualAtom sameIndividualAtom)
	{
		SWRLIArgument argument1 = sameIndividualAtom.getFirstArgument();
		SWRLIArgument argument2 = sameIndividualAtom.getSecondArgument();
		StringBuilder sb = new StringBuilder();

		sb.append("sameAs");

		sb.append("(" + visit(argument1) + ", " + visit(argument2) + ")");

		return sb.toString();
	}

	@Override
	public String visit(SWRLDifferentIndividualsAtom differentIndividualsAtom)
	{
		SWRLIArgument argument1 = differentIndividualsAtom.getFirstArgument();
		SWRLIArgument argument2 = differentIndividualsAtom.getSecondArgument();
		StringBuilder sb = new StringBuilder();

		sb.append("differentFrom");

		sb.append("(" + visit(argument1) + ", " + visit(argument2) + ")");

		return sb.toString();
	}

	private String visit(SWRLIArgument argument)
	{
		StringBuilder sb = new StringBuilder();

		if (argument instanceof SWRLIndividualArgument) {
			SWRLIndividualArgument individualArgument = (SWRLIndividualArgument)argument;
			sb.append(individualArgument.accept(this));
		} else if (argument instanceof SWRLVariable) {
			SWRLVariable variableArgument = (SWRLVariable)argument;
			sb.append(variableArgument.accept(this));
		} else
			sb.append("[Unknown " + SWRLIArgument.class.getName() + " type " + argument.getClass().getName() + "]");

		return sb.toString();
	}

	private String visit(SWRLDArgument argument)
	{
		StringBuilder sb = new StringBuilder();

		if (argument instanceof SWRLBuiltInArgument) {
			SWRLBuiltInArgument builtInArgument = (SWRLBuiltInArgument)argument;
			// TODO Look at. accept() in SWRLBuiltInArgument and SWRLObject could apply
			sb.append(builtInArgument.accept((SWRLBuiltInArgumentVisitorEx<String>)this));
		} else if (argument instanceof SWRLLiteralArgument) {
			SWRLLiteralArgument literalArgument = (SWRLLiteralArgument)argument;
			sb.append(literalArgument.accept(this));
		} else if (argument instanceof SWRLVariable) {
			SWRLVariable variableArgument = (SWRLVariable)argument;
			sb.append(variableArgument.accept(this));
		} else
			sb.append("[Unknown " + SWRLDArgument.class.getName() + " type " + argument.getClass().getName() + "]");

		return sb.toString();
	}

	private String visit(OWLClassExpression classExpression)
	{
		if (classExpression.isAnonymous())
			return classExpression.toString(); // Use the OWLAPI's rendering
		else {
			OWLClass cls = classExpression.asOWLClass();
			return visit(cls);
		}
	}

	private String visit(OWLClass cls)
	{
		return prefixManager.getShortForm(cls.getIRI());
	}

	private String visit(OWLIndividual individual)
	{
		if (individual.isNamed())
			return prefixManager.getShortForm(individual.asOWLNamedIndividual().getIRI());
		else
			return individual.toString(); // Use the OWLAPI's rendering
	}

	private String visit(OWLObjectPropertyExpression objectPropertyExpression)
	{
		if (objectPropertyExpression.isAnonymous())
			return objectPropertyExpression.toString(); // Use the OWLAPI's rendering
		else {
			OWLObjectProperty property = objectPropertyExpression.asOWLObjectProperty();
			return visit(property);
		}
	}

	private String visit(OWLObjectProperty property)
	{
		return prefixManager.getShortForm(property.getIRI());
	}

	private String visit(OWLDataPropertyExpression dataPropertyExpression)
	{
		if (dataPropertyExpression.isAnonymous())
			return dataPropertyExpression.toString(); // Use the OWLAPI's rendering
		else {
			OWLDataProperty property = dataPropertyExpression.asOWLDataProperty();
			return visit(property);
		}
	}

	private String visit(OWLDataProperty property)
	{
		return prefixManager.getShortForm(property.getIRI());
	}

	private String visit(OWLDataRange dataRange)
	{
		if (dataRange.isDatatype()) {
			OWLDatatype datatype = dataRange.asOWLDatatype();
			return prefixManager.getShortForm(datatype.getIRI());
		} else
			return dataRange.toString(); // Use the OWLAPI's rendering
	}

	@Override
	public String visit(SWRLClassBuiltInArgument argument)
	{
		OWLClass cls = argument.getOWLClass();
		return prefixManager.getShortForm(cls.getIRI());
	}

	@Override
	public String visit(SWRLNamedIndividualBuiltInArgument argument)
	{
		OWLNamedIndividual individual = argument.getOWLNamedIndividual();
		return prefixManager.getShortForm(individual.getIRI());
	}

	@Override
	public String visit(SWRLObjectPropertyBuiltInArgument argument)
	{
		OWLObjectProperty property = argument.getOWLObjectProperty();
		return prefixManager.getShortForm(property.getIRI());
	}

	@Override
	public String visit(SWRLDataPropertyBuiltInArgument argument)
	{
		OWLDataProperty property = argument.getOWLDataProperty();
		return prefixManager.getShortForm(property.getIRI());
	}

	@Override
	public String visit(SWRLAnnotationPropertyBuiltInArgument argument)
	{
		OWLAnnotationProperty property = argument.getOWLAnnotationProperty();
		return prefixManager.getShortForm(property.getIRI());
	}

	@Override
	public String visit(SWRLDatatypeBuiltInArgument argument)
	{
		OWLDatatype datatype = argument.getOWLDatatype();
		return prefixManager.getShortForm(datatype.getIRI());
	}

	@Override
	public String visit(SWRLLiteralBuiltInArgument argument)
	{
		OWLLiteral literal = argument.getLiteral();
		return visit(literal);
	}

	@Override
	public String visit(SWRLVariableBuiltInArgument argument)
	{
		String variableShortName = argument.getVariableShortName();

		return variableShortName2VariableName(variableShortName);
	}

	@Override
	public String visit(SWRLMultiValueVariableBuiltInArgument argument)
	{
		String variableShortName = argument.getVariableShortName();

		return variableShortName2VariableName(variableShortName);
	}

	@Override
	public String visit(SQWRLCollectionVariableBuiltInArgument argument)
	{
		String variableShortName = argument.getVariableShortName();

		return variableShortName2VariableName(variableShortName);
	}

	private String visit(OWLLiteral literal)
	{
		OWLDatatype datatype = literal.getDatatype();
		String value = literal.getLiteral();

		return "\"" + value + "\"^^" + visit(datatype);
	}

	private String variableShortName2VariableName(String variableShortName)
	{
		if (variableShortName.startsWith(":"))
			return "?" + variableShortName.substring(1);
		else
			return "?" + variableShortName;
	}
}
