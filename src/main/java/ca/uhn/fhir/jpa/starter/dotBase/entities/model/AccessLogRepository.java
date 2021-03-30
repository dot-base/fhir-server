package ca.uhn.fhir.jpa.starter.dotBase.entities.model;

import ca.uhn.fhir.jpa.starter.dotBase.entities.entity.AccessLog;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.transaction.Transactional;
import org.springframework.stereotype.Repository;

@Repository
public class AccessLogRepository {
  @PersistenceContext
  public EntityManager em;

  @Transactional
  public void createLog(String method, String username, String url, String resourcetype, String timestamp) {
    AccessLog logEntity = new AccessLog();
    logEntity.method = method;
    logEntity.username = username;
    logEntity.url = url;
    logEntity.timestamp = timestamp;
    logEntity.resourcetype = resourcetype;
    em.persist(logEntity);
  }

  @Transactional
  @SuppressWarnings("unchecked")
  public List<AccessLog> findAll() {
    Query query = em.createNativeQuery("SELECT * FROM ACCESS_LOGGING LIMIT 1000", AccessLog.class);
    return query.getResultList();
  }

  @Transactional
  @SuppressWarnings("unchecked")
  public List<AccessLog> findByQuery(Map<String, String> queryParams) {
    Query query = em.createNativeQuery(buildQuery(queryParams), AccessLog.class);
    return query.getResultList();
  }

  //TODO: build query based on params
  private String buildQuery(Map<String, String> queryParams) {
    String query = "SELECT * FROM ACCESS_LOGGING LIMIT 1000";
    return query;
  }
}
