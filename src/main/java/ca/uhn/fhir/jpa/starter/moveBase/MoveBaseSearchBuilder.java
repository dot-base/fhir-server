package ca.uhn.fhir.jpa.starter.moveBase;

import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.RuntimeResourceDefinition;
import ca.uhn.fhir.context.RuntimeSearchParam;
import ca.uhn.fhir.interceptor.api.HookParams;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.jpa.api.dao.IDao;
import ca.uhn.fhir.jpa.dao.SearchBuilder;
import ca.uhn.fhir.jpa.interceptor.JpaPreResourceAccessDetails;
import ca.uhn.fhir.jpa.model.entity.ResourceTable;
import ca.uhn.fhir.jpa.searchparam.registry.ISearchParamRegistry;
import ca.uhn.fhir.jpa.util.JpaInterceptorBroadcaster;
import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.model.primitive.InstantDt;
import ca.uhn.fhir.rest.api.server.IPreResourceAccessDetails;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.api.server.storage.ResourcePersistentId;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import ca.uhn.fhir.util.StopWatch;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.springframework.beans.factory.annotation.Autowired;

public class MoveBaseSearchBuilder extends SearchBuilder {
  @Autowired
  private ISearchParamRegistry mySearchParamRegistry;

  private static final org.slf4j.Logger ourLog = org.slf4j.LoggerFactory.getLogger(
    MoveBaseSearchBuilder.class
  );

  public MoveBaseSearchBuilder(
    IDao theDao,
    String theResourceName,
    Class<? extends IBaseResource> theResourceType
  ) {
    super(theDao, theResourceName, theResourceType);
    ourLog.info("MoveBaseSearchBuilder");
  }

  @Override
  public HashSet<ResourcePersistentId> loadIncludes(
    FhirContext theContext,
    EntityManager theEntityManager,
    Collection<ResourcePersistentId> theMatches,
    Set<Include> theRevIncludes,
    boolean theReverseMode,
    DateRangeParam theLastUpdated,
    String theSearchIdOrDescription,
    RequestDetails theRequest
  ) {
    // HashSet<ResourcePersistentId> addAll = super.loadIncludes(
    // theContext,
    // theEntityManager,
    // theMatches,
    // theRevIncludes,
    // theReverseMode,
    // theLastUpdated,
    // theSearchIdOrDescription,
    // theRequest
    // );
    // return addAll;
    if (theMatches.size() == 0) {
      return new HashSet<>();
    }
    if (theRevIncludes == null || theRevIncludes.isEmpty()) {
      return new HashSet<>();
    }
    String searchFieldName = theReverseMode
      ? "myTargetResourcePid"
      : "mySourceResourcePid";
    String findFieldName = theReverseMode ? "mySourceResourcePid" : "myTargetResourcePid";

    Collection<ResourcePersistentId> nextRoundMatches = theMatches;
    HashSet<ResourcePersistentId> allAdded = new HashSet<>();
    HashSet<ResourcePersistentId> original = new HashSet<>(theMatches);
    ArrayList<Include> includes = new ArrayList<>(theRevIncludes);

    int roundCounts = 0;
    StopWatch w = new StopWatch();

    boolean addedSomeThisRound;
    do {
      roundCounts++;

      HashSet<ResourcePersistentId> pidsToInclude = new HashSet<>();

      for (Iterator<Include> iter = includes.iterator(); iter.hasNext();) {
        Include nextInclude = iter.next();
        if (nextInclude.isRecurse() == false) {
          iter.remove();
        }

        boolean matchAll = "*".equals(nextInclude.getValue());
        if (matchAll) {
          String sql;
          sql =
            "SELECT r." +
            findFieldName +
            " FROM ResourceLink r WHERE r." +
            searchFieldName +
            " IN (:target_pids) ";
          List<Collection<ResourcePersistentId>> partitions = partition(
            nextRoundMatches,
            getMaximumPageSize()
          );
          for (Collection<ResourcePersistentId> nextPartition : partitions) {
            TypedQuery<Long> q = theEntityManager.createQuery(sql, Long.class);
            q.setParameter("target_pids", ResourcePersistentId.toLongList(nextPartition));
            List<Long> results = q.getResultList();
            for (Long resourceLink : results) {
              if (theReverseMode) {
                pidsToInclude.add(new ResourcePersistentId(resourceLink));
              } else {
                pidsToInclude.add(new ResourcePersistentId(resourceLink));
              }
            }
          }
        } else {
          List<String> paths;
          RuntimeSearchParam param;
          String resType = nextInclude.getParamType();
          if (isBlank(resType)) {
            continue;
          }
          RuntimeResourceDefinition def = theContext.getResourceDefinition(resType);
          if (def == null) {
            ourLog.warn(
              "Unknown resource type in include/revinclude=" + nextInclude.getValue()
            );
            continue;
          }

          String paramName = nextInclude.getParamName();
          if (isNotBlank(paramName)) {
            param = mySearchParamRegistry.getSearchParamByName(def, paramName);
          } else {
            param = null;
          }
          if (param == null) {
            ourLog.warn(
              "Unknown param name in include/revinclude=" + nextInclude.getValue()
            );
            continue;
          }

          paths = param.getPathsSplit();

          String targetResourceType = defaultString(
            nextInclude.getParamTargetType(),
            null
          );
          for (String nextPath : paths) {
            String sql;

            boolean haveTargetTypesDefinedByParam = param.hasTargets();
            if (targetResourceType != null) {
              sql =
                "SELECT r." +
                findFieldName +
                " FROM ResourceLink r WHERE r.mySourcePath = :src_path AND r." +
                searchFieldName +
                " IN (:target_pids) AND r.myTargetResourceType = :target_resource_type";
            } else if (haveTargetTypesDefinedByParam) {
              sql =
                "SELECT r." +
                findFieldName +
                ",r.myTargetResourceUrl  FROM ResourceLink r WHERE r.mySourcePath = :src_path AND r." +
                searchFieldName +
                " IN (:target_pids) AND r.myTargetResourceType in (:target_resource_types)";
            } else {
              sql =
                "SELECT r." +
                findFieldName +
                " FROM ResourceLink r WHERE r.mySourcePath = :src_path AND r." +
                searchFieldName +
                " IN (:target_pids)";
            }

            List<Collection<ResourcePersistentId>> partitions = partition(
              nextRoundMatches,
              getMaximumPageSize()
            );
            for (Collection<ResourcePersistentId> nextPartition : partitions) {
              TypedQuery<Object[]> q = theEntityManager.createQuery(sql, Object[].class);
              q.setParameter("src_path", nextPath);
              q.setParameter(
                "target_pids",
                ResourcePersistentId.toLongList(nextPartition)
              );
              if (targetResourceType != null) {
                q.setParameter("target_resource_type", targetResourceType);
              } else if (haveTargetTypesDefinedByParam) {
                q.setParameter("target_resource_types", param.getTargets());
              }
              List<Object[]> results = q.getResultList();
              for (Object[] resourceLink : results) {
                if (resourceLink != null) if (
                  resourceLink.length == 2 && resourceLink[1] != null
                ) theRequest.setAttribute(
                  "includesExternalReference",
                  resourceLink[1]
                ); else pidsToInclude.add(new ResourcePersistentId(resourceLink[0]));
              }
            }
          }
        }
      }
      Map<String, String[]> map = new <String, String[]>HashMap();
      map.put("some", new String[] { "someValue" });
      theRequest.setParameters(map);
      if (theReverseMode) {
        if (
          theLastUpdated != null &&
          (
            theLastUpdated.getLowerBoundAsInstant() != null ||
            theLastUpdated.getUpperBoundAsInstant() != null
          )
        ) {
          pidsToInclude =
            new HashSet<>(
              filterResourceIdsByLastUpdated(
                theEntityManager,
                theLastUpdated,
                pidsToInclude
              )
            );
        }
      }
      for (ResourcePersistentId next : pidsToInclude) {
        if (original.contains(next) == false && allAdded.contains(next) == false) {
          theMatches.add(next);
        }
      }

      addedSomeThisRound = allAdded.addAll(pidsToInclude);
      nextRoundMatches = pidsToInclude;
    } while (includes.size() > 0 && nextRoundMatches.size() > 0 && addedSomeThisRound);

    allAdded.removeAll(original);

    ourLog.info(
      "Loaded {} {} in {} rounds and {} ms for search {}",
      allAdded.size(),
      theReverseMode ? "_revincludes" : "_includes",
      roundCounts,
      w.getMillisAndRestart(),
      theSearchIdOrDescription
    );

    // if results contains no PID but URL add to request for RESPONSE_INTERCEPTOR

    // Interceptor call: STORAGE_PREACCESS_RESOURCES
    // This can be used to remove results from the search result details before
    // the user has a chance to know that they were in the results
    if (allAdded.size() > 0) {
      List<ResourcePersistentId> includedPidList = new ArrayList<>(allAdded);
      JpaPreResourceAccessDetails accessDetails = new JpaPreResourceAccessDetails(
        includedPidList,
        () -> this
      );
      HookParams params = new HookParams()
        .add(IPreResourceAccessDetails.class, accessDetails)
        .add(RequestDetails.class, theRequest)
        .addIfMatchesType(ServletRequestDetails.class, theRequest);
      JpaInterceptorBroadcaster.doCallHooks(
        myInterceptorBroadcaster,
        theRequest,
        Pointcut.STORAGE_PREACCESS_RESOURCES,
        params
      );

      for (int i = includedPidList.size() - 1; i >= 0; i--) {
        if (accessDetails.isDontReturnResourceAtIndex(i)) {
          ResourcePersistentId value = includedPidList.remove(i);
          if (value != null) {
            theMatches.remove(value);
          }
        }
      }

      allAdded = new HashSet<>(includedPidList);
    }
    return allAdded;
    // PersistedJpaBundleProvider.toResourceList()
    // then loadResourcesByPid()
  }

  private List<Collection<ResourcePersistentId>> partition(
    Collection<ResourcePersistentId> theNextRoundMatches,
    int theMaxLoad
  ) {
    if (theNextRoundMatches.size() <= theMaxLoad) {
      return Collections.singletonList(theNextRoundMatches);
    } else {
      List<Collection<ResourcePersistentId>> retVal = new ArrayList<>();
      Collection<ResourcePersistentId> current = null;
      for (ResourcePersistentId next : theNextRoundMatches) {
        if (current == null) {
          current = new ArrayList<>(theMaxLoad);
          retVal.add(current);
        }

        current.add(next);

        if (current.size() >= theMaxLoad) {
          current = null;
        }
      }

      return retVal;
    }
  }

  private static List<Predicate> createLastUpdatedPredicates(
    final DateRangeParam theLastUpdated,
    CriteriaBuilder builder,
    From<?, ResourceTable> from
  ) {
    List<Predicate> lastUpdatedPredicates = new ArrayList<>();
    if (theLastUpdated != null) {
      if (theLastUpdated.getLowerBoundAsInstant() != null) {
        ourLog.debug(
          "LastUpdated lower bound: {}",
          new InstantDt(theLastUpdated.getLowerBoundAsInstant())
        );
        Predicate predicateLower = builder.greaterThanOrEqualTo(
          from.get("myUpdated"),
          theLastUpdated.getLowerBoundAsInstant()
        );
        lastUpdatedPredicates.add(predicateLower);
      }
      if (theLastUpdated.getUpperBoundAsInstant() != null) {
        Predicate predicateUpper = builder.lessThanOrEqualTo(
          from.get("myUpdated"),
          theLastUpdated.getUpperBoundAsInstant()
        );
        lastUpdatedPredicates.add(predicateUpper);
      }
    }
    return lastUpdatedPredicates;
  }

  private static List<ResourcePersistentId> filterResourceIdsByLastUpdated(
    EntityManager theEntityManager,
    final DateRangeParam theLastUpdated,
    Collection<ResourcePersistentId> thePids
  ) {
    if (thePids.isEmpty()) {
      return Collections.emptyList();
    }
    CriteriaBuilder builder = theEntityManager.getCriteriaBuilder();
    CriteriaQuery<Long> cq = builder.createQuery(Long.class);
    Root<ResourceTable> from = cq.from(ResourceTable.class);
    cq.select(from.get("myId").as(Long.class));

    List<Predicate> lastUpdatedPredicates = createLastUpdatedPredicates(
      theLastUpdated,
      builder,
      from
    );
    lastUpdatedPredicates.add(
      from.get("myId").as(Long.class).in(ResourcePersistentId.toLongList(thePids))
    );

    cq.where(SearchBuilder.toPredicateArray(lastUpdatedPredicates));
    TypedQuery<Long> query = theEntityManager.createQuery(cq);

    return ResourcePersistentId.fromLongList(query.getResultList());
  }
}
