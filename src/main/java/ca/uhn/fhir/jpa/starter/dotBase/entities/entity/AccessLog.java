package ca.uhn.fhir.jpa.starter.dotBase.entities.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;

@Entity
@Table(
  name = "ACCESS_LOGGING",
  uniqueConstraints = {},
  indexes = {
    @Index(name = "IDX_LOG_ID", columnList = "LOG_ID", unique = true),
    @Index(name = "IDX_REQUEST_ID", columnList = "REQUEST_ID", unique = true),
    @Index(name = "IDX_METHOD", columnList = "METHOD", unique = false),
    @Index(name = "IDX_USERNAME", columnList = "USERNAME", unique = false),
    @Index(name = "IDX_URL", columnList = "URL", unique = false),
    @Index(name = "IDX_TIMESTAMP", columnList = "TIMESTAMP", unique = false),
    @Index(name = "IDX_RESOURCETYPE", columnList = "RESOURCETYPE", unique = false)
  }
)
public class AccessLog {
  @Id
  @Column(name = "LOG_ID")
  @GeneratedValue(strategy = GenerationType.AUTO)
  public int id;

  @Column(name = "REQUEST_ID")
  public String requestId;

  @Column(name = "METHOD")
  public String method;

  @Column(name = "USERNAME")
  public String username;

  @Column(name = "URL")
  public String url;

  @Column(name = "TIMESTAMP")
  public String timestamp;

  @Column(name = "RESOURCETYPE")
  public String resourcetype;
}
