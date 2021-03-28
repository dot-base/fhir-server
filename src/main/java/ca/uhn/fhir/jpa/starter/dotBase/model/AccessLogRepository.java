package ca.uhn.fhir.jpa.starter.dotBase.model;

import ca.uhn.fhir.jpa.starter.dotBase.entity.AccessLog;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;
import org.springframework.stereotype.Repository;

@Repository
public class AccessLogRepository {
  @PersistenceContext
  private EntityManager em;

  public AccessLogRepository() {
  }

  @Transactional
  public void createLog(String method, String username, String url, String timestamp) {
    AccessLog logEntity = new AccessLog();
    logEntity.method = method;
    logEntity.username = username;
    logEntity.url = url;
    logEntity.timestamp = timestamp;
    em.persist(logEntity);
  }
}
