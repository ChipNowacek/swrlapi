package org.swrlapi.bridge;

import org.semanticweb.owlapi.model.OWLAxiom;
import org.swrlapi.exceptions.BuiltInException;
import org.swrlapi.exceptions.TargetRuleEngineException;
import org.swrlapi.owl2rl.OWL2RLEngine;
import org.swrlapi.sqwrl.SQWRLQuery;

/**
 * This interface defines the methods that must be provided by an implementation of a SWRLAPI-based SWRL rule engine.
 * </p>
 * A SWRL rule engine must also implement an OWL 2 RL reasoner.
 * <p>
 * A target rule engine can communicate with the bridge using the {@link SWRLRuleEngineBridge} interface.
 *
 * @see org.swrlapi.owl2rl.OWL2RLEngine
 * @see org.swrlapi.bridge.SWRLRuleEngineBridge
 */
public interface TargetSWRLRuleEngine
{
	/**
	 * Define a target rule engine representation of an OWL axiom. SWRL rules are a type of OWL axiom.
	 */
	void defineOWLAxiom(OWLAxiom axiom) throws TargetRuleEngineException;

	/**
	 * Define a target rule engine representation of a SQWRL query.
	 */
	void defineSQWRLQuery(SQWRLQuery query) throws TargetRuleEngineException, BuiltInException;

	/**
	 * Run the rule engine.
	 */
	void runRuleEngine() throws TargetRuleEngineException;

	/**
	 * Reset the rule engine.
	 */
	void resetRuleEngine() throws TargetRuleEngineException;

	/**
	 * Return the name of the target rule engine.
	 */
	String getName();

	/**
	 * Return version information of the target rule engine.
	 */
	String getVersion();

	/**
	 * Get the underlying OWL 2 RL reasoner provided by the rule engine.
	 */
	OWL2RLEngine getOWL2RLEngine();
}