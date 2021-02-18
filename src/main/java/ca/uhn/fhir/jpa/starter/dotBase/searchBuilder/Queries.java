package ca.uhn.fhir.jpa.starter.dotBase.searchBuilder;

public class Queries {

  protected static String matchAll(String findFieldName, String searchFieldName) {
    return (
      "SELECT r." +
      findFieldName +
      " FROM ResourceLink r WHERE r." +
      searchFieldName +
      " IN (:target_pids) "
    );
  }

  protected static String notMatchAll(
    String findFieldName,
    String searchFieldName,
    String targetResourceType,
    boolean haveTargetTypesDefinedByParam
  ) {
    if (targetResourceType != null) return Queries.withTargetResourceType(
      findFieldName,
      searchFieldName
    ); else if (
      haveTargetTypesDefinedByParam
    ) return Queries.haveTargetTypesDefinedByParam(
      findFieldName,
      searchFieldName
    ); else return Queries.NoTargetResourceType(findFieldName, searchFieldName);
  }

  private static String withTargetResourceType(
    String findFieldName,
    String searchFieldName
  ) {
    return (
      "SELECT r." +
      findFieldName +
      ",r.myTargetResourceUrl  FROM ResourceLink r WHERE r.mySourcePath = :src_path AND r." +
      searchFieldName +
      " IN (:target_pids) AND r.myTargetResourceType = :target_resource_type"
    );
  }

  private static String haveTargetTypesDefinedByParam(
    String findFieldName,
    String searchFieldName
  ) {
    return (
      "SELECT r." +
      findFieldName +
      ",r.myTargetResourceUrl  FROM ResourceLink r WHERE r.mySourcePath = :src_path AND r." +
      searchFieldName +
      " IN (:target_pids) AND r.myTargetResourceType in (:target_resource_types)"
    );
  }

  private static String NoTargetResourceType(
    String findFieldName,
    String searchFieldName
  ) {
    return (
      "SELECT r." +
      findFieldName +
      ",r.myTargetResourceUrl  FROM ResourceLink r WHERE r.mySourcePath = :src_path AND r." +
      searchFieldName +
      " IN (:target_pids)"
    );
  }
}
