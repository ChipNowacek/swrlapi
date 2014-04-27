package org.swrlapi.core.arguments.impl;

import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.swrlapi.core.arguments.SWRLBuiltInArgumentVisitorEx;
import org.swrlapi.core.arguments.SWRLNamedIndividualBuiltInArgument;

class SWRLNamedIndividualBuiltInArgumentImpl extends SWRLNamedBuiltInArgumentImpl implements
		SWRLNamedIndividualBuiltInArgument
{
	private static final long serialVersionUID = 1L;

	public SWRLNamedIndividualBuiltInArgumentImpl(OWLNamedIndividual individual)
	{
		super(individual);
	}

	@Override
	public OWLNamedIndividual getOWLNamedIndividual()
	{
		return getOWLEntity().asOWLNamedIndividual();
	}

	@Override
	public <T> T accept(SWRLBuiltInArgumentVisitorEx<T> visitor)
	{
		return visitor.visit(this);
	}
}
