package org.swrlapi.factory;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.semanticweb.owlapi.formats.PrefixDocumentFormat;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDocumentFormat;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.util.DefaultPrefixManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.swrlapi.core.IRIResolver;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class DefaultIRIResolver implements IRIResolver
{
  private static final Logger log = LoggerFactory.getLogger(DefaultIRIResolver.class);

  @NonNull private final DefaultPrefixManager prefixManager;

  @NonNull private final Map<@NonNull String, @NonNull String> autogenNamespace2Prefix = new HashMap<>();
  @NonNull private final Map<@NonNull String, @NonNull String> autogeneratedPrefix2Namespace = new HashMap<>();
  @NonNull private final Map<@NonNull IRI, @NonNull String> autogenIRI2PrefixedName = new HashMap<>();
  @NonNull private final Map<@NonNull String, @NonNull IRI> autogenPrefixedName2IRI = new HashMap<>();

  private int autogenPrefixNumber = 0;
  private int autogenPrefixedNameNumber = 0;

  @Nullable private String defaultPrefix;

  public DefaultIRIResolver()
  {
    this.prefixManager = new DefaultPrefixManager();
  }

  public DefaultIRIResolver(@NonNull String defaultPrefix)
  {
    this.prefixManager = new DefaultPrefixManager();
    this.prefixManager.setDefaultPrefix(defaultPrefix);
    this.defaultPrefix = defaultPrefix;
  }

  @Override public void reset()
  {
    this.autogenNamespace2Prefix.clear();
    this.autogeneratedPrefix2Namespace.clear();
    this.autogenPrefixNumber = 0;

    this.autogenPrefixedName2IRI.clear();

    this.autogenIRI2PrefixedName.clear();
    this.autogenPrefixedNameNumber = 0;
  }

  @Override @NonNull public Optional<@NonNull IRI> prefixedName2IRI(@NonNull String prefixedName)
  {
    if (this.autogenPrefixedName2IRI.containsKey(prefixedName))
      return Optional.of(this.autogenPrefixedName2IRI.get(prefixedName));
    else {
      String prefix = getPrefix(prefixedName);
      String remainder = getRemainder(prefixedName);
      if (remainder.isEmpty()) {
        return Optional.empty();
      } else { // remainder not empty
        IRI iri = this.prefixManager.getIRI(prefixedName);
        if (iri != null)
          return Optional.of(iri);
        else {
          // Prefix manager does not have a mapping - try the auto-generated cache
          if (this.autogeneratedPrefix2Namespace.containsKey(prefix)) {
            String namespace = this.autogeneratedPrefix2Namespace.get(prefix);
            return Optional.of(IRI.create(namespace, remainder));
          } else
            return Optional.empty(); // Cannot find a match
        }
      }
    }
  }

  @Override public Optional<@NonNull String> iri2PrefixedName(@NonNull IRI iri)
  {
    String existingPrefixedName = this.prefixManager.getPrefixIRI(iri);
    if (existingPrefixedName != null)
      return Optional.of(existingPrefixedName);
    else {
      String namespace = iri.getNamespace();
      com.google.common.base.Optional<@NonNull String> remainder = iri.getRemainder();
      if (remainder.isPresent()) {
        if (namespace.isEmpty()) {
          String prefixedName = remainder.get();
          return Optional.of(prefixedName);
        } else { // OWLAPI prefix manager does not have a prefixed form. We auto-generate a prefix for each namespace.
          return Optional.of(autoGeneratePrefixedName(iri, namespace, remainder.get()));
        }
      } else { // No remainder - auto-generate a prefixed form
        return Optional.of(autoGeneratePrefixedName(iri));
      }
    }
  }

  @Override @NonNull public Optional<@NonNull String> iri2ShortForm(@NonNull IRI iri)
  {
    String shortForm = this.prefixManager.getShortForm(iri);

    if (shortForm == null || shortForm.isEmpty())
      return Optional.empty();
    else
      return Optional.of(shortForm);
  }

  @Override public void setPrefix(@NonNull String prefix, @NonNull String namespace)
  {
    this.prefixManager.setPrefix(prefix, namespace);
  }

  @Override public void updatePrefixes(@NonNull OWLOntology ontology)
  {
    OWLOntologyManager owlOntologyManager = ontology.getOWLOntologyManager();
    OWLDocumentFormat ontologyFormat = owlOntologyManager.getOntologyFormat(ontology);

    this.prefixManager.clear();
    if (this.defaultPrefix != null)
      this.prefixManager.setDefaultPrefix(this.defaultPrefix);

    if (ontologyFormat != null && ontologyFormat.isPrefixOWLOntologyFormat()) {
      PrefixDocumentFormat prefixOntologyFormat = ontologyFormat.asPrefixOWLOntologyFormat();

      Map<@NonNull String, String> map = prefixOntologyFormat.getPrefixName2PrefixMap();
      for (String prefix : map.keySet())
        this.prefixManager.setPrefix(prefix, map.get(prefix));
    }
    addSWRLAPIPrefixes();

    //log.info("updated prefixes " + prefixManager.getPrefixName2PrefixMap());
  }

  @NonNull private String getPrefix(@NonNull String prefixedName)
  {
    int separatorIndex = prefixedName.indexOf(":");

    if (separatorIndex > 0)
      return prefixedName.substring(0, separatorIndex);
    else
      return "";
  }

  @NonNull private String getRemainder(@NonNull String prefixedName)
  {
    int separatorIndex = prefixedName.indexOf(":");

    if (separatorIndex != -1)
      return prefixedName.substring(separatorIndex, prefixedName.length());
    else
      return prefixedName;
  }

  @NonNull private String autoGeneratePrefix(@NonNull String namespace)
  {
    if (this.autogenNamespace2Prefix.containsKey(namespace))
      return this.autogenNamespace2Prefix.get(namespace);
    else {
      String autogeneratedPrefix = "autogen" + this.autogenPrefixNumber++ + ":";
      this.autogenNamespace2Prefix.put(namespace, autogeneratedPrefix);
      this.autogeneratedPrefix2Namespace.put(autogeneratedPrefix, namespace);

      return autogeneratedPrefix;
    }
  }

  @NonNull private String autoGeneratePrefixedName(@NonNull IRI iri)
  {
    if (this.autogenIRI2PrefixedName.containsKey(iri))
      return this.autogenIRI2PrefixedName.get(iri);
    else {
      String autoGeneratedPrefixedName = "autogen:p" + this.autogenPrefixedNameNumber++;
      this.autogenPrefixedName2IRI.put(autoGeneratedPrefixedName, iri);
      this.autogenIRI2PrefixedName.put(iri, autoGeneratedPrefixedName);

      return autoGeneratedPrefixedName;
    }
  }

  @NonNull private String autoGeneratePrefixedName(@NonNull IRI iri, @NonNull String namespace,
    @NonNull String remainder)
  {
    String autogenPrefix = autoGeneratePrefix(namespace);
    String autogenPrefixedName = autogenPrefix + remainder;
    this.autogenPrefixedName2IRI.put(autogenPrefixedName, iri);

    return autogenPrefixedName;
  }

  private void addSWRLAPIPrefixes()
  {
    this.prefixManager.setPrefix("owl:", "http://www.w3.org/2002/07/owl#");
    this.prefixManager.setPrefix("swrl:", "http://www.w3.org/2003/11/swrl#");
    this.prefixManager.setPrefix("swrlb:", "http://www.w3.org/2003/11/swrlb#");
    this.prefixManager.setPrefix("sqwrl:", "http://sqwrl.stanford.edu/ontologies/built-ins/3.4/sqwrl.owl#");
    this.prefixManager.setPrefix("swrlm:", "http://swrl.stanford.edu/ontologies/built-ins/3.4/swrlm.owl#");
    this.prefixManager.setPrefix("temporal:", "http://swrl.stanford.edu/ontologies/built-ins/3.3/temporal.owl#");
    this.prefixManager.setPrefix("swrlx:", "http://swrl.stanford.edu/ontologies/built-ins/3.3/swrlx.owl#");
    this.prefixManager.setPrefix("swrla:", "http://swrl.stanford.edu/ontologies/3.3/swrla.owl#");
  }
}