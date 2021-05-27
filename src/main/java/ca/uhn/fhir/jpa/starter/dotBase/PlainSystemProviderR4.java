package ca.uhn.fhir.jpa.starter.dotBase;

import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.provider.r4.JpaSystemProviderR4;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.annotation.RawParam;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.BundleProviders;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.StringType;
import org.springframework.web.context.ContextLoaderListener;

public class PlainSystemProviderR4 extends JpaSystemProviderR4 {

  @Operation(name = "$history", idempotent = true)
  public <T extends IBaseResource> IBundleProvider getAllVersions(HttpServletRequest theRequest,
      RequestDetails requestDetails, @OperationParam(name = "_type") StringType resourceType,
      @OperationParam(name = "_id") IdType resourceId, @RawParam Map<String, List<String>> queryParams)
      throws Exception {
    if (resourceType == null)
      throw new InvalidRequestException("Parameter '_type' must be provided");
    IFhirResourceDao<T> resourceDAO = this.getDao(resourceType);

    if (resourceId != null) {
      return this.instanceHistory(requestDetails, resourceDAO, resourceId);
    }

    IBundleProvider typeHistory = this.typeHistory(requestDetails, resourceDAO);
    if (queryParams == null)
      return typeHistory;

    return this.searchHistory(queryParams, typeHistory, requestDetails, resourceDAO);
  }

  @SuppressWarnings("unchecked")
  public <T extends IBaseResource> IFhirResourceDao<T> getDao(StringType resourceType) {
    return ContextLoaderListener.getCurrentWebApplicationContext().getBean("my" + resourceType + "DaoR4",
        IFhirResourceDao.class);
  }

  public <T extends IBaseResource> IBundleProvider instanceHistory(RequestDetails requestDetails,
      IFhirResourceDao<T> resourceDAO, IdType resourceId) {
    IBundleProvider instanceHisotry = this.getHistoryForResourceInstance(requestDetails, resourceDAO, resourceId);
    return this.filterDeletes(instanceHisotry);
  }

  private <T extends IBaseResource> IBundleProvider typeHistory(RequestDetails requestDetails,
      IFhirResourceDao<T> resourceDAO) {
    IBundleProvider typeHistory = this.getHistoryForResourceType(requestDetails, resourceDAO);
    return this.filterDeletes(typeHistory);
  }

  private <T extends IBaseResource> IBundleProvider searchHistory(Map<String, List<String>> queryParams,
      IBundleProvider typeHistory, RequestDetails requestDetails, IFhirResourceDao<T> resourceDAO) {
    IBundleProvider searchRes = searchQuery(queryParams, requestDetails, resourceDAO);
    return this.filterSearchHistory(typeHistory, searchRes);
  }

  private <T extends IBaseResource> IBundleProvider getHistoryForResourceInstance(RequestDetails requestDetails,
      IFhirResourceDao<T> resourceDAO, IdType resourceId) {
    return resourceDAO.history(resourceId, null, null, requestDetails);
  }

  private <T extends IBaseResource> IBundleProvider getHistoryForResourceType(RequestDetails requestDetails,
      IFhirResourceDao<T> resourceDAO) {
    return resourceDAO.history(null, null, requestDetails);
  }

  private IBundleProvider filterDeletes(IBundleProvider resources) {
    List<IBaseResource> resourceList = resources.getResources(0, resources.size());
    List<String> deleteIds = this.getDeleteIds(resourceList);
    List<IBaseResource> filteredList = this.filterDelete(deleteIds, resourceList);
    return BundleProviders.newList(filteredList);
  }

  private List<IBaseResource> filterDelete(List<String> deleteIds, List<IBaseResource> resourceList) {
    return resourceList.stream().filter(resource -> !deleteIds.contains(resource.getIdElement().getIdPart()))
        .collect(Collectors.toList());
  }

  private List<String> getDeleteIds(List<IBaseResource> resourceList) {
    return resourceList.stream().filter(resource -> resource.getUserData("ENTRY_TRANSACTION_OPERATION") == "DELETE")
        .map(resource -> resource.getIdElement().getIdPart()).collect(Collectors.toList());
  }

  private <T extends IBaseResource> IBundleProvider searchQuery(Map<String, List<String>> queryParams,
      RequestDetails requestDetails, IFhirResourceDao<T> resourceDAO) {
    SearchParameterMap paramMap = new SearchParameterMap();
    resourceDAO.translateRawParameters(queryParams, paramMap);
    return resourceDAO.search(paramMap, requestDetails);
  }

  private IBundleProvider filterSearchHistory(IBundleProvider typeHistory, IBundleProvider searchRes) {
    List<IBaseResource> historyList = typeHistory.getResources(0, typeHistory.size());
    List<String> searchIds = this.getSearchIds(searchRes);
    List<IBaseResource> searchHistory = this.filterHistory(searchIds, historyList);
    return BundleProviders.newList(searchHistory);
  }

  private List<String> getSearchIds(IBundleProvider searchRes) {
    return searchRes.getResources(0, searchRes.size()).stream().map(resource -> resource.getIdElement().getIdPart())
        .collect(Collectors.toList());
  }

  private List<IBaseResource> filterHistory(List<String> searchIds, List<IBaseResource> historyList) {
    return historyList.stream().filter(resource -> searchIds.contains(resource.getIdElement().getIdPart()))
        .collect(Collectors.toList());
  }
}
