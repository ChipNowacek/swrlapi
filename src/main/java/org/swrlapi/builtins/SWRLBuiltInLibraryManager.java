package org.swrlapi.builtins;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.swrlapi.builtins.arguments.SWRLBuiltInArgument;
import org.swrlapi.builtins.arguments.SWRLMultiValueVariableBuiltInArgument;
import org.swrlapi.exceptions.IncompatibleBuiltInMethodException;
import org.swrlapi.exceptions.IncompatibleSWRLBuiltInClassException;
import org.swrlapi.exceptions.SWRLBuiltInException;
import org.swrlapi.exceptions.SWRLBuiltInLibraryException;
import org.swrlapi.exceptions.UnresolvedSWRLBuiltInClassException;
import org.swrlapi.exceptions.UnresolvedSWRLBuiltInMethodException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * This class manages the dynamic loading of SWRL built-in libraries and the invocation of built-ins in those libraries.
 * A library is identified by a prefix and this prefix is used to find and dynamically load a Java class implementing
 * the built-ins in this library.
 *
 * @see org.swrlapi.builtins.SWRLBuiltInLibrary
 */
public class SWRLBuiltInLibraryManager
{
  private static final Logger log = LoggerFactory.getLogger(SWRLBuiltInLibraryManager.class);

  private static final String SWRLBuiltInLibraryPackageBaseName = "org.swrlapi.builtins.";
  private static final String SWRLBuiltInLibraryImplementationClassName = "SWRLBuiltInLibraryImpl";

  // Map of built-in library prefix name to SWRLBuiltInLibrary instance
  @NonNull private final Map<@NonNull String, @NonNull SWRLBuiltInLibrary> swrlBuiltInLibraryImplementations;
  // Map of prefix : methodName to method implementation
  @NonNull private final Map<@NonNull String, @NonNull Method> swrlBuiltInMethods;

  public SWRLBuiltInLibraryManager()
  {
    this.swrlBuiltInLibraryImplementations = new HashMap<>();
    this.swrlBuiltInMethods = new HashMap<>();
  }

  public void loadSWRLBuiltInLibraries(@NonNull String pathToSWRLBuiltInsDirectory)
  {
    // TODO Where do we call this from?
    // TODO Need to be careful we do not load classes more than once
    // TODO library.reset() not called using this approach
    // TODO Return list of built-in IRIs from here so that SWRLAPIOWLOntology implementation can register them?

    try {
      Map<@NonNull String, @NonNull Set<@NonNull String>> swrlBuiltInLibraryJARs = getSWRLBuiltInLibraryJARs(
        pathToSWRLBuiltInsDirectory);

      for (String swrlBuiltInLibraryJARFilePath : swrlBuiltInLibraryJARs.keySet()) {
        URL[] classLoaderURLs = new URL[] { new URL(swrlBuiltInLibraryJARFilePath) };
        URLClassLoader classLoader = new URLClassLoader(classLoaderURLs, this.getClass().getClassLoader());
        for (String swrlBuiltInLibraryImplementationClassName : swrlBuiltInLibraryJARs
          .get(swrlBuiltInLibraryJARFilePath)) {
          Optional<String> swrlBuiltInLibraryPrefix = extractSWRLBuiltInLibraryPrefixFromClassName(
            swrlBuiltInLibraryImplementationClassName);
          if (swrlBuiltInLibraryPrefix.isPresent()) {

            Class swrlBuiltInLibraryImplementationClass = Class
              .forName(swrlBuiltInLibraryImplementationClassName, true, classLoader);
            if (!this.swrlBuiltInLibraryImplementations.containsKey(swrlBuiltInLibraryPrefix.get())) {
              SWRLBuiltInLibrary library = loadSWRLBuiltInLibraryImplementationClass(swrlBuiltInLibraryPrefix.get(),
                swrlBuiltInLibraryImplementationClassName);
              this.swrlBuiltInLibraryImplementations.put(swrlBuiltInLibraryPrefix.get(), library);
            } else
              log.warn("Could not extract a valid prefix from built-in library class "
                + swrlBuiltInLibraryImplementationClassName);
          }
        }
      }
    } catch (IOException e) {
      // TODO
    } catch (ClassNotFoundException e) {
      // TODO

    }
  }

  /**
   * @param swrlBuiltInsDirectoryPath
   * @return A map of built-in JAR names to a set of built-in library implementation classes in each JAR
   * @throws IOException If an error occurs during processing
   */
  @NonNull private Map<@NonNull String, @NonNull Set<@NonNull String>> getSWRLBuiltInLibraryJARs(
    @NonNull String swrlBuiltInsDirectoryPath) throws IOException
  {
    File swrlBuiltInsDirectory = new File(swrlBuiltInsDirectoryPath);
    Map<@NonNull String, @NonNull Set<@NonNull String>> swrlBuiltInLibraryJARs = new HashMap<>();

    for (File swrlBuiltInLibraryJARFile : swrlBuiltInsDirectory.listFiles()) {
      ZipInputStream jarFileStream = new ZipInputStream(new FileInputStream(swrlBuiltInLibraryJARFile));
      String swrlBuiltInLibraryJARFilePath = swrlBuiltInLibraryJARFile.getAbsolutePath();
      for (ZipEntry entry = jarFileStream.getNextEntry(); entry != null; entry = jarFileStream.getNextEntry()) {
        if (entry.getName().endsWith(".class") && !entry.isDirectory()) {
          String className = getClassNameFromEntry(entry);
          if (className.startsWith(SWRLBuiltInLibraryPackageBaseName) && className
            .endsWith("." + SWRLBuiltInLibraryImplementationClassName)) {
            String swrlBuiltInLibraryImplementationClassName = className;
            if (swrlBuiltInLibraryJARs.containsKey(swrlBuiltInLibraryJARFilePath))
              swrlBuiltInLibraryJARs.get(swrlBuiltInLibraryJARFilePath).add(swrlBuiltInLibraryImplementationClassName);
            else {
              Set<@NonNull String> swrlBuiltInLibraryImplementationClassNames = new HashSet<>();
              swrlBuiltInLibraryImplementationClassNames.add(swrlBuiltInLibraryImplementationClassName);
              swrlBuiltInLibraryJARs.put(swrlBuiltInLibraryJARFilePath, swrlBuiltInLibraryImplementationClassNames);
            }
          }
        }
      }
    }
    return swrlBuiltInLibraryJARs;
  }

  /**
   * Invoke a SWRL built-in. This method is called from the invokeSWRLBuiltIn method in the
   * {@link org.swrlapi.bridge.SWRLRuleEngineBridge} and should not be called directly from a rule engine. The built-in
   * name should be the prefixed name of the built-in (e.g., swrlb:lessThanOrEqual).
   * <p/>
   * For built-ins that evaluate to true, this method will return a list of argument patterns, one pattern for each
   * combination of arguments that evaluates to true.
   * <p/>
   * If the built-in evaluates to false, it will return an empty argument pattern list.
   *
   * @param bridge         The built-in bridge
   * @param ruleName       The name of the invoking rule
   * @param builtInName    The invoked built-in
   * @param builtInIndex   The 0-based index of the invoked built-in
   * @param isInConsequent If the built-in in the consequent?
   * @param arguments      The built-in arguments
   * @return The result of the built-in
   * @throws SWRLBuiltInException If an exception occurs during invocation
   */
  @NonNull public List<@NonNull List<@NonNull SWRLBuiltInArgument>> invokeSWRLBuiltIn(@NonNull SWRLBuiltInBridge bridge,
    @NonNull String ruleName, @NonNull String builtInName, int builtInIndex, boolean isInConsequent,
    @NonNull List<@NonNull SWRLBuiltInArgument> arguments) throws SWRLBuiltInException
  {
    String prefix = getPrefix(builtInName);
    String swrlBuiltinLibraryImplementationClassName = getBuiltInLibraryImplementationClassName(prefix);
    String builtInMethodName = getBuiltInMethodName(builtInName);
    SWRLBuiltInLibrary library = getSWRLBuiltInLibraryImplementation(bridge, prefix,
      swrlBuiltinLibraryImplementationClassName);
    Method method = resolveSWRLBuiltInMethod(ruleName, library, prefix, builtInMethodName);
    List<@NonNull List<@NonNull SWRLBuiltInArgument>> argumentPatterns = new ArrayList<>();

    if (library.invokeBuiltInMethod(method, bridge, ruleName, prefix, builtInMethodName, builtInIndex, isInConsequent,
      arguments)) {

      if (hasUnboundArguments(arguments)) // Make sure the built-in has bound all of its arguments.
        throw new SWRLBuiltInException("built-in " + builtInName + "(index " + builtInIndex + ") in rule " + ruleName
          + " returned with unbound arguments");

      processBoundArguments(arguments);

      argumentPatterns.addAll(generateBuiltInArgumentPattern(ruleName, builtInName, builtInIndex, arguments).stream()
        .collect(Collectors.toList()));
    }

    return argumentPatterns;
  }

  private void processBoundArguments(@NonNull List<@NonNull SWRLBuiltInArgument> arguments) throws SWRLBuiltInException
  {
    for (int argumentIndex = 0; argumentIndex < arguments.size(); argumentIndex++) {
      SWRLBuiltInArgument argument = arguments.get(argumentIndex);
      if (argument.isVariable() && argument.asVariable().hasBuiltInResult()) {
        Optional<SWRLBuiltInArgument> builtInResult = argument.asVariable().getBuiltInResult();

        if (builtInResult.isPresent())
          arguments.set(argumentIndex, builtInResult.get());
      }
    }
  }

  @NonNull private SWRLBuiltInLibrary getSWRLBuiltInLibraryImplementation(@NonNull SWRLBuiltInBridge bridge,
    @NonNull String prefix, @NonNull String swrlBuiltInLibraryImplementationClassName)
    throws SWRLBuiltInLibraryException
  {
    if (this.swrlBuiltInLibraryImplementations.containsKey(prefix)) { // Find the cached implementation.
      return this.swrlBuiltInLibraryImplementations.get(prefix);
    } else { // Implementation class not loaded - load it, cache it, and call its reset method.
      SWRLBuiltInLibrary library = loadSWRLBuiltInLibraryImplementationClass(prefix,
        swrlBuiltInLibraryImplementationClassName);
      this.swrlBuiltInLibraryImplementations.put(prefix, library);
      invokeBuiltInLibraryResetMethod(bridge, library);
      return library;
    }
  }

  /**
   * Invoke the reset() method for each registered built-in library.
   */
  private void invokeBuiltInLibraryResetMethod(@NonNull SWRLBuiltInBridge bridge, @NonNull SWRLBuiltInLibrary library)
    throws SWRLBuiltInLibraryException
  {
    try {
      library.invokeResetMethod(bridge);
    } catch (Exception e) {
      throw new SWRLBuiltInLibraryException("error calling reset method in built-in library " + library.getClass());
    }
  }

  public void invokeAllBuiltInLibrariesResetMethod(@NonNull SWRLBuiltInBridge bridge) throws SWRLBuiltInLibraryException
  {
    for (SWRLBuiltInLibrary library : this.swrlBuiltInLibraryImplementations.values())
      invokeBuiltInLibraryResetMethod(bridge, library);
  }

  /**
   * This method is called with a list of built-in arguments. Some argument positions may contain multi-arguments,
   * indicating that there is more than one pattern. If the result has more than one multi-argument, each multi-argument
   * must have the same number of elements.
   */
  @NonNull Set<@NonNull List<@NonNull SWRLBuiltInArgument>> generateBuiltInArgumentPattern(@NonNull String ruleName,
    @NonNull String builtInName, int builtInIndex, @NonNull List<@NonNull SWRLBuiltInArgument> arguments)
    throws SWRLBuiltInException
  {
    List<@NonNull Integer> multiValueBuiltInArgumentIndexes = getMultiValueBuiltInArgumentIndexes(arguments);
    Set<@NonNull List<@NonNull SWRLBuiltInArgument>> pattern = new HashSet<>();

    if (multiValueBuiltInArgumentIndexes.isEmpty()) // No multi-arguments - generate a single pattern
      pattern.add(arguments);
    else { // Generate all possible patterns
      int firstMultiValueBuiltInArgumentIndex = multiValueBuiltInArgumentIndexes.get(0); // Pick the first
      // multi-argument.
      SWRLMultiValueVariableBuiltInArgument multiValueBuiltInArgument = getArgumentAsASWRLMultiValueBuiltInArgument(
        arguments, firstMultiValueBuiltInArgumentIndex);
      int numberOfArgumentsInMultiValueBuiltInArgument = multiValueBuiltInArgument.getNumberOfArguments();

      if (numberOfArgumentsInMultiValueBuiltInArgument < 1)
        throw new SWRLBuiltInException(
          "empty multi-value argument for built-in " + builtInName + "(index " + builtInIndex + ") in rule "
            + ruleName);

      for (int i = 1; i < multiValueBuiltInArgumentIndexes.size(); i++) {
        int multiValueBuiltInArgumentIndex = multiValueBuiltInArgumentIndexes.get(i);
        multiValueBuiltInArgument = getArgumentAsASWRLMultiValueBuiltInArgument(arguments,
          multiValueBuiltInArgumentIndex);
        if (numberOfArgumentsInMultiValueBuiltInArgument != multiValueBuiltInArgument.getNumberOfArguments())
          throw new SWRLBuiltInException(
            "all multi-value arguments must have the same number of elements for built-in " + builtInName + "(index "
              + builtInIndex + ") in rule " + ruleName);
      }

      for (int multiValueBuiltInArgumentArgumentIndex = 0; multiValueBuiltInArgumentArgumentIndex
        < numberOfArgumentsInMultiValueBuiltInArgument; multiValueBuiltInArgumentArgumentIndex++) {
        List<@NonNull SWRLBuiltInArgument> argumentsPattern = generateArgumentsPattern(arguments,
          multiValueBuiltInArgumentArgumentIndex);
        pattern.add(argumentsPattern);
      }
    }
    return pattern;
  }

  @NonNull private SWRLMultiValueVariableBuiltInArgument getArgumentAsASWRLMultiValueBuiltInArgument(
    @NonNull List<@NonNull SWRLBuiltInArgument> arguments, int argumentIndex) throws SWRLBuiltInException
  {
    if (arguments.get(argumentIndex) instanceof SWRLMultiValueVariableBuiltInArgument)
      return (SWRLMultiValueVariableBuiltInArgument)arguments.get(argumentIndex);
    else
      throw new SWRLBuiltInException("expecting milti-argment for (0-indexed) argument #" + argumentIndex);
  }

  // Find indices of multi-arguments (if any) in a list of arguments.
  @NonNull private List<@NonNull Integer> getMultiValueBuiltInArgumentIndexes(
    @NonNull List<@NonNull SWRLBuiltInArgument> arguments)
  {
    List<@NonNull Integer> result = new ArrayList<>();

    for (int i = 0; i < arguments.size(); i++)
      if (arguments.get(i) instanceof SWRLMultiValueVariableBuiltInArgument)
        result.add(i);

    return result;
  }

  @NonNull private List<@NonNull SWRLBuiltInArgument> generateArgumentsPattern(
    @NonNull List<@NonNull SWRLBuiltInArgument> arguments, int multiValueBuiltInArgumentArgumentIndex)
  {
    List<@NonNull SWRLBuiltInArgument> result = new ArrayList<>();

    for (SWRLBuiltInArgument argument : arguments) {
      if (argument instanceof SWRLMultiValueVariableBuiltInArgument) {
        SWRLMultiValueVariableBuiltInArgument multiValueBuiltInArgument = (SWRLMultiValueVariableBuiltInArgument)argument;
        result.add(multiValueBuiltInArgument.getArguments().get(multiValueBuiltInArgumentArgumentIndex));
      } else
        result.add(argument);
    }

    return result;
  }

  @NonNull private Method resolveSWRLBuiltInMethod(@NonNull String ruleName, @NonNull SWRLBuiltInLibrary library,
    @NonNull String prefix, @NonNull String builtInMethodName) throws UnresolvedSWRLBuiltInMethodException
  {
    String key = prefix + ":" + builtInMethodName;

    if (swrlBuiltInMethods.containsKey(key))
      return swrlBuiltInMethods.get(key);
    else {
      try {
        Method method = library.getClass().getMethod(builtInMethodName, List.class);

        checkSWRLBuiltInMethodSignature(ruleName, prefix, builtInMethodName, method); // Check signature of method

        swrlBuiltInMethods.put(key, method);

        return method;
      } catch (Exception e) {
        throw new UnresolvedSWRLBuiltInMethodException(ruleName, prefix, builtInMethodName,
          e.getMessage() != null ? e.getMessage() : "", e);
      }
    }
  }

  // TODO Need to get constructor of library to catch exceptions it may throw.
  @NonNull private SWRLBuiltInLibrary loadSWRLBuiltInLibraryImplementationClass(@NonNull String prefix,
    @NonNull String swrlBuiltInLibraryImplementationClassName) throws SWRLBuiltInLibraryException
  {
    Class<?> swrlBuiltInLibraryImplementationClass;

    try {
      swrlBuiltInLibraryImplementationClass = Class.forName(swrlBuiltInLibraryImplementationClassName);
    } catch (Exception e) {
      throw new UnresolvedSWRLBuiltInClassException(prefix, e.getMessage() != null ? e.getMessage() : "", e);
    }

    // Check implementation class for compatibility
    checkSWRLBuiltInLibraryImplementationClassCompatibility(prefix, swrlBuiltInLibraryImplementationClass);

    try {
      return (SWRLBuiltInLibrary)swrlBuiltInLibraryImplementationClass.newInstance();
    } catch (@NonNull InstantiationException | ExceptionInInitializerError | SecurityException | IllegalAccessException e) {
      throw new IncompatibleSWRLBuiltInClassException(prefix, swrlBuiltInLibraryImplementationClassName,
        e.getMessage() != null ? e.getMessage() : "", e);
    }
  }

  private void checkSWRLBuiltInMethodSignature(@NonNull String ruleName, @NonNull String prefix,
    @NonNull String builtInURI, @NonNull Method method) throws IncompatibleBuiltInMethodException
  {
    if (method.getReturnType() != Boolean.TYPE)
      throw new IncompatibleBuiltInMethodException(ruleName, prefix, builtInURI, "Java method must return a boolean");

    Class<?> exceptionTypes[] = method.getExceptionTypes();

    if ((exceptionTypes.length != 1) || (exceptionTypes[0] != SWRLBuiltInException.class))
      throw new IncompatibleBuiltInMethodException(ruleName, prefix, builtInURI,
        "Java method must throw a single exception of type BuiltInException");

    Type parameterTypes[] = method.getGenericParameterTypes();

    if ((parameterTypes.length != 1) || (!(parameterTypes[0] instanceof ParameterizedType)) || (
      ((ParameterizedType)parameterTypes[0]).getRawType() != List.class) || (
      ((ParameterizedType)parameterTypes[0]).getActualTypeArguments().length != 1) || (
      ((ParameterizedType)parameterTypes[0]).getActualTypeArguments()[0] != SWRLBuiltInArgument.class))
      throw new IncompatibleBuiltInMethodException(ruleName, prefix, builtInURI,
        "Java built-in method implementation must accept a single List of SWRLBuiltInArgument objects");
  }

  private void checkSWRLBuiltInLibraryImplementationClassCompatibility(@NonNull String prefix, @NonNull Class<?> cls)
    throws IncompatibleSWRLBuiltInClassException
  {
    if (!SWRLBuiltInLibrary.class.isAssignableFrom(cls))
      throw new IncompatibleSWRLBuiltInClassException(prefix, cls.getName(),
        "Java class does not extend SWRLBuiltInLibrary");
  }

  private boolean hasUnboundArguments(@NonNull List<@NonNull SWRLBuiltInArgument> arguments) throws SWRLBuiltInException
  {
    for (SWRLBuiltInArgument argument : arguments)
      if (argument.isVariable() && argument.asVariable().isUnbound())
        return true;

    return false;
  }

  @NonNull private Optional<String> extractSWRLBuiltInLibraryPrefixFromClassName(
    @NonNull String swrlBuiltInLibraryImplementationClassName)
  {
    if (swrlBuiltInLibraryImplementationClassName.length() > (SWRLBuiltInLibraryPackageBaseName.length()
      + SWRLBuiltInLibraryImplementationClassName.length())) {
      String swrlBuiltInLibraryPrefix = swrlBuiltInLibraryImplementationClassName
        .substring(SWRLBuiltInLibraryPackageBaseName.length(),
          swrlBuiltInLibraryImplementationClassName.length() - SWRLBuiltInLibraryImplementationClassName.length());

      if (isValidJavaIdentifier(swrlBuiltInLibraryPrefix))
        return Optional.of(swrlBuiltInLibraryPrefix);
      else {
        log.warn("Invalid SWRL built-in library implementation prefix " + swrlBuiltInLibraryImplementationClassName);
        return Optional.empty();
      }
    } else {
      log.warn("Invalid SWRL built-in library implementation class name " + swrlBuiltInLibraryImplementationClassName);
      return Optional.empty();
    }
  }

  @NonNull private String getPrefix(@NonNull String builtInName)
  {
    int hashIndex = builtInName.indexOf(':');

    if (hashIndex != -1) {
      return builtInName.substring(0, hashIndex);
    } else
      return ""; // No prefix - try the base built-ins package. Ordinarily, built-ins should not be located here.
  }

  @NonNull private String getBuiltInLibraryImplementationClassName(@NonNull String prefix)
  {
    if (prefix.length() == 0)
      return SWRLBuiltInLibraryPackageBaseName + SWRLBuiltInLibraryImplementationClassName;
    else {
      return SWRLBuiltInLibraryPackageBaseName + prefix + "." + SWRLBuiltInLibraryImplementationClassName;
    }
  }

  @NonNull private String getBuiltInMethodName(@NonNull String builtInName)
  {
    if (!builtInName.contains(":"))
      return builtInName;
    else
      return builtInName.substring(builtInName.indexOf(":") + 1, builtInName.length());
  }

  @NotNull private String getClassNameFromEntry(@NonNull ZipEntry entry)
  {
    StringBuilder className = new StringBuilder();
    for (String part : entry.getName().split("/")) {
      if (className.length() != 0)
        className.append(".");
      className.append(part);
      if (part.endsWith(".class"))
        className.setLength(className.length() - ".class".length());
    }
    return className.toString();
  }

  private static boolean isValidJavaIdentifier(@NonNull String s)
  {
    if (s == null || s.length() == 0)
      return false;

    char[] c = s.toCharArray();
    if (!Character.isJavaIdentifierStart(c[0]))
      return false;

    for (int i = 1; i < c.length; i++) {
      if (!Character.isJavaIdentifierPart(c[i]))
        return false;
    }

    return true;
  }
}
