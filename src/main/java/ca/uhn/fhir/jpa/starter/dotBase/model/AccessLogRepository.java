package ca.uhn.fhir.jpa.starter.dotBase.model;

import ca.uhn.fhir.jpa.starter.dotBase.entity.AccessLog;

import java.text.SimpleDateFormat;
import java.util.Date;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;
import org.springframework.stereotype.Repository;

@Repository
public class AccessLogRepository {
  @PersistenceContext
  private EntityManager em;

  public AccessLogRepository() {}

  @Transactional
  public void createLog(String method, String username, String url) {
    AccessLog logEntity = new AccessLog();
    logEntity.method = method;
    logEntity.username = username;
    logEntity.url = url;
    logEntity.timestamp = getCurrentTimestamp();
    em.persist(logEntity);
  }

  private static String getCurrentTimestamp(){
    SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
    return formatter.format(new Date());
  }
}
