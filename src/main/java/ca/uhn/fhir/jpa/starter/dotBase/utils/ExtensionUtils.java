package ca.uhn.fhir.jpa.starter.dotBase.utils;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.instance.model.api.IBaseHasExtensions;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.StringType;

public class ExtensionUtils {

  public static void addExtension(IBaseResource theResource, String theUrl, String theValue) {
    try {
      IBaseHasExtensions baseHasExtensions = validateExtensionSupport(theResource);
      IBaseExtension<?, ?> extension = baseHasExtensions.addExtension();
      extension.setUrl(theUrl);
      extension.setValue(new StringType(theValue));
    } catch (Exception e) {
      return;
    }
  }

  public static boolean hasExtension(
    IBase theBase,
    String theExtensionUrl,
    String theExtensionValue
  ) {
    if (!hasExtension(theBase, theExtensionUrl)) {
      return false;
    }
    IBaseDatatype value = getExtensionByUrl(theBase, theExtensionUrl).getValue();
    if (value == null) {
      return theExtensionValue == null;
    }
    return value.toString().equals(theExtensionValue);
  }

  public static boolean hasExtension(IBase theBase, String theExtensionUrl) {
    IBaseHasExtensions baseHasExtensions;
    try {
      baseHasExtensions = validateExtensionSupport(theBase);
    } catch (Exception e) {
      return false;
    }

    return getExtensionByUrl(baseHasExtensions, theExtensionUrl) != null;
  }

  public static IBaseExtension<?, ?> getExtensionByUrl(IBase theBase, String theExtensionUrl) {
    Predicate<IBaseExtension<?, ?>> filter;
    if (theExtensionUrl == null) {
      filter = (e -> true);
    } else {
      filter = (e -> theExtensionUrl.equals(e.getUrl()));
    }

    return getExtensionsMatchingPredicate(theBase, filter).stream().findFirst().orElse(null);
  }

  public static List<IBaseExtension<?, ?>> getExtensionsMatchingPredicate(
    IBase theBase,
    Predicate<? super IBaseExtension<?, ?>> theFilter
  ) {
    return validateExtensionSupport(theBase)
      .getExtension()
      .stream()
      .filter(theFilter)
      .collect(Collectors.toList());
  }

  private static IBaseHasExtensions validateExtensionSupport(IBase theBase) {
    if (!(theBase instanceof IBaseHasExtensions)) {
      throw new IllegalArgumentException(
        String.format("Expected instance that supports extensions, but got %s", theBase)
      );
    }
    return (IBaseHasExtensions) theBase;
  }
}
