package ca.uhn.fhir.jpa.starter.dotBase.entities.model;

import ca.uhn.fhir.jpa.starter.dotBase.entities.entity.AccessLog;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.transaction.Transactional;
import org.hl7.fhir.r4.model.StringType;
import org.springframework.stereotype.Repository;

@Repository
public class AccessLogRepository {
  @PersistenceContext
  public EntityManager em;

  @Transactional
  public void createLog(
    String method,
    String username,
    String url,
    String resourcetype,
    String timestamp
  ) {
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
  public List<AccessLog> getLogs(Map<String, StringType> queryParams, StringType limit) {
    String queryString = getQuery(queryParams, limit);
    Query query = em.createNativeQuery(queryString, AccessLog.class);
    return query.getResultList();
  }

  private String getQuery(Map<String, StringType> queryParams, StringType limit) {
    String query = "SELECT * FROM ACCESS_LOGGING";
    String whereString = whereQuery(queryParams);
    String limitString = limitQuery(limit);
    return query + whereString + limitString;
  }

  private String whereQuery(Map<String, StringType> queryParams) {
    String whereString = " WHERE ";
    for (Map.Entry<String, StringType> param : queryParams.entrySet()) {
      if (param.getValue() == null)
        continue;
      
      if (!whereString.equals(" WHERE "))
        whereString += " AND ";
        
      whereString += param.getKey() + " LIKE '" + param.getValue().toString() + "'";
    }
    return !whereString.equals(" WHERE ") ? whereString : "";
  }

  private String limitQuery(StringType limit) {
    if (limit != null && isNumeric(limit)) {
      return limit + ";";
    }
    return ";";
  }

  public static boolean isNumeric(StringType limit) {
    try {
      int number = Integer.parseInt(limit.toString());
      return number > 0;
    } catch (NumberFormatException e) {
      return false;
    }
  }
}
