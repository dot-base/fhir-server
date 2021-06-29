package ca.uhn.fhir.jpa.starter.dotbase.accesslog;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.transaction.Transactional;

import org.apache.commons.lang3.ObjectUtils.Null;
import org.hl7.fhir.r4.model.StringType;
import org.springframework.stereotype.Repository;

@Repository
public class AccessLogModel {
  @PersistenceContext
  public EntityManager em;

  @Transactional
  public void createLog(
    String requestId,
    String method,
    String username,
    String url,
    String resourcetype,
    String timestamp
  ) {
    AccessLog logEntity = new AccessLog();
    logEntity.requestId = requestId;
    logEntity.method = method;
    logEntity.username = username;
    logEntity.url = url;
    logEntity.timestamp = timestamp;
    logEntity.resourcetype = resourcetype;
    em.persist(logEntity);
  }

  @Transactional
  @SuppressWarnings("unchecked")
  public List<AccessLog> getLogs(Map<String, StringType> queryParams, StringType limit) {
    CriteriaQuery<AccessLog> cQuery = getQuery(queryParams);
    Query query = em.createQuery(cQuery);
    try {
      query.setMaxResults(toNumeric(limit));
    } catch (NumberFormatException e) {
    }
    return query.getResultList();
  }

  private CriteriaQuery<AccessLog> getQuery(Map<String, StringType> queryParams) {
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery<AccessLog> query = cb.createQuery(AccessLog.class);
    Root<AccessLog> root = query.from(AccessLog.class);

    if (queryParams != null) {
      return whereQuery(queryParams);
    }
    return query.select(root);
  }

  private CriteriaQuery<AccessLog> whereQuery(Map<String, StringType> queryParams) {
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery<AccessLog> cq = cb.createQuery(AccessLog.class);
    Root<AccessLog> root = cq.from(AccessLog.class);
    List<Predicate> predicates = new ArrayList<Predicate>();

    for (Map.Entry<String, StringType> param : queryParams.entrySet()) {
      if (param.getValue() != null) {
        Predicate p = cb.equal(root.get(param.getKey()), param.getValue().toString());
        predicates.add(p);
      }
    }
    return cq.select(root).where(cb.and(predicates.toArray(new Predicate[] {})));
  }

  private static Integer toNumeric(StringType limit) throws NumberFormatException {
    return Integer.parseInt(limit.toString());
  }
}
