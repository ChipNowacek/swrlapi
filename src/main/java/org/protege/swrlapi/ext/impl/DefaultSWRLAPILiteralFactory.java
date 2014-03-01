package org.protege.swrlapi.ext.impl;

import java.net.URI;

import org.protege.swrlapi.ext.OWLDatatypeFactory;
import org.protege.swrlapi.ext.OWLLiteralFactory;
import org.protege.swrlapi.ext.SWRLAPILiteral;
import org.protege.swrlapi.ext.SWRLAPILiteralFactory;
import org.protege.swrlapi.xsd.XSDDate;
import org.protege.swrlapi.xsd.XSDDateTime;
import org.protege.swrlapi.xsd.XSDDuration;
import org.protege.swrlapi.xsd.XSDTime;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.model.OWLLiteral;

public class DefaultSWRLAPILiteralFactory implements SWRLAPILiteralFactory
{
	private final OWLDatatypeFactory owlDatatypeFactory;
	private final OWLLiteralFactory owlLiteralFactory;

	public DefaultSWRLAPILiteralFactory()
	{
		this.owlDatatypeFactory = new DefaultOWLDatatypeFactory();
		this.owlLiteralFactory = new DefaultOWLLiteralFactory(this.owlDatatypeFactory);
	}

	@Override
	public SWRLAPILiteral getSWRLAPILiteral(String literal, OWLDatatype datatype)
	{
		return new DefaultSWRLAPILiteral(getOWLLiteralFactory().getOWLLiteral(literal, datatype));
	}

	@Override
	public SWRLAPILiteral getSWRLAPILiteral(OWLLiteral literal)
	{
		return new DefaultSWRLAPILiteral(literal);
	}

	@Override
	public SWRLAPILiteral getSWRLAPILiteral(String value)
	{
		return new DefaultSWRLAPILiteral(getOWLLiteralFactory().getOWLLiteral(value));
	}

	@Override
	public SWRLAPILiteral getSWRLAPILiteral(boolean value)
	{
		return new DefaultSWRLAPILiteral(getOWLLiteralFactory().getOWLLiteral(value));
	}

	@Override
	public SWRLAPILiteral getSWRLAPILiteral(double value)
	{
		return new DefaultSWRLAPILiteral(getOWLLiteralFactory().getOWLLiteral(value));
	}

	@Override
	public SWRLAPILiteral getSWRLAPILiteral(float value)
	{
		return new DefaultSWRLAPILiteral(getOWLLiteralFactory().getOWLLiteral(value));
	}

	@Override
	public SWRLAPILiteral getSWRLAPILiteral(int value)
	{
		return new DefaultSWRLAPILiteral(getOWLLiteralFactory().getOWLLiteral(value));
	}

	@Override
	public SWRLAPILiteral getSWRLAPILiteral(byte b)
	{
		return new DefaultSWRLAPILiteral(getOWLLiteralFactory().getOWLLiteral(b));
	}

	@Override
	public SWRLAPILiteral getSWRLAPILiteral(short s)
	{
		return new DefaultSWRLAPILiteral(getOWLLiteralFactory().getOWLLiteral(s));
	}

	@Override
	public SWRLAPILiteral getSWRLAPILiteral(URI uri)
	{
		return new DefaultSWRLAPILiteral(getOWLLiteralFactory().getOWLLiteral(uri));
	}

	@Override
	public SWRLAPILiteral getSWRLAPILiteral(XSDDate date)
	{
		return new DefaultSWRLAPILiteral(getOWLLiteralFactory().getOWLLiteral(date));
	}

	@Override
	public SWRLAPILiteral getSWRLAPILiteral(XSDTime time)
	{
		return new DefaultSWRLAPILiteral(getOWLLiteralFactory().getOWLLiteral(time));
	}

	@Override
	public SWRLAPILiteral getSWRLAPILiteral(XSDDateTime datetime)
	{
		return new DefaultSWRLAPILiteral(getOWLLiteralFactory().getOWLLiteral(datetime));
	}

	@Override
	public SWRLAPILiteral getSWRLAPILiteral(XSDDuration duration)
	{
		return new DefaultSWRLAPILiteral(getOWLLiteralFactory().getOWLLiteral(duration));
	}

	private OWLLiteralFactory getOWLLiteralFactory()
	{
		return this.owlLiteralFactory;
	}
}
