package ca.uhn.fhir.jpa.starter.dotbase.search;

import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.RuntimeResourceDefinition;
import ca.uhn.fhir.context.RuntimeSearchParam;
import ca.uhn.fhir.interceptor.api.HookParams;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.jpa.api.dao.IDao;
import ca.uhn.fhir.jpa.interceptor.JpaPreResourceAccessDetails;
import ca.uhn.fhir.jpa.model.entity.ModelConfig;
import ca.uhn.fhir.jpa.model.entity.ResourceTable;
import ca.uhn.fhir.jpa.search.builder.SearchBuilder;
import ca.uhn.fhir.jpa.util.JpaInterceptorBroadcaster;
import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.model.primitive.InstantDt;
import ca.uhn.fhir.rest.api.server.IPreResourceAccessDetails;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.api.server.storage.ResourcePersistentId;
import ca.uhn.fhir.rest.server.util.ISearchParamRegistry;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import ca.uhn.fhir.util.StopWatch;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Custom SearchBuilder that overrides SearchBuilder.loadIncludes() to handle
 * resolving external references: Due to using separate FHIR servers for clinical,study
 * and core data in dot.base, external references should be resolved and
 * returned as well.
 *
 * In general this aspect is handled by the fhir-reverse-proxy that handles all
 * incoming resources - but only for simple requests like .../fhir/Patient or
 * transaction bundles
 *
 * Requests with _include params that result in a external reference to be
 * included in the response are handled by this SearchBuilder. See also:
 * ResponseInterceptor
 * 
 * ALL CUSTOM CHANGES ARE COMMENTED WITH -- DOTBASE -- TO FACILITATE EASIER UPDATING
 */
public class SearchBuilderExternalReferences extends SearchBuilder {
  @Autowired
  private ISearchParamRegistry mySearchParamRegistry;
  @Autowired
  private ModelConfig myModelConfig;

  private static final Logger ourLog = LoggerFactory.getLogger(SearchBuilder.class);

  public SearchBuilderExternalReferences(IDao theDao, String theResourceName, Class<? extends IBaseResource> theResourceType) {
    super(theDao, theResourceName, theResourceType);
  }

  /**
   * @Autowired private ISearchParamRegistry mySearchParamRegistry;
   * 
   *            private static final org.slf4jcallPreAccessResourcesHook.Logger
   *            ourLog = org.slf4j.LoggerFactory.getLogger(
   *            SearchBuilderIncludes.class ); THIS SHOULD RETURN HASHSET and not
   *            just Set because we add to it later so it can't be
   *            Collections.emptySet() or some such thing
   */
  @Override
  public HashSet<ResourcePersistentId> loadIncludes(FhirContext theContext, EntityManager theEntityManager,
      Collection<ResourcePersistentId> theMatches, Set<Include> theRevIncludes, boolean theReverseMode,
      DateRangeParam theLastUpdated, String theSearchIdOrDescription, RequestDetails theRequest) {
    if (theMatches.size() == 0) {
      return new HashSet<>();
    }
    if (theRevIncludes == null || theRevIncludes.isEmpty()) {
      return new HashSet<>();
    }
    String searchPidFieldName = theReverseMode ? "myTargetResourcePid" : "mySourceResourcePid";
    String findPidFieldName = theReverseMode ? "mySourceResourcePid" : "myTargetResourcePid";
    String findVersionFieldName = null;
    if (!theReverseMode && myModelConfig.isRespectVersionsForSearchIncludes()) {
      findVersionFieldName = "myTargetResourceVersion";
    }

    List<ResourcePersistentId> nextRoundMatches = new ArrayList<>(theMatches);
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

          StringBuilder sqlBuilder = new StringBuilder();
          sqlBuilder.append("SELECT r.").append(findPidFieldName);
          /**
           * -- DOTBASE -- added r.myTargetResourceUrl to queries
           */
          sqlBuilder.append(", r.myTargetResourceUrl");
          if (findVersionFieldName != null) {
            sqlBuilder.append(", r." + findVersionFieldName);
          }
          sqlBuilder.append(" FROM ResourceLink r WHERE r.");
          sqlBuilder.append(searchPidFieldName);
          sqlBuilder.append(" IN (:target_pids)");
          String sql = sqlBuilder.toString();
          List<Collection<ResourcePersistentId>> partitions = partition(nextRoundMatches, getMaximumPageSize());
          for (Collection<ResourcePersistentId> nextPartition : partitions) {
            TypedQuery<?> q = theEntityManager.createQuery(sql, Object[].class);
            q.setParameter("target_pids", ResourcePersistentId.toLongList(nextPartition));
            List<?> results = q.getResultList();
            for (Object nextRow : results) {
              if (nextRow == null) {
                // This can happen if there are outgoing references which are canonical or point
                // to
                // other servers
                continue;
              }

              /**
              * -- DOTBASE -- 
              * Changed else clause due to resourceLink always being of type Object[]
              * as a result of the changed query
              */
              Long resourceLink;
              Long version = null;
              if (findVersionFieldName != null) {
                resourceLink = (Long) ((Object[]) nextRow)[0];
                version = (Long) ((Object[]) nextRow)[1];
              } else {
                resourceLink = (Long) ((Object[]) nextRow)[0];
              }

              pidsToInclude.add(new ResourcePersistentId(resourceLink, version));
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
            ourLog.warn("Unknown resource type in include/revinclude=" + nextInclude.getValue());
            continue;
          }

          String paramName = nextInclude.getParamName();
          if (isNotBlank(paramName)) {
            param = mySearchParamRegistry.getActiveSearchParam(resType, paramName);
          } else {
            param = null;
          }
          if (param == null) {
            ourLog.warn("Unknown param name in include/revinclude=" + nextInclude.getValue());
            continue;
          }

          paths = param.getPathsSplit();

          String targetResourceType = defaultString(nextInclude.getParamTargetType(), null);
          for (String nextPath : paths) {
            String sql;

            boolean haveTargetTypesDefinedByParam = param.hasTargets();
            String fieldsToLoad = "r." + findPidFieldName;
            if (findVersionFieldName != null) {
              fieldsToLoad += ", r." + findVersionFieldName;
            }

            /**
             * -- DOTBASE -- added , r.myTargetResourceUrl to queries
             */
            if (targetResourceType != null) {
              sql = "SELECT " + fieldsToLoad
                  + " , r.myTargetResourceUrl FROM ResourceLink r WHERE r.mySourcePath = :src_path AND r."
                  + searchPidFieldName + " IN (:target_pids) AND r.myTargetResourceType = :target_resource_type";
            } else if (haveTargetTypesDefinedByParam) {
              sql = "SELECT " + fieldsToLoad
                  + " , r.myTargetResourceUrl FROM ResourceLink r WHERE r.mySourcePath = :src_path AND r."
                  + searchPidFieldName + " IN (:target_pids) AND r.myTargetResourceType in (:target_resource_types)";
            } else {
              sql = "SELECT " + fieldsToLoad
                  + " , r.myTargetResourceUrl FROM ResourceLink r WHERE r.mySourcePath = :src_path AND r."
                  + searchPidFieldName + " IN (:target_pids)";
            }

            List<Collection<ResourcePersistentId>> partitions = partition(nextRoundMatches, getMaximumPageSize());
            for (Collection<ResourcePersistentId> nextPartition : partitions) {
              TypedQuery<?> q = theEntityManager.createQuery(sql, Object[].class);
              q.setParameter("src_path", nextPath);
              q.setParameter("target_pids", ResourcePersistentId.toLongList(nextPartition));
              if (targetResourceType != null) {
                q.setParameter("target_resource_type", targetResourceType);
              } else if (haveTargetTypesDefinedByParam) {
                q.setParameter("target_resource_types", param.getTargets());
              }
              List<?> results = q.getResultList();
              for (Object resourceLink : results) {
                if (resourceLink != null) {
                  ResourcePersistentId persistentId;
                  /**
                   * -- DOTBASE -- 
                   * add if clause with method 'externalReferenceToInclude' and moved
                   * original hapi fhir code into else clause. Nested else also changed
                   * due to resourceLink always being of type Object[] as a result of the changed query
                   */
                  if (resourceLink instanceof Object[] && this.isExternalReference((Object[]) resourceLink)) {
                    this.externalReferenceToInclude((Object[]) resourceLink, theRequest);
                  } else {
                    if (findVersionFieldName != null) {
                      persistentId = new ResourcePersistentId(((Object[]) resourceLink)[0]);
                      persistentId.setVersion((Long) ((Object[]) resourceLink)[1]);
                    } else {
                      persistentId = new ResourcePersistentId(((Object[]) resourceLink)[0]);
                    }
                    assert persistentId.getId() instanceof Long;
                    pidsToInclude.add(persistentId);
                  }
                }
              }
            }
          }
        }
      }

      if (theReverseMode) {
        if (theLastUpdated != null
            && (theLastUpdated.getLowerBoundAsInstant() != null || theLastUpdated.getUpperBoundAsInstant() != null)) {
          pidsToInclude = new HashSet<>(
              filterResourceIdsByLastUpdated(theEntityManager, theLastUpdated, pidsToInclude));
        }
      }

      nextRoundMatches.clear();
      for (ResourcePersistentId next : pidsToInclude) {
        if (original.contains(next) == false && allAdded.contains(next) == false) {
          theMatches.add(next);
          nextRoundMatches.add(next);
        }
      }

      addedSomeThisRound = allAdded.addAll(pidsToInclude);
    } while (includes.size() > 0 && nextRoundMatches.size() > 0 && addedSomeThisRound);

    allAdded.removeAll(original);

    ourLog.info("Loaded {} {} in {} rounds and {} ms for search {}", allAdded.size(),
        theReverseMode ? "_revincludes" : "_includes", roundCounts, w.getMillisAndRestart(), theSearchIdOrDescription);

    // Interceptor call: STORAGE_PREACCESS_RESOURCES
    // This can be used to remove results from the search result details before
    // the user has a chance to know that they were in the results
    if (allAdded.size() > 0) {
      List<ResourcePersistentId> includedPidList = new ArrayList<>(allAdded);
      JpaPreResourceAccessDetails accessDetails = new JpaPreResourceAccessDetails(includedPidList, () -> this);
      HookParams params = new HookParams().add(IPreResourceAccessDetails.class, accessDetails)
          .add(RequestDetails.class, theRequest).addIfMatchesType(ServletRequestDetails.class, theRequest);
      JpaInterceptorBroadcaster.doCallHooks(myInterceptorBroadcaster, theRequest, Pointcut.STORAGE_PREACCESS_RESOURCES,
          params);

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
  }

  private List<Collection<ResourcePersistentId>> partition(Collection<ResourcePersistentId> theNextRoundMatches,
      int theMaxLoad) {
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

  private static List<ResourcePersistentId> filterResourceIdsByLastUpdated(EntityManager theEntityManager, final DateRangeParam theLastUpdated, Collection<ResourcePersistentId> thePids) {
		if (thePids.isEmpty()) {
			return Collections.emptyList();
		}
		CriteriaBuilder builder = theEntityManager.getCriteriaBuilder();
		CriteriaQuery<Long> cq = builder.createQuery(Long.class);
		Root<ResourceTable> from = cq.from(ResourceTable.class);
		cq.select(from.get("myId").as(Long.class));

		List<Predicate> lastUpdatedPredicates = createLastUpdatedPredicates(theLastUpdated, builder, from);
		lastUpdatedPredicates.add(from.get("myId").as(Long.class).in(ResourcePersistentId.toLongList(thePids)));

		cq.where(SearchBuilder.toPredicateArray(lastUpdatedPredicates));
		TypedQuery<Long> query = theEntityManager.createQuery(cq);

		return ResourcePersistentId.fromLongList(query.getResultList());
	}

  private static List<Predicate> createLastUpdatedPredicates(final DateRangeParam theLastUpdated, CriteriaBuilder builder, From<?, ResourceTable> from) {
		List<Predicate> lastUpdatedPredicates = new ArrayList<>();
		if (theLastUpdated != null) {
			if (theLastUpdated.getLowerBoundAsInstant() != null) {
				ourLog.debug("LastUpdated lower bound: {}", new InstantDt(theLastUpdated.getLowerBoundAsInstant()));
				Predicate predicateLower = builder.greaterThanOrEqualTo(from.get("myUpdated"), theLastUpdated.getLowerBoundAsInstant());
				lastUpdatedPredicates.add(predicateLower);
			}
			if (theLastUpdated.getUpperBoundAsInstant() != null) {
				Predicate predicateUpper = builder.lessThanOrEqualTo(from.get("myUpdated"), theLastUpdated.getUpperBoundAsInstant());
				lastUpdatedPredicates.add(predicateUpper);
			}
		}
		return lastUpdatedPredicates;
	}

  /**
   * -- DOTBASE --
   * 
   * @param resourceLink
   * @return
   */
  private boolean isExternalReference(Object[] resourceLink) {
    if (resourceLink.length == 2 && resourceLink[1] != null)
      return true;
    return false;
  }

  /**
   * -- DOTBASE --
   * 
   * @param resourceLink
   * @param theRequest
   */
  @SuppressWarnings("unchecked")
  private void externalReferenceToInclude(Object[] resourceLink, RequestDetails theRequest) {
    HashSet<String> attributeValues = new HashSet<String>();
    HashSet<String> currentValues = (HashSet<String>) theRequest.getAttribute("_includeIsExternalReference");
    if (currentValues != null)
      attributeValues = currentValues;
    attributeValues.add(resourceLink[1].toString());
    theRequest.setAttribute("_includeIsExternalReference", attributeValues);
  }

}